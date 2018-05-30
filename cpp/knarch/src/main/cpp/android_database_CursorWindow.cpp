/*
 * Copyright (C) 2007 The Android Open Source Project
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

#undef LOG_TAG
#define LOG_TAG "CursorWindow"

#include "Assert.h"
#include "Memory.h"
#include "Natives.h"
#include "KString.h"
#include "KonanHelper.h"
#include "Porting.h"
#include "Types.h"

#include "utf8.h"

#include "UtilsErrors.h"

#include <stdio.h>
#include <string.h>
#include <unistd.h>

#include "AndroidfwCursorWindow.h"

#include "android_database_SQLiteCommon.h"


namespace android {

    //TODO: We're using KLong to point to alloced memory, but maybe we want to use a pointer type?

/*
static struct {
    jfieldID data;
    jfieldID sizeCopied;
} gCharArrayBufferClassInfo;
*/

//static jstring gEmptyString;

    static void throwExceptionWithRowCol(KInt row, KInt column) {
        char exceptionMessage[150];
        snprintf(exceptionMessage, sizeof(exceptionMessage), "Couldn't read row %d, col %d from CursorWindow.  Make sure the Cursor is initialized correctly before accessing data from it.", row, column);
        ThrowSql_IllegalStateException(makeKString(exceptionMessage));
    }

    static void throwUnknownTypeException(KInt type) {
        char exceptionMessage[50];
        snprintf(exceptionMessage, sizeof(exceptionMessage), "UNKNOWN type %d", type);
        ThrowSql_IllegalStateException(makeKString(exceptionMessage));
    }

    static KLong nativeCreate(KString nameObj, KInt cursorWindowSize) {

        //TODO: Do we need a fresh copy of name?
        CursorWindow *window;
    status_t status = CursorWindow::create(nameObj, cursorWindowSize, &window);
        if (status || !window) {
            ALOGE("Could not allocate CursorWindow '%s' of size %d due to error %d.",
                  nameObj, cursorWindowSize, status);
            return 0;
        }

        LOG_WINDOW("nativeInitializeEmpty: window = %p", window);
        return reinterpret_cast<KLong>(window);
    }

    static void nativeDispose(KLong windowPtr) {
        CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
        if (window) {
            LOG_WINDOW("Closing window %p", window);
            delete window;
        }
    }

    static KString nativeGetName(KLong windowPtr) {
        CursorWindow* window = reinterpret_cast<CursorWindow*>(windowPtr);
        return window->name();
    }

    static void nativeClear(KLong windowPtr) {
        CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
        LOG_WINDOW("Clearing window %p", window);
        status_t status = window->clear();
        if (status) {
            LOG_WINDOW("Could not clear window. error=%d", status);
        }
    }

    static KInt nativeGetNumRows(KLong windowPtr) {
        CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
        return window->getNumRows();
    }

    static KBoolean nativeSetNumColumns(KLong windowPtr, KInt columnNum) {
        CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
        status_t status = window->setNumColumns(columnNum);
        return status == OK;
    }

    static KBoolean nativeAllocRow(KLong windowPtr) {
        CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
        status_t status = window->allocRow();
        return status == OK;
    }

    static void nativeFreeLastRow(KLong windowPtr) {
        CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
        window->freeLastRow();
    }

    static KInt nativeGetType(KLong windowPtr, KInt row, KInt column) {
        CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
        LOG_WINDOW("returning column type affinity for %d,%d from %p", row, column, window);

        CursorWindow::FieldSlot *fieldSlot = window->getFieldSlot(row, column);
        if (!fieldSlot) {
            // FIXME: This is really broken but we have CTS tests that depend
            // on this legacy behavior.
            //throwExceptionWithRowCol(env, row, column);
            return CursorWindow::FIELD_TYPE_NULL;
        }
        return window->getFieldSlotType(fieldSlot);
    }

    static KLong nativeGetLong(KLong windowPtr, KInt row, KInt column) {
        CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
        LOG_WINDOW("Getting long for %d,%d from %p", row, column, window);

        CursorWindow::FieldSlot *fieldSlot = window->getFieldSlot(row, column);
        if (!fieldSlot) {
            throwExceptionWithRowCol(row, column);
            return 0;
        }

        int32_t type = window->getFieldSlotType(fieldSlot);
        if (type == CursorWindow::FIELD_TYPE_INTEGER) {
            return window->getFieldSlotValueLong(fieldSlot);
            //TODO: Figure out type conversion
            /*} else if (type == CursorWindow::FIELD_TYPE_STRING) {
                size_t sizeIncludingNull;
                const char* value = window->getFieldSlotValueString(fieldSlot, &sizeIncludingNull);
                return sizeIncludingNull > 1 ? strtoll(value, NULL, 0) : 0L;
            } else if (type == CursorWindow::FIELD_TYPE_FLOAT) {
                return jlong(window->getFieldSlotValueDouble(fieldSlot));
            } else if (type == CursorWindow::FIELD_TYPE_NULL) {
                return 0;
            } else if (type == CursorWindow::FIELD_TYPE_BLOB) {
                throw_sqlite3_exception(env, "Unable to convert BLOB to long");
                return 0;*/
        } else {
            throwUnknownTypeException(type);
            return 0;
        }
    }

    static KDouble nativeGetDouble(KLong windowPtr, KInt row, KInt column) {
        CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
        LOG_WINDOW("Getting double for %d,%d from %p", row, column, window);

        CursorWindow::FieldSlot *fieldSlot = window->getFieldSlot(row, column);
        if (!fieldSlot) {
            throwExceptionWithRowCol(row, column);
            return 0.0;
        }

        int32_t type = window->getFieldSlotType(fieldSlot);
        if (type == CursorWindow::FIELD_TYPE_FLOAT) {
            return window->getFieldSlotValueDouble(fieldSlot);
            //TODO: Figure out type conversion
            /*} else if (type == CursorWindow::FIELD_TYPE_STRING) {
                size_t sizeIncludingNull;
                const char* value = window->getFieldSlotValueString(fieldSlot, &sizeIncludingNull);
                return sizeIncludingNull > 1 ? strtod(value, NULL) : 0.0;
            } else if (type == CursorWindow::FIELD_TYPE_INTEGER) {
                return jdouble(window->getFieldSlotValueLong(fieldSlot));
            } else if (type == CursorWindow::FIELD_TYPE_NULL) {
                return 0.0;
            } else if (type == CursorWindow::FIELD_TYPE_BLOB) {
                throw_sqlite3_exception(env, "Unable to convert BLOB to double");
                return 0.0;*/
        } else {
            throwUnknownTypeException(type);
            return 0.0;
        }
    }

    static KBoolean nativePutBlob(KLong windowPtr, KConstRef valueObj, KInt row, KInt column) {
        CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);

        const ArrayHeader *array = valueObj->array();

        KInt len = array->count_;

        const KByte *value = ByteArrayAddressOfElementAt(array, 0);
        status_t status = window->putBlob(row, column, value, len);

        if (status) {
            LOG_WINDOW("Failed to put blob. error=%d", status);
            return false;
        }

        LOG_WINDOW("%d,%d is BLOB with %u bytes", row, column, len);
        return true;
    }

    static KBoolean nativePutLong(KLong windowPtr, KLong value, KInt row, KInt column) {
        CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
        status_t status = window->putLong(row, column, value);

        if (status) {
            LOG_WINDOW("Failed to put long. error=%d", status);
            return false;
        }

        LOG_WINDOW("%d,%d is INTEGER 0x%016llx", row, column, value);
        return true;
    }

    static KBoolean nativePutDouble(KLong windowPtr, KDouble value, KInt row, KInt column) {
        CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
        status_t status = window->putDouble(row, column, value);

        if (status) {
            LOG_WINDOW("Failed to put double. error=%d", status);
            return false;
        }

        LOG_WINDOW("%d,%d is FLOAT %lf", row, column, value);
        return true;
    }

    static KBoolean nativePutNull(KLong windowPtr, KInt row, KInt column) {
        CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
        status_t status = window->putNull(row, column);

        if (status) {
            LOG_WINDOW("Failed to put null. error=%d", status);
            return false;
        }

        LOG_WINDOW("%d,%d is NULL", row, column);
        return true;
    }

    extern "C" {

    KLong Android_Database_CursorWindow_nativeCreate(KRef thiz, KString nameObj, KInt cursorWindowSize) {
        return nativeCreate(nameObj, cursorWindowSize);
    }

    void Android_Database_CursorWindow_nativeDispose(KRef thiz, KLong windowPtr) {
        nativeDispose(windowPtr);
    }

    KString Android_Database_CursorWindow_nativeGetName(KRef thiz, KLong windowPtr) {
        return nativeGetName(windowPtr);
    }

    void Android_Database_CursorWindow_nativeClear(KRef thiz, KLong windowPtr) {
        nativeClear(windowPtr);
    }

    KInt Android_Database_CursorWindow_nativeGetNumRows(KRef thiz, KLong windowPtr) {
        return nativeGetNumRows(windowPtr);
    }

    KBoolean Android_Database_CursorWindow_nativeSetNumColumns(KRef thiz, KLong windowPtr, KInt columnNum) {
        return nativeSetNumColumns(windowPtr, columnNum);
    }

    KBoolean Android_Database_CursorWindow_nativeAllocRow(KRef thiz, KLong windowPtr) {
        return nativeAllocRow(windowPtr);
    }

    void Android_Database_CursorWindow_nativeFreeLastRow(KRef thiz, KLong windowPtr) {
        nativeFreeLastRow(windowPtr);
    }

    KInt Android_Database_CursorWindow_nativeGetType(KRef thiz, KLong windowPtr, KInt row, KInt column) {
        return nativeGetType(windowPtr, row, column);
    }

    OBJ_GETTER(Android_Database_CursorWindow_nativeGetBlob, KRef thiz, KLong windowPtr, KInt row, KInt column) {
        throw_sqlite3_exception("Not yet");
        /*CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
        LOG_WINDOW("Getting blob for %d,%d from %p", row, column, window);

        CursorWindow::FieldSlot *fieldSlot = window->getFieldSlot(row, column);
        if (!fieldSlot) {
            throwExceptionWithRowCol(row, column);
            RETURN_OBJ(nullptr);
        }

        KInt type = window->getFieldSlotType(fieldSlot);
        if (type == CursorWindow::FIELD_TYPE_BLOB || type == CursorWindow::FIELD_TYPE_STRING) {
            size_t size;

            const void *value = window->getFieldSlotValueBlob(fieldSlot, &size);

            ArrayHeader *result = AllocArrayInstance(
                    theByteArrayTypeInfo, size, OBJ_RESULT)->array();

            //TODO: How to check if array properly created?
            *//*if (!byteArray) {
                env->ExceptionClear();
                throw_sqlite3_exception(env, "Native could not create new byte[]");
                RETURN_OBJ(nullptr);
            }*//*
            memcpy(PrimitiveArrayAddressOfElementAt<KByte>(result, 0),
                   value,
                   size);

            RETURN_OBJ(result->obj());
        } else if (type == CursorWindow::FIELD_TYPE_INTEGER) {
            throw_sqlite3_exception("INTEGER data in nativeGetBlob ");
        } else if (type == CursorWindow::FIELD_TYPE_FLOAT) {
            throw_sqlite3_exception("FLOAT data in nativeGetBlob ");
        } else if (type == CursorWindow::FIELD_TYPE_NULL) {
            // do nothing
        } else {
            throwUnknownTypeException(type);
        }*/
        RETURN_OBJ(nullptr);
    }

    OBJ_GETTER(Android_Database_CursorWindow_nativeGetString, KRef thiz, KLong windowPtr, KInt row, KInt column) {
        CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);
        LOG_WINDOW("Getting string for %d,%d from %p", row, column, window);

        CursorWindow::FieldSlot *fieldSlot = window->getFieldSlot(row, column);
        if (!fieldSlot) {
            throwExceptionWithRowCol(row, column);
            return NULL;
        }

        int32_t type = window->getFieldSlotType(fieldSlot);
        if (type == CursorWindow::FIELD_TYPE_STRING) {
            size_t sizeIncludingNull;
            const char *value = window->getFieldSlotValueString(fieldSlot, &sizeIncludingNull);
            //TODO: Figure this out
            /*if (sizeIncludingNull <= 1) {
                return gEmptyString;
            }*/
            // Convert to UTF-16 here instead of calling NewStringUTF.  NewStringUTF
            // doesn't like UTF-8 strings with high codepoints.  It actually expects
            // Modified UTF-8 with encoded surrogate pairs.

            //TODO: FIGURE OUT UTF8
//        RETURN_RESULT_OF(CreateStringFromUtf8(value, (uint32_t)(sizeIncludingNull - 1)));
            RETURN_RESULT_OF(CreateStringFromUtf8, value, sizeIncludingNull - 1);
//        RETURN_RESULT_OF(CreateStringFromCString, value);
            //TODO: make sure we should be dropping 1
            //TODO: Tricky string magic
            /*const char* end = value + (sizeIncludingNull - 1);
            uint32_t charCount = utf8::with_replacement::utf16_length(value, end);
            RETURN_RESULT_OF(utf8ToUtf16Impl<utf8::with_replacement::utf8to16>, value, end, charCount);


            ArrayHeader* result = AllocArrayInstance(theStringTypeInfo, charCount, OBJ_RESULT)->array();
            KChar* rawResult = CharArrayAddressOfElementAt(result, 0);
            auto convertResult = conversion(value, end, rawResult);
            RETURN_OBJ(result->obj());*/

        } else if (type == CursorWindow::FIELD_TYPE_INTEGER) {
            throw_sqlite3_exception("NOT CONVERTING YET");
            //TODO: Format number
            /*int64_t value = window->getFieldSlotValueLong(fieldSlot);
            char buf[32];
            snprintf(buf, sizeof(buf), "%" PRId64, value);
            return env->NewStringUTF(buf);*/
            return NULL;
        } else if (type == CursorWindow::FIELD_TYPE_FLOAT) {
            throw_sqlite3_exception("NOT CONVERTING YET");
            //TODO: Format number
            /*double value = window->getFieldSlotValueDouble(fieldSlot);
            char buf[32];
            snprintf(buf, sizeof(buf), "%g", value);
            return env->NewStringUTF(buf);*/
            return NULL;
        } else if (type == CursorWindow::FIELD_TYPE_NULL) {
            return NULL;
        } else if (type == CursorWindow::FIELD_TYPE_BLOB) {
            throw_sqlite3_exception("Unable to convert BLOB to string");
            return NULL;
        } else {
            throwUnknownTypeException(type);
            return NULL;
        }
    }

    KLong Android_Database_CursorWindow_nativeGetLong(KRef thiz, KLong windowPtr, KInt row, KInt column) {
        return nativeGetLong(windowPtr, row, column);
    }

    KDouble Android_Database_CursorWindow_nativeGetDouble(KRef thiz, KLong windowPtr, KInt row, KInt column) {
        return nativeGetDouble(windowPtr, row, column);
    }

    KBoolean
    Android_Database_CursorWindow_nativePutBlob(KRef thiz, KLong windowPtr, KConstRef valueObj, KInt row, KInt column) {
        return nativePutBlob(windowPtr, valueObj, row, column);
    }

    KBoolean
    Android_Database_CursorWindow_nativePutString(KRef thiz, KLong windowPtr, KString valueObj, KInt row, KInt column) {
        CursorWindow *window = reinterpret_cast<CursorWindow *>(windowPtr);

        size_t sizeIncludingNull;

        char *strBytes = CreateCStringFromStringWithSize(valueObj, &sizeIncludingNull);

        status_t status = window->putString(row, column, (const char *) strBytes, sizeIncludingNull);

        DisposeCStringHelper(strBytes);

        if (status) {
            LOG_WINDOW("Failed to put string. error=%d", status);
            return false;
        }

        LOG_WINDOW("%d,%d is TEXT with %u bytes", row, column, sizeIncludingNull);
        return true;
    }

    KBoolean
    Android_Database_CursorWindow_nativePutLong(KRef thiz, KLong windowPtr, KLong value, KInt row, KInt column) {
        return nativePutLong(windowPtr, value, row, column);
    }

    KBoolean
    Android_Database_CursorWindow_nativePutDouble(KRef thiz, KLong windowPtr, KDouble value, KInt row, KInt column) {
        return nativePutDouble(windowPtr, value, row, column);
    }

    KBoolean Android_Database_CursorWindow_nativePutNull(KRef thiz, KLong windowPtr, KInt row, KInt column) {
        return nativePutNull(windowPtr, row, column);
    }
    }
} // namespace android