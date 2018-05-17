/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "SQLiteConnection"

#include "Assert.h"
#include "Memory.h"
#include "Natives.h"
#include "KString.h"
#include "Porting.h"
#include "Types.h"

#include "utf8.h"

#include <stdlib.h>
#include <sys/mman.h>

#include <string.h>
#include <unistd.h>

#include "AndroidfwCursorWindow.h"

#include <sqlite3.h>

#include "android_database_SQLiteCommon.h"

// Set to 1 to use UTF16 storage for localized indexes.
#define UTF16_STORAGE 0

namespace android {

/* Busy timeout in milliseconds.
 * If another connection (possibly in another process) has the database locked for
 * longer than this amount of time then SQLite will generate a SQLITE_BUSY error.
 * The SQLITE_BUSY error is then raised as a SQLiteDatabaseLockedException.
 *
 * In ordinary usage, busy timeouts are quite rare.  Most databases only ever
 * have a single open connection at a time unless they are using WAL.  When using
 * WAL, a timeout could occur if one connection is busy performing an auto-checkpoint
 * operation.  The busy timeout needs to be long enough to tolerate slow I/O write
 * operations but not so long as to cause the application to hang indefinitely if
 * there is a problem acquiring a database lock.
 */
static const int BUSY_TIMEOUT_MS = 2500;

/*static struct {
    jfieldID name;
    jfieldID numArgs;
    jmethodID dispatchCallback;
} gSQLiteCustomFunctionClassInfo;*/

/*static struct {
    jclass clazz;
} gStringClassInfo;*/

struct SQLiteConnection {
    // Open flags.
    // Must be kept in sync with the constants defined in SQLiteDatabase.java.
    enum {
        OPEN_READWRITE          = 0x00000000,
        OPEN_READONLY           = 0x00000001,
        OPEN_READ_MASK          = 0x00000001,
        NO_LOCALIZED_COLLATORS  = 0x00000010,
        CREATE_IF_NECESSARY     = 0x10000000,
    };

    sqlite3* const db;
    const int openFlags;
    const KStdString path;
    const KStdString label;

    volatile bool canceled;

    SQLiteConnection(sqlite3* db, int openFlags, const KStdString& path, const KStdString& label) :
        db(db), openFlags(openFlags), path(path), label(label), canceled(false) { }
};

// Called each time a statement begins execution, when tracing is enabled.
static void sqliteTraceCallback(void *data, const char *sql) {
    SQLiteConnection* connection = static_cast<SQLiteConnection*>(data);
    ALOG(LOG_VERBOSE, SQLITE_TRACE_TAG, "%s: \"%s\"\n",
            connection->label.c_str(), sql);
}

// Called each time a statement finishes execution, when profiling is enabled.
static void sqliteProfileCallback(void *data, const char *sql, sqlite3_uint64 tm) {
    SQLiteConnection* connection = static_cast<SQLiteConnection*>(data);
    ALOG(LOG_VERBOSE, SQLITE_PROFILE_TAG, "%s: \"%s\" took %0.3f ms\n",
            connection->label.c_str(), sql, tm * 0.000001f);
}

// Called after each SQLite VM instruction when cancelation is enabled.
static int sqliteProgressHandlerCallback(void* data) {
    SQLiteConnection* connection = static_cast<SQLiteConnection*>(data);
    return connection->canceled;
}


static KLong nativeOpen(KString pathStr, KInt openFlags,
        KString labelStr, KBoolean enableTrace, KBoolean enableProfile) {

    RuntimeAssert(pathStr->type_info() == theStringTypeInfo, "Must use a string");
    RuntimeAssert(labelStr->type_info() == theStringTypeInfo, "Must use a string");

    int sqliteFlags;
    if (openFlags & SQLiteConnection::CREATE_IF_NECESSARY) {
        sqliteFlags = SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE;
    } else if (openFlags & SQLiteConnection::OPEN_READONLY) {
        sqliteFlags = SQLITE_OPEN_READONLY;
    } else {
        sqliteFlags = SQLITE_OPEN_READWRITE;
    }

    KStdString path;
    KStdString label;

    const KChar* utf16 = CharArrayAddressOfElementAt(pathStr, 0);
    utf8::with_replacement::utf16to8(utf16, utf16 + pathStr->count_, back_inserter(path));

    utf16 = CharArrayAddressOfElementAt(labelStr, 0);
    utf8::with_replacement::utf16to8(utf16, utf16 + labelStr->count_, back_inserter(label));

    sqlite3* db;
    int err = sqlite3_open_v2(path.c_str(), &db, sqliteFlags, NULL);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception_errcode(err, "Could not open database");
        return 0;
    }

    // Check that the database is really read/write when that is what we asked for.
    if ((sqliteFlags & SQLITE_OPEN_READWRITE) && sqlite3_db_readonly(db, NULL)) {
        throw_sqlite3_exception( db, "Could not open the database in read/write mode.");
        sqlite3_close(db);
        return 0;
    }

    // Set the default busy handler to retry automatically before returning SQLITE_BUSY.
    err = sqlite3_busy_timeout(db, BUSY_TIMEOUT_MS);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception( db, "Could not set busy timeout");
        sqlite3_close(db);
        return 0;
    }

    // Create wrapper object.
    SQLiteConnection* connection = new SQLiteConnection(db, openFlags, path, label);

    // Enable tracing and profiling if requested.
    if (enableTrace) {
        sqlite3_trace(db, &sqliteTraceCallback, connection);
    }
    if (enableProfile) {
        sqlite3_profile(db, &sqliteProfileCallback, connection);
    }

    ALOGV("Opened connection %p with label '%s'", db, label.c_str());
    return reinterpret_cast<KLong>(connection);
}

extern "C" KLong Android_Database_SQLiteConnection_nativeOpen(KString pathStr, KInt openFlags,
                                                                                 KString labelStr, KBoolean enableTrace, KBoolean enableProfile)
{
    return nativeOpen(pathStr, openFlags,
                   labelStr, enableTrace, enableProfile);
}

static void nativeClose(KLong connectionPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);

    if (connection) {
        ALOGV("Closing connection %p", connection->db);
        int err = sqlite3_close(connection->db);
        if (err != SQLITE_OK) {
            // This can happen if sub-objects aren't closed first.  Make sure the caller knows.
            ALOGE("sqlite3_close(%p) failed: %d", connection->db, err);
            throw_sqlite3_exception( connection->db, "Count not close db.");
            return;
        }

        delete connection;
    }
}

extern "C" void Android_Database_SQLiteConnection_nativeClose(KLong connectionPtr)
{
    nativeClose(connectionPtr);
}

static KLong nativePrepareStatement(KLong connectionPtr, KString sqlString) {

    RuntimeAssert(sqlString->type_info() == theStringTypeInfo, "Must use a string");

    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);

    KInt sqlLength = sqlString->count_;

    const KChar* sql = CharArrayAddressOfElementAt(sqlString, 0);
//    const jchar* sql = env->GetStringCritical(sqlString, NULL);
    sqlite3_stmt* statement;
    int err = sqlite3_prepare16_v2(connection->db,
            sql, sqlLength * sizeof(KChar), &statement, NULL);

            //TODO: I don't think we create or manage anything but...
//    env->ReleaseStringCritical(sqlString, sql);

    if (err != SQLITE_OK) {
        // Error messages like 'near ")": syntax error' are not
        // always helpful enough, so construct an error string that
        // includes the query itself.

      /*
      TODO: Figure out KN string manipulation
        char *message = (char*) malloc(strlen(query) + 50);
        if (message) {
            strcpy(message, ", while compiling: "); // less than 50 chars
            strcat(message, query);
        }
        env->ReleaseStringUTFChars(sqlString, query);
        throw_sqlite3_exception( connection->db, message);
        free(message);
        */
        return 0;
    }

    ALOGV("Prepared statement %p on connection %p", statement, connection->db);
    return reinterpret_cast<KLong>(statement);
}

extern "C" KLong Android_Database_SQLiteConnection_nativePrepareStatement(KLong connectionPtr, KString sqlString)
{
    return nativePrepareStatement(connectionPtr, sqlString);
}

static void nativeFinalizeStatement(KLong connectionPtr, KLong statementPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    // We ignore the result of sqlite3_finalize because it is really telling us about
    // whether any errors occurred while executing the statement.  The statement itself
    // is always finalized regardless.
    ALOGV("Finalized statement %p on connection %p", statement, connection->db);
    sqlite3_finalize(statement);
}

extern "C" void Android_Database_SQLiteConnection_nativeFinalizeStatement(
    KLong connectionPtr, KLong statementPtr)
{
    nativeFinalizeStatement(connectionPtr, statementPtr);
}

static KInt nativeGetParameterCount(KLong connectionPtr, KLong statementPtr) {
    //SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    return sqlite3_bind_parameter_count(statement);
}

extern "C" KInt Android_Database_SQLiteConnection_nativeGetParameterCount(
    KLong connectionPtr, KLong statementPtr)
{
    return nativeGetParameterCount(connectionPtr, statementPtr);
}

static KBoolean nativeIsReadOnly(KLong connectionPtr, KLong statementPtr) {
    //SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    return sqlite3_stmt_readonly(statement) != 0;
}

extern "C" KBoolean Android_Database_SQLiteConnection_nativeIsReadOnly(
    KLong connectionPtr, KLong statementPtr)
{
    return nativeIsReadOnly(connectionPtr, statementPtr);
}

static KInt nativeGetColumnCount(KLong connectionPtr, KLong statementPtr) {
    //SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    return sqlite3_column_count(statement);
}

extern "C" KInt Android_Database_SQLiteConnection_nativeGetColumnCount(
    KLong connectionPtr, KLong statementPtr)
{
    return nativeGetColumnCount(connectionPtr, statementPtr);
}

static size_t lengthOfString(const KChar* wstr)
{
    KChar* p = (KChar*)wstr;
    size_t len = 0;
    while(*p != 0){
        p++;
        len++;
    }

    return len;
}

extern "C" OBJ_GETTER(Android_Database_SQLiteConnection_nativeGetColumnName, KLong connectionPtr, KLong statementPtr, KInt index){
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    const KChar* name = static_cast<const KChar*>(sqlite3_column_name16(statement, index));
    if (name) {
        size_t size = lengthOfString(name);
        ArrayHeader* result = AllocArrayInstance(
            theStringTypeInfo, size, OBJ_RESULT)->array();

        memcpy(CharArrayAddressOfElementAt(result, 0),
            name,
            size * sizeof(KChar));

        RETURN_OBJ(result->obj());
    }

    RETURN_OBJ(nullptr);
}

static void nativeBindNull(KLong connectionPtr, KLong statementPtr, KInt index) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = sqlite3_bind_null(statement, index);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception( connection->db, NULL);
    }
}

extern "C" void Android_Database_SQLiteConnection_nativeBindNull(
    KLong connectionPtr, KLong statementPtr, KInt index)
{
    nativeBindNull(connectionPtr, statementPtr, index);
}

static void nativeBindLong(KLong connectionPtr, KLong statementPtr, KInt index, KLong value) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = sqlite3_bind_int64(statement, index, value);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception( connection->db, NULL);
    }
}

extern "C" void Android_Database_SQLiteConnection_nativeBindLong(
    KLong connectionPtr, KLong statementPtr, KInt index, KLong value)
{
    nativeBindLong(connectionPtr, statementPtr, index, value);
}

static void nativeBindDouble(KLong connectionPtr, KLong statementPtr, KInt index, KDouble value) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = sqlite3_bind_double(statement, index, value);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception( connection->db, NULL);
    }
}

extern "C" void Android_Database_SQLiteConnection_nativeBindDouble(
    KLong connectionPtr, KLong statementPtr, KInt index, KDouble value)
{
    nativeBindDouble(connectionPtr, statementPtr, index, value);
}

static void nativeBindString(KLong connectionPtr, KLong statementPtr, KInt index, KString valueString) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    KInt valueLength = valueString->count_;

    const KChar* value = CharArrayAddressOfElementAt(valueString, 0);
    int err = sqlite3_bind_text16(statement, index, value, valueLength * sizeof(KChar),
            SQLITE_TRANSIENT);

    if (err != SQLITE_OK) {
        throw_sqlite3_exception( connection->db, NULL);
    }
}

extern "C" void Android_Database_SQLiteConnection_nativeBindString(
    KLong connectionPtr, KLong statementPtr, KInt index, KString valueString)
{
    nativeBindString(connectionPtr, statementPtr, index, valueString);
}

static void nativeBindBlob(KLong connectionPtr, KLong statementPtr, KInt index, KConstRef valueArray) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    const ArrayHeader* array = valueArray->array();

    KInt valueLength = array->count_;
    const KByte* value = static_cast<const KByte*>(ByteArrayAddressOfElementAt(array, 0));
    //TODO: Do *we* need to copy the array?
    int err = sqlite3_bind_blob(statement, index, value, valueLength, SQLITE_TRANSIENT);

    if (err != SQLITE_OK) {
        throw_sqlite3_exception( connection->db, NULL);
    }
}

extern "C" void Android_Database_SQLiteConnection_nativeBindBlob(
    KLong connectionPtr, KLong statementPtr, KInt index, KConstRef valueArray)
{
    nativeBindBlob(connectionPtr, statementPtr, index, valueArray);
}

static void nativeResetStatementAndClearBindings(KLong connectionPtr, KLong statementPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = sqlite3_reset(statement);
    if (err == SQLITE_OK) {
        err = sqlite3_clear_bindings(statement);
    }
    if (err != SQLITE_OK) {
        throw_sqlite3_exception( connection->db, NULL);
    }
}

extern "C" void Android_Database_SQLiteConnection_nativeResetStatementAndClearBindings(
    KLong connectionPtr, KLong statementPtr)
{
    nativeResetStatementAndClearBindings(connectionPtr, statementPtr);
}

static int executeNonQuery(SQLiteConnection* connection, sqlite3_stmt* statement) {
    int err = sqlite3_step(statement);
    if (err == SQLITE_ROW) {
        throw_sqlite3_exception(
                "Queries can be performed using SQLiteDatabase query or rawQuery methods only.");
    } else if (err != SQLITE_DONE) {
        throw_sqlite3_exception( connection->db);
    }
    return err;
}

static void nativeExecute(KLong connectionPtr, KLong statementPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    executeNonQuery(connection, statement);
}

extern "C" void Android_Database_SQLiteConnection_nativeExecute(
    KLong connectionPtr, KLong statementPtr)
{
    nativeExecute(connectionPtr, statementPtr);
}

static KInt nativeExecuteForChangedRowCount(KLong connectionPtr, KLong statementPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = executeNonQuery(connection, statement);
    return err == SQLITE_DONE ? sqlite3_changes(connection->db) : -1;
}

extern "C" KInt Android_Database_SQLiteConnection_nativeExecuteForChangedRowCount(
    KLong connectionPtr, KLong statementPtr)
{
    return nativeExecuteForChangedRowCount(connectionPtr, statementPtr);
}

static KLong nativeExecuteForLastInsertedRowId(KLong connectionPtr, KLong statementPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = executeNonQuery(connection, statement);
    return err == SQLITE_DONE && sqlite3_changes(connection->db) > 0
            ? sqlite3_last_insert_rowid(connection->db) : -1;
}

extern "C" KLong Android_Database_SQLiteConnection_nativeExecuteForLastInsertedRowId(
    KLong connectionPtr, KLong statementPtr)
{
    return nativeExecuteForLastInsertedRowId(connectionPtr, statementPtr);
}

static int executeOneRowQuery(SQLiteConnection* connection, sqlite3_stmt* statement) {
    int err = sqlite3_step(statement);
    if (err != SQLITE_ROW) {
        throw_sqlite3_exception(connection->db);
    }
    return err;
}

static KLong nativeExecuteForLong(KLong connectionPtr, KLong statementPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = executeOneRowQuery(connection, statement);
    if (err == SQLITE_ROW && sqlite3_column_count(statement) >= 1) {
        return sqlite3_column_int64(statement, 0);
    }
    return -1;
}

extern "C" KLong Android_Database_SQLiteConnection_nativeExecuteForLong(
    KLong connectionPtr, KLong statementPtr)
{
    return nativeExecuteForLong(connectionPtr, statementPtr);
}

extern "C" OBJ_GETTER(Android_Database_SQLiteConnection_nativeExecuteForString, KLong connectionPtr, KLong statementPtr){
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = executeOneRowQuery(connection, statement);
    if (err == SQLITE_ROW && sqlite3_column_count(statement) >= 1) {
        const KChar* text = static_cast<const KChar*>(sqlite3_column_text16(statement, 0));
        if (text) {
            size_t size = lengthOfString(text);
            ArrayHeader* result = AllocArrayInstance(
                theStringTypeInfo, size, OBJ_RESULT)->array();

            memcpy(CharArrayAddressOfElementAt(result, 0),
                text,
                size * sizeof(KChar));

            RETURN_OBJ(result->obj());
        }
    }
    RETURN_OBJ(nullptr);
}

enum CopyRowResult {
    CPR_OK,
    CPR_FULL,
    CPR_ERROR,
};

static CopyRowResult copyRow(CursorWindow* window,
        sqlite3_stmt* statement, int numColumns, int startPos, int addedRows) {
    // Allocate a new field directory for the row.
    status_t status = window->allocRow();
    if (status) {
        LOG_WINDOW("Failed allocating fieldDir at startPos %d row %d, error=%d",
                startPos, addedRows, status);
        return CPR_FULL;
    }

    // Pack the row into the window.
    CopyRowResult result = CPR_OK;
    for (int i = 0; i < numColumns; i++) {
        int type = sqlite3_column_type(statement, i);
        if (type == SQLITE_TEXT) {
            // TEXT data
            const char* text = reinterpret_cast<const char*>(
                    sqlite3_column_text(statement, i));
            // SQLite does not include the NULL terminator in size, but does
            // ensure all strings are NULL terminated, so increase size by
            // one to make sure we store the terminator.
            size_t sizeIncludingNull = sqlite3_column_bytes(statement, i) + 1;
            status = window->putString(addedRows, i, text, sizeIncludingNull);
            if (status) {
                LOG_WINDOW("Failed allocating %u bytes for text at %d,%d, error=%d",
                        sizeIncludingNull, startPos + addedRows, i, status);
                result = CPR_FULL;
                break;
            }
            LOG_WINDOW("%d,%d is TEXT with %u bytes",
                    startPos + addedRows, i, sizeIncludingNull);
        } else if (type == SQLITE_INTEGER) {
            // INTEGER data
            int64_t value = sqlite3_column_int64(statement, i);
            status = window->putLong(addedRows, i, value);
            if (status) {
                LOG_WINDOW("Failed allocating space for a long in column %d, error=%d",
                        i, status);
                result = CPR_FULL;
                break;
            }
            LOG_WINDOW("%d,%d is INTEGER 0x%016llx", startPos + addedRows, i, value);
        } else if (type == SQLITE_FLOAT) {
            // FLOAT data
            double value = sqlite3_column_double(statement, i);
            status = window->putDouble(addedRows, i, value);
            if (status) {
                LOG_WINDOW("Failed allocating space for a double in column %d, error=%d",
                        i, status);
                result = CPR_FULL;
                break;
            }
            LOG_WINDOW("%d,%d is FLOAT %lf", startPos + addedRows, i, value);
        } else if (type == SQLITE_BLOB) {
            // BLOB data
            const void* blob = sqlite3_column_blob(statement, i);
            size_t size = sqlite3_column_bytes(statement, i);
            status = window->putBlob(addedRows, i, blob, size);
            if (status) {
                LOG_WINDOW("Failed allocating %u bytes for blob at %d,%d, error=%d",
                        size, startPos + addedRows, i, status);
                result = CPR_FULL;
                break;
            }
            LOG_WINDOW("%d,%d is Blob with %u bytes",
                    startPos + addedRows, i, size);
        } else if (type == SQLITE_NULL) {
            // NULL field
            status = window->putNull(addedRows, i);
            if (status) {
                LOG_WINDOW("Failed allocating space for a null in column %d, error=%d",
                        i, status);
                result = CPR_FULL;
                break;
            }

            LOG_WINDOW("%d,%d is NULL", startPos + addedRows, i);
        } else {
            // Unknown data
            ALOGE("Unknown column type when filling database window");
            throw_sqlite3_exception( "Unknown column type when filling window");
            result = CPR_ERROR;
            break;
        }
    }

    // Free the last row if if was not successfully copied.
    if (result != CPR_OK) {
        window->freeLastRow();
    }
    return result;
}

static KLong nativeExecuteForCursorWindow(KLong connectionPtr, KLong statementPtr, KLong windowPtr,
        KInt startPos, KInt requiredPos, KBoolean countAllRows) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);
    CursorWindow* window = reinterpret_cast<CursorWindow*>(windowPtr);

    status_t status = window->clear();
    if (status) {
    //TODO: format and throw
        /*String8 msg;
        msg.appendFormat("Failed to clear the cursor window, status=%d", status);
        throw_sqlite3_exception( connection->db, msg.string());*/
        return 0;
    }

    int numColumns = sqlite3_column_count(statement);
    status = window->setNumColumns(numColumns);
    if (status) {
    //TODO: format and throw
        /*String8 msg;
        msg.appendFormat("Failed to set the cursor window column count to %d, status=%d",
                numColumns, status);
        throw_sqlite3_exception( connection->db, msg.string());*/
        return 0;
    }

    int retryCount = 0;
    int totalRows = 0;
    int addedRows = 0;
    bool windowFull = false;
    bool gotException = false;
    while (!gotException && (!windowFull || countAllRows)) {
        int err = sqlite3_step(statement);
        if (err == SQLITE_ROW) {
            LOG_WINDOW("Stepped statement %p to row %d", statement, totalRows);
            retryCount = 0;
            totalRows += 1;

            // Skip the row if the window is full or we haven't reached the start position yet.
            if (startPos >= totalRows || windowFull) {
                continue;
            }

            CopyRowResult cpr = copyRow(window, statement, numColumns, startPos, addedRows);
            if (cpr == CPR_FULL && addedRows && startPos + addedRows <= requiredPos) {
                // We filled the window before we got to the one row that we really wanted.
                // Clear the window and start filling it again from here.
                // TODO: Would be nicer if we could progressively replace earlier rows.
                window->clear();
                window->setNumColumns(numColumns);
                startPos += addedRows;
                addedRows = 0;
                cpr = copyRow(window, statement, numColumns, startPos, addedRows);
            }

            if (cpr == CPR_OK) {
                addedRows += 1;
            } else if (cpr == CPR_FULL) {
                windowFull = true;
            } else {
                gotException = true;
            }
        } else if (err == SQLITE_DONE) {
            // All rows processed, bail
            LOG_WINDOW("Processed all rows");
            break;
        } else if (err == SQLITE_LOCKED || err == SQLITE_BUSY) {
            // The table is locked, retry
            LOG_WINDOW("Database locked, retrying");
            if (retryCount > 50) {
                ALOGE("Bailing on database busy retry");
                throw_sqlite3_exception( connection->db, "retrycount exceeded");
                gotException = true;
            } else {
                // Sleep to give the thread holding the lock a chance to finish
                usleep(1000);
                retryCount++;
            }
        } else {
            throw_sqlite3_exception( connection->db);
            gotException = true;
        }
    }

    LOG_WINDOW("Resetting statement %p after fetching %d rows and adding %d rows"
            "to the window in %d bytes",
            statement, totalRows, addedRows, window->size() - window->freeSpace());
    sqlite3_reset(statement);

    // Report the total number of rows on request.
    if (startPos > totalRows) {
        ALOGE("startPos %d > actual rows %d", startPos, totalRows);
    }
    KLong result = KLong(startPos) << 32 | KLong(totalRows);
    return result;
}

extern "C" KLong Android_Database_SQLiteConnection_nativeExecuteForCursorWindow(
    KLong connectionPtr, KLong statementPtr, KLong windowPtr,
                                      KInt startPos, KInt requiredPos, KBoolean countAllRows)
{
    return nativeExecuteForCursorWindow(
                   connectionPtr, statementPtr, windowPtr,
                   startPos, requiredPos, countAllRows);
}

static KInt nativeGetDbLookaside(KLong connectionPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);

    int cur = -1;
    int unused;
    sqlite3_db_status(connection->db, SQLITE_DBSTATUS_LOOKASIDE_USED, &cur, &unused, 0);
    return cur;
}

extern "C" KInt Android_Database_SQLiteConnection_nativeGetDbLookaside(
    KLong connectionPtr)
{
    return nativeGetDbLookaside(connectionPtr);
}

static void nativeCancel(KLong connectionPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    connection->canceled = true;
}

extern "C" void Android_Database_SQLiteConnection_nativeCancel(
    KLong connectionPtr)
{
    nativeCancel(connectionPtr);
}

static void nativeResetCancel(KLong connectionPtr,
        KBoolean cancelable) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    connection->canceled = false;

    if (cancelable) {
        sqlite3_progress_handler(connection->db, 4, sqliteProgressHandlerCallback,
                connection);
    } else {
        sqlite3_progress_handler(connection->db, 0, NULL, NULL);
    }
}

extern "C" void Android_Database_SQLiteConnection_nativeResetCancel(
    KLong connectionPtr, KBoolean cancelable)
{
    nativeResetCancel(connectionPtr, cancelable) ;
}

/*static JNINativeMethod sMethods[] =
{
    { "nativeOpen", "(Ljava/lang/String;ILjava/lang/String;ZZ)J",
            (void*)nativeOpen },
    { "nativeClose", "(J)V",
            (void*)nativeClose },
    { "nativeRegisterCustomFunction", "(JLandroid/database/sqlite/SQLiteCustomFunction;)V",
            (void*)nativeRegisterCustomFunction },
    { "nativeRegisterLocalizedCollators", "(JLjava/lang/String;)V",
            (void*)nativeRegisterLocalizedCollators },
    { "nativePrepareStatement", "(JLjava/lang/String;)J",
            (void*)nativePrepareStatement },
    { "nativeFinalizeStatement", "(JJ)V",
            (void*)nativeFinalizeStatement },
    { "nativeGetParameterCount", "(JJ)I",
            (void*)nativeGetParameterCount },
    { "nativeIsReadOnly", "(JJ)Z",
            (void*)nativeIsReadOnly },
    { "nativeGetColumnCount", "(JJ)I",
            (void*)nativeGetColumnCount },
    { "nativeGetColumnName", "(JJI)Ljava/lang/String;",
            (void*)nativeGetColumnName },
    { "nativeBindNull", "(JJI)V",
            (void*)nativeBindNull },
    { "nativeBindLong", "(JJIJ)V",
            (void*)nativeBindLong },
    { "nativeBindDouble", "(JJID)V",
            (void*)nativeBindDouble },
    { "nativeBindString", "(JJILjava/lang/String;)V",
            (void*)nativeBindString },
    { "nativeBindBlob", "(JJI[B)V",
            (void*)nativeBindBlob },
    { "nativeResetStatementAndClearBindings", "(JJ)V",
            (void*)nativeResetStatementAndClearBindings },
    { "nativeExecute", "(JJ)V",
            (void*)nativeExecute },
    { "nativeExecuteForLong", "(JJ)J",
            (void*)nativeExecuteForLong },
    { "nativeExecuteForString", "(JJ)Ljava/lang/String;",
            (void*)nativeExecuteForString },
    { "nativeExecuteForBlobFileDescriptor", "(JJ)I",
            (void*)nativeExecuteForBlobFileDescriptor },
    { "nativeExecuteForChangedRowCount", "(JJ)I",
            (void*)nativeExecuteForChangedRowCount },
    { "nativeExecuteForLastInsertedRowId", "(JJ)J",
            (void*)nativeExecuteForLastInsertedRowId },
    { "nativeExecuteForCursorWindow", "(JJJIIZ)J",
            (void*)nativeExecuteForCursorWindow },
    { "nativeGetDbLookaside", "(J)I",
            (void*)nativeGetDbLookaside },
    { "nativeCancel", "(J)V",
            (void*)nativeCancel },
    { "nativeResetCancel", "(JZ)V",
            (void*)nativeResetCancel },
};*/


//TODO: This?
/*#define FIND_CLASS(var, className) \
        var = env->FindClass(className); \
        LOG_FATAL_IF(! var, "Unable to find class " className);

#define GET_METHOD_ID(var, clazz, methodName, fieldDescriptor) \
        var = env->GetMethodID(clazz, methodName, fieldDescriptor); \
        LOG_FATAL_IF(! var, "Unable to find method" methodName);

#define GET_FIELD_ID(var, clazz, fieldName, fieldDescriptor) \
        var = env->GetFieldID(clazz, fieldName, fieldDescriptor); \
        LOG_FATAL_IF(! var, "Unable to find field " fieldName);*/

//TODO: This?
/*int register_android_database_SQLiteConnection(JNIEnv *env)
{
    jclass clazz;
    FIND_CLASS(clazz, "android/database/sqlite/SQLiteCustomFunction");

    GET_FIELD_ID(gSQLiteCustomFunctionClassInfo.name, clazz,
            "name", "Ljava/lang/String;");
    GET_FIELD_ID(gSQLiteCustomFunctionClassInfo.numArgs, clazz,
            "numArgs", "I");
    GET_METHOD_ID(gSQLiteCustomFunctionClassInfo.dispatchCallback,
            clazz, "dispatchCallback", "([Ljava/lang/String;)V");

    FIND_CLASS(clazz, "java/lang/String");
    gStringClassInfo.clazz = jclass(env->NewGlobalRef(clazz));

    //return AndroidRuntime::registerNativeMethods(env, "android/database/sqlite/SQLiteConnection",
    //        sMethods, NELEM(sMethods));
    return 0;
}*/

} // namespace android
