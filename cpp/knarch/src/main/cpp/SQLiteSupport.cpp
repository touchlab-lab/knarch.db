//
// Created by Kevin Galligan on 6/8/18.
//

#include "lrucache.hpp"
#include <string>
#include <pthread.h>
#include "Types.h"
#include "Natives.h"
#include "utf8.h"

extern "C" {
void finalizeStmt(KLong connectionPtr, KNativePtr ptr);
}

namespace {

    KStdString makeStdString(KString kstring) {
        const KChar *utf16 = CharArrayAddressOfElementAt(kstring, 0);
        KStdString utf8;
        utf8::unchecked::utf16to8(utf16, utf16 + kstring->count_, back_inserter(utf8));
        return utf8;
    }

    class Locker {
    public:
        explicit Locker(pthread_mutex_t *lock) : lock_(lock) {
            pthread_mutex_lock(lock_);
        }

        ~Locker() {
            pthread_mutex_unlock(lock_);
        }

    private:
        pthread_mutex_t *lock_;
    };

    class DatabaseInfo {
    public:
        DatabaseInfo(KInt maxCacheSize) : stmtCache(maxCacheSize) {

        }

        void putStmt(KLong connectionPtr, KString kstring, KRef stmtRef) {
            KStdString utf8 = makeStdString(kstring);
            KNativePtr stmt = CreateStablePointer(stmtRef);
            KNativePtr removedPair = stmtCache.put(utf8, stmt);
            if (removedPair != nullptr) {
                removeStmt(connectionPtr, removedPair);
            }
        }

        void evictAll(KLong connectionPtr) {
            auto all = stmtCache.allEntries();
            std::list<std::pair<KStdString, KNativePtr>>::const_iterator iterator;
            for (iterator = all.begin(); iterator != all.end(); ++iterator) {
                if (stmtCache.exists(iterator->first))
                    removeStmt(connectionPtr, iterator->second);
            }
            stmtCache.removeAll();
        }

        void remove(KLong connectionPtr, KString sql) {
            auto key = makeStdString(sql);
            if (stmtCache.exists(key)) {
                removeStmt(connectionPtr, stmtCache.get(key));
                stmtCache.remove(key);
            }
        }

        KRef getStmt(KString sql) {
            auto key = makeStdString(sql);
            if (stmtCache.exists(key))
                return (KRef) stmtCache.get(key);
            else
                return nullptr;
        }

    private:
        void removeStmt(KLong connectionPtr, KNativePtr stmtPtr) {
            finalizeStmt(connectionPtr, stmtPtr);
            DisposeStablePointer(stmtPtr);
        }

        cache::lru_cache<KStdString, KNativePtr> stmtCache;
    };


    class SQLiteState {
    public:
        SQLiteState() {
            pthread_mutex_init(&lock_, nullptr);
        }

        ~SQLiteState() {
            pthread_mutex_destroy(&lock_);
        }

        void putStmt(KInt dataId, KLong connectionPtr, KString sql, KRef stmt) {
            Locker locker(&lock_);
            auto it = data_.find(dataId);
            it->second->putStmt(connectionPtr, sql, stmt);
        }

        KRef getStmt(KInt dataId, KString sql) {
            Locker locker(&lock_);
            auto it = data_.find(dataId);
            return it->second->getStmt(sql);
        }

        void evictAll(KInt dataId, KLong connectionPtr) {
            Locker locker(&lock_);
            auto it = data_.find(dataId);
            it->second->evictAll(connectionPtr);
        }

        void remove(KInt dataId, KLong connectionPtr, KString sql) {
            Locker locker(&lock_);
            auto it = data_.find(dataId);
            it->second->remove(connectionPtr, sql);
        }

        KInt nextDataId() {
            Locker locker(&lock_);
            return currentDataId_++;
        }

        void createDataStore(KInt dataId, KInt maxCacheSize) {
            Locker locker(&lock_);
            data_[dataId] = new DatabaseInfo(maxCacheSize);
        }

        void removeDataStore(KInt dataId) {
            Locker locker(&lock_);

            auto it = data_.find(dataId);
            if (it == data_.end()) return;
            DatabaseInfo* removing = it->second;
            data_.erase(dataId);
            delete removing;
        }

        void putHelperInfo(KInt dataId, KRef helperInfo) {
            Locker locker(&lock_);
            removeHelperInfo(dataId);
            if(helperInfo != nullptr) {
                helperData_[dataId] = CreateStablePointer(helperInfo);
            }
        }

        KRef getHelperInfo(KInt dataId) {
            Locker locker(&lock_);
            auto it = helperData_.find(dataId);
            if (it != helperData_.end()) {
                return (KRef)it->second;
            } else {
                return nullptr;
            }
        }

        KInt nextHelperInfoId() {
            Locker locker(&lock_);
            return currentHelperInfoId_++;
        }

    private:

        void removeHelperInfo(KInt dataId) {
            auto it = helperData_.find(dataId);
            if (it != helperData_.end()) {
                DisposeStablePointer(it->second);
                helperData_.erase(it);
            }
        }

        pthread_mutex_t lock_;
        KStdUnorderedMap<KInt, DatabaseInfo *> data_;
        KStdUnorderedMap<KInt, KNativePtr> helperData_;
        KInt currentDataId_;
        KInt currentHelperInfoId_;
    };

    SQLiteState *dataState() {
        static SQLiteState *state = nullptr;

        if (state != nullptr) {
            return state;
        }

        SQLiteState *result = konanConstructInstance<SQLiteState>();

        SQLiteState *old = __sync_val_compare_and_swap(&state, nullptr, result);

        if (old != nullptr) {
            konanDestructInstance(result);
            // Someone else inited this data.
            return old;
        }

        return state;
    }
}

extern "C" {

void SQLiteSupport_putStmt(KInt dataId, KLong connectionPtr, KString sql, KRef stmt) {
    dataState()->putStmt(dataId, connectionPtr, sql, stmt);
}

OBJ_GETTER(SQLiteSupport_getStmt, KInt dataId, KString sql) {
    RETURN_OBJ(dataState()->getStmt(dataId, sql));
}

KBoolean SQLiteSupport_hasStmt(KInt dataId, KString sql) {
    return dataState()->getStmt(dataId, sql) != nullptr;
}

void SQLiteSupport_evictAll(KInt dataId, KLong connectionPtr) {
    return dataState()->evictAll(dataId, connectionPtr);
}

void SQLiteSupport_remove(KInt dataId, KLong connectionPtr, KString sql) {
    return dataState()->remove(dataId, connectionPtr, sql);
}

KInt SQLiteSupport_nextDataId(){
    return dataState()->nextDataId();
}
void SQLiteSupport_createDataStore(KInt dataId, KInt maxCacheSize) {
    return dataState()->createDataStore(dataId, maxCacheSize);
}

void SQLiteSupport_removeDataStore(KInt dataId) {
    return dataState()->removeDataStore(dataId);
}

void SQLiteSupport_putHelperInfo(KInt dataId, KRef helperInfo) {
    dataState()->putHelperInfo(dataId, helperInfo);
}

KRef SQLiteSupport_getHelperInfo(KInt dataId) {
    return dataState()->getHelperInfo(dataId);
}

KInt SQLiteSupport_nextHelperInfoId() {
    return dataState()->nextHelperInfoId();
}

}