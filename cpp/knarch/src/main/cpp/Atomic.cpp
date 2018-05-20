//
// Created by Kevin Galligan on 5/20/18.
//
#include "Memory.h"
#include "Types.h"


extern "C" {

OBJ_GETTER0(makeAtomicCounter);

OBJ_GETTER(Atomic_atomicGetCounter, ObjHeader *referred) {
    MetaObjHeader *meta = referred->meta_object();

    if (meta->counter_ == nullptr) {
        ObjHolder counterHolder;
// Cast unneeded, just to emphasize we store an object reference as void*.
        ObjHeader *counter = makeAtomicCounter(counterHolder.slot());
        UpdateRefIfNull(&meta->counter_, counter);
    }

    RETURN_OBJ(meta->counter_);
}

}