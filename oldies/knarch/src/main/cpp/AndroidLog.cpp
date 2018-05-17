

#include "AndroidLog.h"

int __android_log_write(int prio, const char *tag, const char *text){
    return 0;
}

int __android_log_print(int prio, const char *tag,  const char *fmt, ...){
    return 0;
}

int __android_log_vprint(int prio, const char *tag,
                         const char *fmt, va_list ap){
     return 0;
}

void __android_log_assert(const char *cond, const char *tag,
                          const char *fmt, ...){

}