//
// Created by Kevin Galligan on 6/20/18.
//
#include <pthread.h>
#include "Types.h"

namespace {
    //We're copy/pasting this a lot
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

    class ThreadState{
    public:
        ThreadState() {
            pthread_mutex_init(&lock_, nullptr);
        }

        ~ThreadState() {
            pthread_mutex_destroy(&lock_);
        }

        KInt nextThreadLocalId() {
            Locker locker(&lock_);
            return threadLocalId_++;
        }
    private:
        pthread_mutex_t lock_;
        KInt threadLocalId_;
    };

    ThreadState *dataState() {
        static ThreadState *state = nullptr;

        if (state != nullptr) {
            return state;
        }

        ThreadState *result = konanConstructInstance<ThreadState>();

        ThreadState *old = __sync_val_compare_and_swap(&state, nullptr, result);

        if (old != nullptr) {
            konanDestructInstance(result);
            // Someone else inited this data.
            return old;
        }

        return state;
    }
}

extern "C"{
    KInt ThreadSupport_nextThreadLocalId(){
        return dataState()->nextThreadLocalId();
    }
}