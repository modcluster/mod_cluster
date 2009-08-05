/*
 *  SIGHT - System information gathering hybrid tool
 *
 *  Copyright(c) 2007 Red Hat Middleware, LLC,
 *  and individual contributors as indicated by the @authors tag.
 *  See the copyright.txt in the distribution for a
 *  full listing of individual contributors.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library in the file COPYING.LIB;
 *  if not, write to the Free Software Foundation, Inc.,
 *  59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
 *
 * @author Mladen Turk
 *
 */

#include "sight.h"
#include "sight_local.h"
#include "apr.h"
#include "apr_version.h"
#include "apr_atomic.h"
#include "sight_version.h"

apr_pool_t      *sight_master_pool = NULL;
apr_pool_t      *sight_global_pool = NULL;
apr_pool_t      *sight_temp_pool   = NULL;
static JavaVM   *sight_global_vm   = NULL;
static apr_thread_mutex_t *sight_global_mutex = NULL;
static apr_uint64_t sight_counter_ii = 0;

static volatile apr_uint32_t sight_global_a = 0;
volatile apr_uint32_t *sight_global_atomic = &sight_global_a;


volatile apr_uint64_t sight_cnt_native_alloc        = 0;
volatile apr_uint64_t sight_cnt_native_create       = 0;
volatile apr_uint64_t sight_cnt_native_free         = 0;
volatile apr_uint64_t sight_cnt_native_destroyed    = 0;
volatile apr_uint64_t sight_cnt_native_pcleanup     = 0;
volatile apr_uint64_t sight_cnt_native_clear        = 0;
volatile apr_uint64_t sight_cnt_native_cleared      = 0;
volatile apr_uint64_t sight_cnt_native_clrcall      = 0;

static volatile apr_uint64_t sight_cnt_jvm_attached = 0;
static volatile apr_uint64_t sight_cnt_jvm_detached = 0;
static volatile apr_uint64_t sight_cnt_jvm_getenv   = 0;



#define STD_GLOBAL_JCLASSES SIGHT_CC_MAX
static jclass global_classes[STD_GLOBAL_JCLASSES];

static void init_java_rt(JNIEnv *e)
{
    jobject c;
    int i;
    for (i = 0; i < STD_GLOBAL_JCLASSES; i++)
        global_classes[i] = NULL;

    c = (jobject)(*e)->FindClass(e, "Ljava/lang/Object;");
    global_classes[SIGHT_CC_OBJECT] = (jclass)(*e)->NewGlobalRef(e, c);
    (*e)->DeleteLocalRef(e, c);
    c = (jobject)(*e)->FindClass(e, "Ljava/lang/String;");
    global_classes[SIGHT_CC_STRING] = (jclass)(*e)->NewGlobalRef(e, c);
    (*e)->DeleteLocalRef(e, c);
    /* [][] Core Arrays */
    c = (jobject)(*e)->FindClass(e, "[Z");
    global_classes[SIGHT_CC_ZARRAY] = (jclass)(*e)->NewGlobalRef(e, c);
    (*e)->DeleteLocalRef(e, c);
    c = (jobject)(*e)->FindClass(e, "[B");
    global_classes[SIGHT_CC_BARRAY] = (jclass)(*e)->NewGlobalRef(e, c);
    (*e)->DeleteLocalRef(e, c);
    c = (jobject)(*e)->FindClass(e, "[C");
    global_classes[SIGHT_CC_CARRAY] = (jclass)(*e)->NewGlobalRef(e, c);
    (*e)->DeleteLocalRef(e, c);
    c = (jobject)(*e)->FindClass(e, "[I");
    global_classes[SIGHT_CC_IARRAY] = (jclass)(*e)->NewGlobalRef(e, c);
    (*e)->DeleteLocalRef(e, c);
    c = (jobject)(*e)->FindClass(e, "[J");
    global_classes[SIGHT_CC_JARRAY] = (jclass)(*e)->NewGlobalRef(e, c);
    (*e)->DeleteLocalRef(e, c);
    c = (jobject)(*e)->FindClass(e, "[Ljava/lang/String;");
    global_classes[SIGHT_CC_TARRAY] = (jclass)(*e)->NewGlobalRef(e, c);
    (*e)->DeleteLocalRef(e, c);
    c = (jobject)(*e)->FindClass(e, "[Ljava/lang/Object;");
    global_classes[SIGHT_CC_OARRAY] = (jclass)(*e)->NewGlobalRef(e, c);
    (*e)->DeleteLocalRef(e, c);

}

jclass sight_get_cc(sight_cclass_e i)
{
    if (i > 0 && i < SIGHT_CC_MAX)
        return global_classes[i];
    else
        return NULL;
}

jobjectArray sight_new_cc_array(JNIEnv *e, sight_cclass_e i, jsize len)
{
    if (i > 0 && i < SIGHT_CC_MAX)
        return (*e)->NewObjectArray(e, len, global_classes[i], NULL);
    else
        return NULL;
}

jbyteArray sight_new_byte_array(JNIEnv *e, jbyte *data,
                                jsize len)
{
    jbyteArray bytes = (*e)->NewByteArray(e, len);
    if (bytes != NULL) {
        (*e)->SetByteArrayRegion(e, bytes, 0, (jint)len, data);
    }
    return bytes;
}

/* Called by the JVM when APR_JAVA is loaded */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
    JNIEnv *_E;

    UNREFERENCED(reserved);
    if ((*vm)->GetEnv(vm, (void **)&_E, JNI_VERSION_1_4)) {
        return JNI_ERR;
    }
    sight_global_vm  = vm;
    sight_counter_ii = (apr_uint64_t)time(NULL);
    /* Initialize all counters to zero */
    return  JNI_VERSION_1_4;
}

/* Called by the JVM before the APR_JAVA is unloaded */
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved)
{
    JNIEnv *env;

    UNREFERENCED(reserved);

    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_2)) {
        return;
    }
    if (sight_global_pool) {
        apr_terminate();
        sight_unload_classes(env);
    }
}

SIGHT_EXPORT_DECLARE(jboolean, Library, initialize0)(SIGHT_STDARGS)
{

    UNREFERENCED_STDARGS;
    /* TODO Add reference count */
    if (!sight_global_pool) {
        sight_global_a = 1;

        apr_initialize();
        init_java_rt(_E);
        if (sight_load_classes(_E)) {
            apr_terminate();
            sight_unload_classes(_E);
            return JNI_FALSE;
        }
        if (apr_pool_create(&sight_master_pool, NULL) != APR_SUCCESS) {
            return JNI_FALSE;
        }
        if (apr_thread_mutex_create(&sight_global_mutex,
                                    APR_THREAD_MUTEX_NESTED,
                                    sight_master_pool) != APR_SUCCESS) {
            return JNI_FALSE;
        }

        if (sight_pool_create(&sight_global_pool, NULL, NULL, 1) != APR_SUCCESS) {
            return JNI_FALSE;
        }
        if (sight_pool_create(&sight_temp_pool, NULL, NULL, 1) != APR_SUCCESS) {
            return JNI_FALSE;
        }

        apr_atomic_init(sight_global_pool);
        if (sight_main(sight_global_pool) != APR_SUCCESS) {
            apr_pool_destroy(sight_temp_pool);
            apr_pool_destroy(sight_global_pool);
            sight_global_pool = NULL;
            sight_unload_classes(_E);
            return JNI_FALSE;
        }
    }
    return JNI_TRUE;
}

apr_status_t sight_pool_create(apr_pool_t **pool, apr_thread_mutex_t **lock,
                               apr_pool_t *parent, int lockable)
{
    apr_status_t rc;
    apr_allocator_t *allocator = NULL;
    apr_thread_mutex_t *mutex  = NULL;

    if ((rc = apr_allocator_create(&allocator)) != APR_SUCCESS)
        return rc;

    if ((rc = apr_pool_create_ex(pool, parent, NULL,
                                 allocator)) != APR_SUCCESS) {
        apr_allocator_destroy(allocator);
        return rc;
    }
    if (lockable) {
        if ((rc = apr_thread_mutex_create(&mutex,
                                          APR_THREAD_MUTEX_NESTED,
                                          sight_master_pool)) != APR_SUCCESS) {
            apr_allocator_destroy(allocator);
            *pool = NULL;
            return rc;
        }
        if (lock)
            *lock = mutex;
    }
    else if (parent) {
        /* Use parents allocator mutex */
        mutex = apr_allocator_mutex_get(apr_pool_allocator_get(parent));
    }
    apr_allocator_mutex_set(allocator, mutex);
    apr_allocator_owner_set(allocator, *pool);
    return APR_SUCCESS;
}

apr_status_t sight_pool_clear(apr_pool_t *pool)
{
    apr_allocator_t *allocator = apr_pool_allocator_get(pool);
    apr_thread_mutex_t *mutex  = apr_allocator_mutex_get(allocator);

    if (mutex)
        apr_thread_mutex_lock(mutex);
    apr_pool_clear(pool);
    if (mutex)
        apr_thread_mutex_unlock(mutex);
    return APR_SUCCESS;
}

apr_status_t sight_pool_destroy(apr_pool_t *pool)
{
    apr_allocator_t *allocator = apr_pool_allocator_get(pool);
    apr_thread_mutex_t *mutex  = apr_allocator_mutex_get(allocator);

    if (mutex)
        apr_thread_mutex_lock(mutex);
    apr_pool_destroy(pool);
    if (mutex)
        apr_thread_mutex_unlock(mutex);
    return APR_SUCCESS;
}

apr_status_t sight_pool_lock(apr_thread_mutex_t **mutex, apr_pool_t *pool)
{
    if (pool) {
        apr_allocator_t *allocator = apr_pool_allocator_get(pool);
        apr_thread_mutex_t *lock;

        if ((lock = apr_allocator_mutex_get(allocator))) {
            if (mutex)
                *mutex = lock;
            return apr_thread_mutex_lock(lock);
        }
    }
    if (mutex)
        *mutex = NULL;
    return APR_ENOPOOL;
}

apr_status_t sight_pool_lock_parent(apr_thread_mutex_t **mutex, apr_pool_t *pool)
{
    if (pool) {
        apr_pool_t *parent         = apr_pool_parent_get(pool);
        apr_allocator_t *allocator = apr_pool_allocator_get(parent);

        if ((*mutex = apr_allocator_mutex_get(allocator)) != NULL) {
            return apr_thread_mutex_lock(*mutex);
        }
    }
    *mutex = NULL;
    return APR_SUCCESS;
}

static void dump_stats()
{
    fprintf(stderr, "\nNativeObject statistics ......\n");
    fprintf(stderr, "Allocated               : %" APR_INT64_T_FMT "\n", sight_cnt_native_alloc);
    fprintf(stderr, "Initialized             : %" APR_INT64_T_FMT "\n", sight_cnt_native_create);
    fprintf(stderr, "Garbage collected       : %" APR_INT64_T_FMT "\n", sight_cnt_native_free);
    fprintf(stderr, "Garbage destroyed       : %" APR_INT64_T_FMT "\n", sight_cnt_native_destroyed);
    fprintf(stderr, "Pool cleanup destroyed  : %" APR_INT64_T_FMT "\n", sight_cnt_native_pcleanup);
    fprintf(stderr, "Clear called            : %" APR_INT64_T_FMT "\n", sight_cnt_native_clear);
    fprintf(stderr, "Cleared                 : %" APR_INT64_T_FMT "\n", sight_cnt_native_cleared);
    fprintf(stderr, "Clean callbacks         : %" APR_INT64_T_FMT "\n", sight_cnt_native_clrcall);

    fprintf(stderr, "\nJVM statistics ...............\n");
    fprintf(stderr, "GetEnv calls            : %" APR_INT64_T_FMT "\n", sight_cnt_jvm_getenv);
    fprintf(stderr, "Attached Threads        : %" APR_INT64_T_FMT "\n", sight_cnt_jvm_attached);
    fprintf(stderr, "Detached Threads        : %" APR_INT64_T_FMT "\n", sight_cnt_jvm_detached);

    fflush(stderr);
}

#define OPTIMISTIC_LOCK_CNT     10000
#define OPTIMISTIC_LOCK_MAX     OPTIMISTIC_LOCK_CNT + 60000

SIGHT_EXPORT_DECLARE(void, Library, terminate0)(SIGHT_STDARGS)
{

    UNREFERENCED_STDARGS;

    if (sight_global_pool) {
        apr_thread_mutex_t *glock;
        apr_thread_mutex_t *tlock;
#if SIGHT_APR_REFCOUNT
        apr_uint32_t ioc = 0;
        apr_uint32_t in_object_count = apr_atomic_dec32(sight_global_atomic);
#endif
#ifdef SIGHT_DO_STATS
        fprintf(stderr, "\nLibrary terminate ............\n");
        fprintf(stderr, "Global Atomic counter   : %d\n", sight_global_a);
#endif
        sight_pool_lock(&tlock, sight_temp_pool);
        sight_pool_lock(&glock, sight_global_pool);

#if SIGHT_APR_REFCOUNT
        /* Wait until all JNI calls are done
         */
        while (in_object_count) {
            apr_thread_yield();
            in_object_count = apr_atomic_read32(sight_global_atomic);
            if (ioc++ > OPTIMISTIC_LOCK_CNT) {
                /* Sleep one millisecond  */
                apr_sleep(1000L);
            }
            if (ioc > OPTIMISTIC_LOCK_MAX) {
                /* TODO: We have a zombie or a lengthy JNI op.
                 * Find a way to bail out without crushing JVM
                 */
#ifdef SIGHT_DO_STATS
                fprintf(stderr, "Library.terminate() call is locked ...\n");
                fprintf(stderr, "Bailing out !\n");
                fflush(stderr);
                exit(-1);
#endif
            }
        }
#endif
        apr_pool_destroy(sight_temp_pool);
        sight_temp_pool   = NULL;
        apr_pool_destroy(sight_global_pool);
        sight_global_pool = NULL;
        apr_thread_mutex_unlock(tlock);
        apr_thread_mutex_unlock(glock);
        apr_sleep(1000L);
        apr_pool_destroy(sight_master_pool);
        apr_terminate();
        sight_unload_classes(_E);
    }
#ifdef SIGHT_DO_STATS
    dump_stats();
#endif
}

SIGHT_EXPORT_DECLARE(void, Library, clear0)(SIGHT_STDARGS)
{

    UNREFERENCED_O;

    if (sight_global_pool) {
#if SIGHT_APR_REFCOUNT
        apr_uint32_t ioc = 0;
        apr_uint32_t in_object_count = apr_atomic_dec32(sight_global_atomic);
#endif
        /* Wait untill all object native calls are finished */
#ifdef SIGHT_DO_STATS
        fprintf(stderr, "\nLibrary clear ................\n");
        fprintf(stderr, "Global Atomic counter   : %d\n", sight_global_a);
#endif

#if SIGHT_APR_REFCOUNT

        while (in_object_count) {
            apr_thread_yield();
            in_object_count = apr_atomic_read32(sight_global_atomic);
        }
        if (ioc++ > OPTIMISTIC_LOCK_CNT) {
            /* Sleep one millisecond  */
            apr_sleep(1000L);
        }
        if (ioc > OPTIMISTIC_LOCK_MAX) {
            /* TODO: We have a zombie or a lengthy JNI op.
             * Find a way to bail out without crushing JVM
             */
#ifdef SIGHT_DO_STATS
            fprintf(stderr, "Library.clear0() call is locked ...\n");
            fprintf(stderr, "Bailing out !\n");
            fflush(stderr);
#endif
            throwAprException(_E, APR_TIMEUP);
            return;
        }

#endif

        sight_pool_clear(sight_temp_pool);
        sight_pool_clear(sight_global_pool);
        /* Sleep one second */
        apr_sleep(1000L);
        apr_atomic_set32(sight_global_atomic, 1);
    }
#ifdef SIGHT_DO_STATS
    dump_stats();
#endif
}

SIGHT_EXPORT_DECLARE(jint, Library, version)(SIGHT_STDARGS, jint what)
{
    apr_version_t apv;

    UNREFERENCED_STDARGS;
    apr_version(&apv);

    switch (what) {
        case 0x01:
            return SIGHT_MAJOR_VERSION;
        break;
        case 0x02:
            return SIGHT_MINOR_VERSION;
        break;
        case 0x03:
            return SIGHT_PATCH_VERSION;
        break;
        case 0x04:
#ifdef SIGHT_IS_DEV_VERSION
            return 1;
#else
            return 0;
#endif
        break;
        case 0x11:
            return apv.major;
        break;
        case 0x12:
            return apv.minor;
        break;
        case 0x13:
            return apv.patch;
        break;
        case 0x14:
            return apv.is_dev;
        break;
    }
    return 0;
}

SIGHT_EXPORT_DECLARE(jstring, Library, getVersionString)(SIGHT_STDARGS)
{
    UNREFERENCED_O;
    return CSTR_TO_JSTRING(SIGHT_VERSION_STRING);
}

SIGHT_EXPORT_DECLARE(jstring, Library, getAprVersionString)(SIGHT_STDARGS)
{
    UNREFERENCED_O;
    return CSTR_TO_JSTRING(apr_version_string());
}


/* Get current JNIEnv
 * If this is a thread not created by JVM attach the thread
 */
jint sight_get_jnienv(JNIEnv **env)
{
    jint rv;

#ifdef SIGHT_DO_STATS
    sight_cnt_jvm_getenv++;
#endif

    rv = (*sight_global_vm)->GetEnv(sight_global_vm,
                                    (void**)env,
                                    JNI_VERSION_1_4);
    if (rv == JNI_EDETACHED) {
        (*sight_global_vm)->AttachCurrentThread(sight_global_vm,
                                                (void**)env,
                                                NULL);
#ifdef SIGHT_DO_STATS
        sight_cnt_jvm_attached++;
#endif
    }
    return rv;
}

/* If the thread was attached detach the current thread.
 * Allways call this function with return value from the
 * sight_get_jnienv
 */
void sight_clr_jnienv(jint mode)
{
    if (mode == JNI_EDETACHED) {
        (*sight_global_vm)->DetachCurrentThread(sight_global_vm);
#ifdef SIGHT_DO_STATS
        sight_cnt_jvm_detached++;
#endif
    }
}

apr_uint64_t sight_atomic_inc64()
{
    apr_uint64_t rv;
    apr_thread_mutex_lock(sight_global_mutex);
    rv = sight_counter_ii++;
    apr_thread_mutex_unlock(sight_global_mutex);
    return rv;
}

static apr_uint64_t sight_counter_lo = 1;
static apr_uint64_t sight_counter_hi = 0;

void sight_atomic_inc128(apr_byte_t *buf)
{
    apr_uint64_t a, b;
    apr_thread_mutex_lock(sight_global_mutex);
    a = sight_counter_lo++;
    if (a == 0)
        b = ++sight_counter_hi;
    else
        b = sight_counter_hi;
    apr_thread_mutex_unlock(sight_global_mutex);

    *buf++ = (apr_byte_t)(a >> 56);
    *buf++ = (apr_byte_t)(a >> 48);
    *buf++ = (apr_byte_t)(a >> 40);
    *buf++ = (apr_byte_t)(a >> 32);
    *buf++ = (apr_byte_t)(a >> 24);
    *buf++ = (apr_byte_t)(a >> 16);
    *buf++ = (apr_byte_t)(a >>  8);
    *buf++ = (apr_byte_t)(a >>  0);

    *buf++ = (apr_byte_t)(b >> 56);
    *buf++ = (apr_byte_t)(b >> 48);
    *buf++ = (apr_byte_t)(b >> 40);
    *buf++ = (apr_byte_t)(b >> 32);
    *buf++ = (apr_byte_t)(b >> 24);
    *buf++ = (apr_byte_t)(b >> 16);
    *buf++ = (apr_byte_t)(b >>  8);
    *buf   = (apr_byte_t)(b >>  0);
}
