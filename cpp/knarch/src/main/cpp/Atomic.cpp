//
// Created by Kevin Galligan on 5/20/18.
//

#include <pthread.h>

#include "Memory.h"
#include "Types.h"

namespace {
    class AtomicState {
    public:
        AtomicState() {
            pthread_mutex_init(&lock_, nullptr);
        }

        KInt nextDataId() { return currentDataId_++; }

        KInt createDataStore() {
            KInt dataId = 0;

            pthread_mutex_lock(&lock_);
            dataId = nextDataId();
            data_[dataId] = nullptr;
            pthread_mutex_unlock(&lock_);

            return dataId;
        }

        void putDataPointer(KInt dataId, KNativePtr atomicPointer) {
            pthread_mutex_lock(&lock_);
            data_[dataId] = atomicPointer;
            pthread_mutex_unlock(&lock_);
        }

        KNativePtr getDataPointer(KInt dataId) {
            pthread_mutex_lock(&lock_);

            auto it = data_.find(dataId);

            pthread_mutex_unlock(&lock_);

            if (it == data_.end())
                return nullptr;
            else
                return it->second;
        }

        void removeDataPointer(KInt id) {
            pthread_mutex_lock(&lock_);
            auto it = data_.find(id);
            if (it == data_.end()) return;
            data_.erase(it);
            pthread_mutex_unlock(&lock_);
        }

    private:
        pthread_mutex_t lock_;
        KStdUnorderedMap<KInt, KNativePtr> data_;
        KInt currentDataId_;
    };

    AtomicState *dataState() {
        static AtomicState *state = nullptr;

        if (state != nullptr) {
            return state;
        }

        AtomicState *result = konanConstructInstance<AtomicState>();

        AtomicState *old = __sync_val_compare_and_swap(&state, nullptr, result);

        if (old != nullptr) {
            konanDestructInstance(result);
            // Someone else inited this data.
            return old;
        }

        return state;
    }
}

extern "C" {

KInt Atomic_createDataStore() {
    return dataState()->createDataStore();
}

void Atomic_putDataPointer(KInt dataId, KNativePtr atomicPointer) {
    dataState()->putDataPointer(dataId, atomicPointer);
}

KNativePtr Atomic_getDataPointer(KInt dataId) {
    return dataState()->getDataPointer(dataId);
}

void Atomic_removeDataPointer(KInt id) {
    dataState()->removeDataPointer(id);
}

OBJ_GETTER(makeAtomicCounter, KRef target);

OBJ_GETTER(Atomic_atomicGetCounter, ObjHeader *referred) {
    MetaObjHeader *meta = referred->meta_object();

    if (meta->counter_ == nullptr) {
        ObjHolder counterHolder;

// Cast unneeded, just to emphasize we store an object reference as void*.
        ObjHeader *counter = makeAtomicCounter(referred, counterHolder.slot());
        UpdateRef(&meta->counter_, counter);
    }

    RETURN_OBJ(meta->counter_);
}

}