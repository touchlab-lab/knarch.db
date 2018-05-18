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

#include "Exceptions.h"
#include "Memory.h"

#include "Runtime.h"

#include "Porting.h"
#include "Types.h"

#include <dispatch/dispatch.h>

extern "C" {
RUNTIME_NORETURN void ThrowDispatchInvalidState();
void mainCallbackReturn(KInt callbackId);
void ReportUnhandledException(KRef e);
OBJ_GETTER(DispatchLaunchpad, KRef);
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
                    ThrowDispatchInvalidState();
                    return nullptr;
                }
                return object;
        }
        return nullptr;
    }

    struct Gig {
        KRef (*function)(KRef, ObjHeader **);

        KNativePtr argument;
        KInt transferMode;
    };

    class ResultState {
    public:
        ResultState() {
            pthread_mutex_init(&lock_, nullptr);
        }

        KInt nextResultId() { return currentResultId_++; }

        ~ResultState() {
            pthread_mutex_destroy(&lock_);
        }

        void storeResult(KInt id, KNativePtr result);

        KNativePtr pullResult(KInt id);

        OBJ_GETTER(pullResult, KInt id) {
            pthread_mutex_lock(&lock_);
            auto it = results_.find(id);
            if (it != results_.end()) {
                results_.erase(it);
            }

            auto result = AdoptStablePointer(it->second, OBJ_RESULT);

            pthread_mutex_unlock(&lock_);

            return result;
        }

    private:
        pthread_mutex_t lock_;
        KStdUnorderedMap<KInt, KNativePtr> results_;
        KInt currentResultId_;
    };

    ResultState *resultState() {
        static ResultState *state = nullptr;

        if (state != nullptr) {
            return state;
        }

        ResultState *result = konanConstructInstance<ResultState>();

        ResultState *old = __sync_val_compare_and_swap(&state, nullptr, result);

        if (old != nullptr) {
            konanDestructInstance(result);
            // Someone else inited this data.
            return old;
        }

        return state;
    }

    void ResultState::storeResult(KInt id, KNativePtr result) {
        pthread_mutex_lock(&lock_);
        results_[id] = result;
        pthread_mutex_unlock(&lock_);
    }

    void nativeDispatchInternal(KInt transferMode, KRef producer, KNativePtr jobFunction, KInt callbackId) {
        // Note that this is a bit hacky, as we must not auto-release jobArgumentRef,
        // so we don't use ObjHolder.
        KRef jobArgumentRef = nullptr;
        DispatchLaunchpad(producer, &jobArgumentRef);
        KNativePtr jobArgument = dispatchtransfer(jobArgumentRef, transferMode);

        Gig gig;
        gig.function = reinterpret_cast<KRef (*)(KRef, ObjHeader **)>(jobFunction);
        gig.argument = jobArgument;
        gig.transferMode = transferMode;
        ObjHolder argumentHolder;
        KRef argument = AdoptStablePointer(gig.argument, argumentHolder.slot());
        // Copied from Worker.cpp
        // Note that this is a bit hacky, as we must not auto-release resultRef,
        // so we don't use ObjHolder.
        // It is so, as ownership is transferred.
        // TODO: Figure out how to consume Unit result. Probably OK as is.
        KRef resultRef = nullptr;
        KNativePtr result = nullptr;

        try {
            gig.function(argument, &resultRef);
            // Transfer the result.
            result = dispatchtransfer(resultRef, gig.transferMode);
        } catch (ObjHolder &e) {
            ReportUnhandledException(e.obj());
        }

        resultState()->storeResult(callbackId, result);

        dispatch_async(dispatch_get_main_queue(), ^{
            mainCallbackReturn(callbackId);
        });
    }
}

extern "C" {

void Co_Touchlab_Kite_Threads_nativeDispatch(KInt transferMode, KRef producer, KNativePtr job, KInt callbackId) {
    nativeDispatchInternal(transferMode, producer, job, callbackId);
}

OBJ_GETTER(Co_Touchlab_Kite_Threads_pullResult, KInt id) {
    RETURN_RESULT_OF(resultState()->pullResult, id);
}

} // extern "C"
