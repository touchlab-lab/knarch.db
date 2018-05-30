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
#include "KonanHelper.h"

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
    char* path;
    char* label;

    volatile bool canceled;

    //TODO: WTF is KStdString?
    SQLiteConnection(sqlite3* db, int openFlags, char* path, char* label) :
        db(db), openFlags(openFlags), path(path), label(label), canceled(false) { }

        ~SQLiteConnection(){
        if(path != nullptr)
            DisposeCStringHelper(path);
        if(label != nullptr)
            DisposeCStringHelper(label);
    }
};

// Called each time a statement begins execution, when tracing is enabled.
static void sqliteTraceCallback(void *data, const char *sql) {
    SQLiteConnection* connection = static_cast<SQLiteConnection*>(data);
    ALOGV("%s: \"%s\"\n",
            connection->label, sql);
}

// Called each time a statement finishes execution, when profiling is enabled.
static void sqliteProfileCallback(void *data, const char *sql, sqlite3_uint64 tm) {
    SQLiteConnection* connection = static_cast<SQLiteConnection*>(data);
    ALOGV("%s: \"%s\" took %0.3f ms\n",
            connection->label, sql, tm * 0.000001f);
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

    size_t utf8Size;
    char * path = CreateCStringFromStringWithSize(pathStr, &utf8Size);
    char * label = CreateCStringFromStringWithSize(labelStr, &utf8Size);

//    const KChar* utf16 = CharArrayAddressOfElementAt(pathStr, 0);
//    ALOGW("A 2a");
//    utf8::with_replacement::utf16to8(utf16, utf16 + pathStr->count_, back_inserter(path));
//    ALOGW("A 2b");
//
//    utf16 = CharArrayAddressOfElementAt(labelStr, 0);
//    ALOGW("A 2c");
//    utf8::with_replacement::utf16to8(utf16, utf16 + labelStr->count_, back_inserter(label));
//    ALOGW("A 2d");

    sqlite3* db;
    int err = sqlite3_open_v2(path, &db, sqliteFlags, NULL);
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

    printf("Opened connection %p with label '%s'", db, label);
    ALOG("ASDF", "Opened connection %p with label '%s'", db, label);
    return reinterpret_cast<KLong>(connection);
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

static KLong nativePrepareStatement(KLong connectionPtr, KString sqlString) {

    RuntimeAssert(sqlString->type_info() == theStringTypeInfo, "Must use a string");

    auto connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);

    KInt sqlLength = sqlString->count_;

    const KChar* sql = CharArrayAddressOfElementAt(sqlString, 0);

    sqlite3_stmt* statement;
    int err = sqlite3_prepare16_v2(connection->db,
            sql, sqlLength * sizeof(KChar), &statement, NULL);

    if (err != SQLITE_OK) {
        // Error messages like 'near ")": syntax error' are not
        // always helpful enough, so construct an error string that
        // includes the query itself.

        std::string str;

        size_t utf8size;
        str.append(", while compiling: ");

        char* hardString = CreateCStringFromStringWithSize(sqlString, &utf8size);
        str.append(const_cast<const char *>(hardString));
        DisposeCStringHelper(hardString);

        throw_sqlite3_exception(connection->db, str.c_str());
        return 0;
    }

    ALOGV("Prepared statement %p on connection %p", statement, connection->db);
    return reinterpret_cast<KLong>(statement);
}

static void nativeFinalizeStatement(KLong connectionPtr, KLong statementPtr) {
    auto connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    auto statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    // We ignore the result of sqlite3_finalize because it is really telling us about
    // whether any errors occurred while executing the statement.  The statement itself
    // is always finalized regardless.
    ALOGV("Finalized statement %p on connection %p", statement, connection->db);
    sqlite3_finalize(statement);
}

static KInt nativeGetParameterCount(KLong connectionPtr, KLong statementPtr) {
    auto statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    return sqlite3_bind_parameter_count(statement);
}

static KBoolean nativeIsReadOnly(KLong connectionPtr, KLong statementPtr) {
    auto statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    return sqlite3_stmt_readonly(statement) != 0;
}

static KInt nativeGetColumnCount(KLong connectionPtr, KLong statementPtr) {
    auto * statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    return sqlite3_column_count(statement);
}

static size_t lengthOfString(const KChar* wstr)
{
    auto p = (KChar*)wstr;
    size_t len = 0;
    while(*p != 0){
        p++;
        len++;
    }

    return len;
}

static void nativeBindNull(KLong connectionPtr, KLong statementPtr, KInt index) {
    auto * connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    auto * statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = sqlite3_bind_null(statement, index);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception( connection->db, NULL);
    }
}

static void nativeBindLong(KLong connectionPtr, KLong statementPtr, KInt index, KLong value) {
    auto * connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    auto * statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = sqlite3_bind_int64(statement, index, value);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception( connection->db, NULL);
    }
}

static void nativeBindDouble(KLong connectionPtr, KLong statementPtr, KInt index, KDouble value) {
    auto * connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    auto * statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = sqlite3_bind_double(statement, index, value);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception( connection->db, NULL);
    }
}

static void nativeBindString(KLong connectionPtr, KLong statementPtr, KInt index, KString valueString) {
    auto * connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    auto * statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    KInt valueLength = valueString->count_;

    const KChar* value = CharArrayAddressOfElementAt(valueString, 0);
    int err = sqlite3_bind_text16(statement, index, value, valueLength * sizeof(KChar),
            SQLITE_TRANSIENT);

    if (err != SQLITE_OK) {
        throw_sqlite3_exception( connection->db, NULL);
    }
}

static void nativeBindBlob(KLong connectionPtr, KLong statementPtr, KInt index, KConstRef valueArray) {
    auto * connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    auto * statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    const ArrayHeader* array = valueArray->array();

    KInt valueLength = array->count_;
    const auto * value = ByteArrayAddressOfElementAt(array, 0);
    //TODO: Do *we* need to copy the array?
    int err = sqlite3_bind_blob(statement, index, value, valueLength, SQLITE_TRANSIENT);

    if (err != SQLITE_OK) {
        throw_sqlite3_exception( connection->db, NULL);
    }
}

static void nativeResetStatementAndClearBindings(KLong connectionPtr, KLong statementPtr) {
    auto * connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    auto * statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = sqlite3_reset(statement);
    if (err == SQLITE_OK) {
        err = sqlite3_clear_bindings(statement);
    }
    if (err != SQLITE_OK) {
        throw_sqlite3_exception( connection->db, NULL);
    }
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
    auto connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    auto statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    executeNonQuery(connection, statement);
}

static KInt nativeExecuteForChangedRowCount(KLong connectionPtr, KLong statementPtr) {
    auto connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    auto statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = executeNonQuery(connection, statement);
    return err == SQLITE_DONE ? sqlite3_changes(connection->db) : -1;
}

static KLong nativeExecuteForLastInsertedRowId(KLong connectionPtr, KLong statementPtr) {
    auto connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    auto statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = executeNonQuery(connection, statement);
    return err == SQLITE_DONE && sqlite3_changes(connection->db) > 0
            ? sqlite3_last_insert_rowid(connection->db) : -1;
}

static int executeOneRowQuery(SQLiteConnection* connection, sqlite3_stmt* statement) {
    int err = sqlite3_step(statement);
    if (err != SQLITE_ROW) {
        throw_sqlite3_exception(connection->db);
    }
    return err;
}

static KLong nativeExecuteForLong(KLong connectionPtr, KLong statementPtr) {
    auto connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    auto statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = executeOneRowQuery(connection, statement);
    if (err == SQLITE_ROW && sqlite3_column_count(statement) >= 1) {
        return sqlite3_column_int64(statement, 0);
    }
    return -1;
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
    auto connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    auto statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);
    auto window = reinterpret_cast<CursorWindow*>(windowPtr);

    status_t status = window->clear();
    if (status) {
        char buff[100];
        snprintf(buff, sizeof(buff), "Failed to clear the cursor window, status=%d", status);
        throw_sqlite3_exception( connection->db, const_cast<const char*>(buff));
        return 0;
    }

    int numColumns = sqlite3_column_count(statement);
    status = window->setNumColumns(numColumns);
    if (status) {
        char buff[100];
        snprintf(buff, sizeof(buff), "Failed to set the cursor window column count to %d, status=%d",
                 numColumns, status);
        throw_sqlite3_exception( connection->db, const_cast<const char*>(buff));
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

static KInt nativeGetDbLookaside(KLong connectionPtr) {
    auto connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);

    int cur = -1;
    int unused;
    sqlite3_db_status(connection->db, SQLITE_DBSTATUS_LOOKASIDE_USED, &cur, &unused, 0);
    return cur;
}

static void nativeCancel(KLong connectionPtr) {
    auto connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    connection->canceled = true;
}

static void nativeResetCancel(KLong connectionPtr,
        KBoolean cancelable) {
    auto connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    connection->canceled = false;

    if (cancelable) {
        sqlite3_progress_handler(connection->db, 4, sqliteProgressHandlerCallback,
                connection);
    } else {
        sqlite3_progress_handler(connection->db, 0, NULL, NULL);
    }
}

extern "C"{
KLong Android_Database_SQLiteConnection_nativeOpen(KRef thiz, KString pathStr, KInt openFlags,
                                                   KString labelStr, KBoolean enableTrace, KBoolean enableProfile)
{
    return nativeOpen(pathStr, openFlags,
                      labelStr, enableTrace, enableProfile);
}

void Android_Database_SQLiteConnection_nativeClose(KRef thiz, KLong connectionPtr)
{
    nativeClose(connectionPtr);
}

KLong Android_Database_SQLiteConnection_nativePrepareStatement(KRef thiz, KLong connectionPtr, KString sqlString)
{
    return nativePrepareStatement(connectionPtr, sqlString);
}

void Android_Database_SQLiteConnection_nativeFinalizeStatement(KRef thiz,
                                                               KLong connectionPtr, KLong statementPtr)
{
    nativeFinalizeStatement(connectionPtr, statementPtr);
}

KInt Android_Database_SQLiteConnection_nativeGetParameterCount(KRef thiz,
                                                               KLong connectionPtr, KLong statementPtr)
{
    return nativeGetParameterCount(connectionPtr, statementPtr);
}

KBoolean Android_Database_SQLiteConnection_nativeIsReadOnly(KRef thiz,
                                                            KLong connectionPtr, KLong statementPtr)
{
    return nativeIsReadOnly(connectionPtr, statementPtr);
}

KInt Android_Database_SQLiteConnection_nativeGetColumnCount(KRef thiz,
                                                            KLong connectionPtr, KLong statementPtr)
{
    return nativeGetColumnCount(connectionPtr, statementPtr);
}

OBJ_GETTER(Android_Database_SQLiteConnection_nativeGetColumnName, KRef thiz, KLong connectionPtr, KLong statementPtr, KInt index){
    auto * statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    const auto * name = static_cast<const KChar*>(sqlite3_column_name16(statement, index));
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

void Android_Database_SQLiteConnection_nativeBindNull(KRef thiz,
                                                      KLong connectionPtr, KLong statementPtr, KInt index)
{
    nativeBindNull(connectionPtr, statementPtr, index);
}

void Android_Database_SQLiteConnection_nativeBindLong(KRef thiz,
                                                      KLong connectionPtr, KLong statementPtr, KInt index, KLong value)
{
    nativeBindLong(connectionPtr, statementPtr, index, value);
}

void Android_Database_SQLiteConnection_nativeBindDouble(KRef thiz,
                                                        KLong connectionPtr, KLong statementPtr, KInt index, KDouble value)
{
    nativeBindDouble(connectionPtr, statementPtr, index, value);
}

void Android_Database_SQLiteConnection_nativeBindString(KRef thiz,
                                                        KLong connectionPtr, KLong statementPtr, KInt index, KString valueString)
{
    nativeBindString(connectionPtr, statementPtr, index, valueString);
}

void Android_Database_SQLiteConnection_nativeBindBlob(KRef thiz,
                                                      KLong connectionPtr, KLong statementPtr, KInt index, KConstRef valueArray)
{
    nativeBindBlob(connectionPtr, statementPtr, index, valueArray);
}

void Android_Database_SQLiteConnection_nativeResetStatementAndClearBindings(KRef thiz,
                                                                            KLong connectionPtr, KLong statementPtr)
{
    nativeResetStatementAndClearBindings(connectionPtr, statementPtr);
}

void Android_Database_SQLiteConnection_nativeExecute(KRef thiz,
                                                     KLong connectionPtr, KLong statementPtr)
{
    nativeExecute(connectionPtr, statementPtr);
}

KInt Android_Database_SQLiteConnection_nativeExecuteForChangedRowCount(KRef thiz,
                                                                       KLong connectionPtr, KLong statementPtr)
{
    return nativeExecuteForChangedRowCount(connectionPtr, statementPtr);
}

KLong Android_Database_SQLiteConnection_nativeExecuteForLastInsertedRowId(KRef thiz,
                                                                          KLong connectionPtr, KLong statementPtr)
{
    return nativeExecuteForLastInsertedRowId(connectionPtr, statementPtr);
}

KLong Android_Database_SQLiteConnection_nativeExecuteForLong(KRef thiz,
                                                             KLong connectionPtr, KLong statementPtr)
{
    return nativeExecuteForLong(connectionPtr, statementPtr);
}

OBJ_GETTER(Android_Database_SQLiteConnection_nativeExecuteForString, KRef thiz, KLong connectionPtr, KLong statementPtr){
    auto connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    auto statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = executeOneRowQuery(connection, statement);
    if (err == SQLITE_ROW && sqlite3_column_count(statement) >= 1) {
        auto text = static_cast<const KChar*>(sqlite3_column_text16(statement, 0));
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

KLong Android_Database_SQLiteConnection_nativeExecuteForCursorWindow(KRef thiz,
                                                                     KLong connectionPtr, KLong statementPtr, KLong windowPtr,
                                                                     KInt startPos, KInt requiredPos, KBoolean countAllRows)
{
    return nativeExecuteForCursorWindow(
            connectionPtr, statementPtr, windowPtr,
            startPos, requiredPos, countAllRows);
}

KInt Android_Database_SQLiteConnection_nativeGetDbLookaside(KRef thiz,
                                                            KLong connectionPtr)
{
    return nativeGetDbLookaside(connectionPtr);
}

void Android_Database_SQLiteConnection_nativeCancel(KRef thiz,
                                                    KLong connectionPtr)
{
    nativeCancel(connectionPtr);
}

void Android_Database_SQLiteConnection_nativeResetCancel(KRef thiz,
                                                         KLong connectionPtr, KBoolean cancelable)
{
    nativeResetCancel(connectionPtr, cancelable) ;
}

}

} // namespace android
