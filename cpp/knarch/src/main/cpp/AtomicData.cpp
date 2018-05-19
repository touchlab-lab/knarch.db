/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <stdlib.h>

#include <pthread.h>

#include "Alloc.h"
#include "Memory.h"
#include "Runtime.h"
#include "Types.h"
#include "AtomicData.h"
#include "KString.h"

extern "C" {

RUNTIME_NORETURN void ThrowAtomicDataInvalidState();

OBJ_GETTER(AtomicDataLaunchpad, KRef);

}

namespace {
    enum {
        CHECKED = 0,
        UNCHECKED = 1
    };

    KNativePtr dispatchtransfer(KRef object, KInt mode) {
        switch (mode) {
            case CHECKED:
            case UNCHECKED:
                if (!ClearSubgraphReferences(object, mode == CHECKED)) {
                    // Release reference to the object, as it is not being managed by ObjHolder.
                    UpdateRef(&object, nullptr);
                    ThrowAtomicDataInvalidState();
                    return nullptr;
                }
                return object;
        }
        return nullptr;
    }

    //TODO: Delete if returned but closing
    class DataStore {
    public:
        DataStore(KNativePtr ptr) : ptr_(ptr) {
            pthread_mutex_init(&lock_, nullptr);
        }

        OBJ_GETTER0(accessData) {
            auto result = AdoptStablePointer(ptr_, OBJ_RESULT);
            ptr_ = nullptr;
            return result;
        }

        void returnData(KRef data) {
            ptr_ = dispatchtransfer(data, CHECKED);
        }

        void lockAccess() {
            pthread_mutex_lock(&lock_);
        }

        void unlockAccess() {
            pthread_mutex_unlock(&lock_);
        }

    private:
        pthread_mutex_t lock_;
        KNativePtr ptr_;
    };

    class AtomicDataState {
    public:
        AtomicDataState() {
            pthread_mutex_init(&lock_, nullptr);
        }

        KInt nextDataId() { return currentDataId_++; }

        KInt createDataStore(KNativePtr jobArgument) {

            KInt dataId = 0;
            DataStore *dataStore = nullptr;
            {
                pthread_mutex_lock(&lock_);
                KInt dataId = nextDataId();
                dataStore = konanConstructInstance<DataStore>(jobArgument);
                data_[dataId] = dataStore;
                pthread_mutex_unlock(&lock_);
            }

            return dataId;
        }

        OBJ_GETTER(openAccess, KInt id) {
            pthread_mutex_lock(&lock_);
            auto it = data_.find(id);
            pthread_mutex_unlock(&lock_);

            if (it == data_.end())
                return nullptr;

//            DataStore* dataStore= it.second;
//            dataStore->lockAccess();
//            dataStore->accessData(OBJ_RESULT);
        }

        void closeAccess(KInt id, KRef ref) {
            pthread_mutex_lock(&lock_);
//            DataStore *dataStore = data_.find(id).second;
//            pthread_mutex_unlock(&lock_);
//
//            dataStore->returnData(ref);
//            dataStore->unlockAccess();
        }

        void removeDataStore(KInt id) {
            //Null out reference when locked. Will prevent anybody else from getting and waiting.
            pthread_mutex_lock(&lock_);
//            DataStore *dataStore = data_.find(id).second;
//            data_.erase(id);
//            pthread_mutex_unlock(&lock_);
//
//            //Make sure nobody's currently looking at it
//            dataStore->lockAccess();
//            dataStore->unlockAccess();
//            konanDestructInstance(dataStore);
        }

        ~AtomicDataState() {
            pthread_mutex_destroy(&lock_);
        }


    private:
        pthread_mutex_t lock_;
        KStdUnorderedMap<KInt, DataStore *> data_;
        KInt currentDataId_;
    };

    AtomicDataState *dataState() {
        static AtomicDataState *state = nullptr;

        if (state != nullptr) {
            return state;
        }

        AtomicDataState *result = konanConstructInstance<AtomicDataState>();

        AtomicDataState *old = __sync_val_compare_and_swap(&state, nullptr, result);

        if (old != nullptr) {
            konanDestructInstance(result);
            // Someone else inited this data.
            return old;
        }

        return state;
    }
}

extern "C" {
KInt Co_Touchlab_Kite_Threads_AtomicData_init(KRef producer) {
    KRef jobArgumentRef = nullptr;
    AtomicDataLaunchpad(producer, &jobArgumentRef);
    KNativePtr jobArgument = dispatchtransfer(jobArgumentRef, CHECKED);
    return dataState()->createDataStore(jobArgument);
}

OBJ_GETTER(Co_Touchlab_Kite_Threads_AtomicData_openAccess, KInt id) {
    dataState()->openAccess(id, OBJ_RESULT);
}

void Co_Touchlab_Kite_Threads_AtomicData_closeAccess(KInt id, KRef ref){
    dataState()->closeAccess(id, ref);
}

void Co_Touchlab_Kite_Threads_AtomicData_removeDataStore(KInt id){
    dataState()->removeDataStore(id);
}

KString Co_Touchlab_Kite_Threads_AtomicData_testMakeString(KInt anum){
    char exceptionMessage[150];
    snprintf(exceptionMessage, sizeof(exceptionMessage), "Your num %d", anum);
//    KString formattedMessage = nullptr;
//    KRef motherfucker = (KRef)formattedMessage;
ObjHolder hold;

    ObjHeader* res = CreateStringFromCString(const_cast<const char*>(exceptionMessage), hold.slot());
    UpdateRef(hold.slot(), res);
    return reinterpret_cast<KString>(res);
}
OBJ_GETTER(Co_Touchlab_Kite_Threads_AtomicData_testMakeStringWrapped, KInt anum){
    char exceptionMessage[150];
    snprintf(exceptionMessage, sizeof(exceptionMessage), "Your num %d", anum);
//    KString formattedMessage = nullptr;
//    KRef motherfucker = (KRef)formattedMessage;
//    CreateStringFromCString(const_cast<const char*>(exceptionMessage), &motherfucker);

    RETURN_RESULT_OF(CreateStringFromCString, exceptionMessage);
}



}