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

extern apr_pool_t *sight_global_pool;

#define OPTIMISTIC_LOCK_CNT     10000
#define OPTIMISTIC_LOCK_MAX     OPTIMISTIC_LOCK_CNT + 60000

/*
 * NativeObject
 * This is the core of object management in SIGHT.
 */

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "NativeObject"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "INSTANCE",
    "J"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "POOL",
    "J"
};

SIGHT_CLASS_LDEF(NativeObject)
{
    if (sight_load_class(_E, &_clazzn))
        return 1;

    J_LOAD_IFIELD(0000);
    J_LOAD_IFIELD(0001);
    return 0;
}

SIGHT_CLASS_UDEF(NativeObject)
{
    sight_unload_class(_E, &_clazzn);
}

static apr_status_t native_object_cleanup(void *data)
{
    apr_status_t rv = APR_SUCCESS;
    sight_object_t *no = (sight_object_t *)data;

#ifdef SIGHT_DO_STATS
    sight_cnt_native_pcleanup++;
#endif
    if (data) {
#if SIGHT_APR_REFCOUNT
        apr_uint32_t ioc = 0;
        apr_uint32_t refcount;
#endif
        JNIEnv *_E = NULL;
        jint em = sight_get_jnienv(&_E);

        apr_atomic_inc32(&no->interrupted);
        /* Mark the pool as invaild */
        no->pool = NULL;
        if (_E && no->object) {
            jobject object = (*_E)->NewLocalRef(_E, no->object);
            (*_E)->DeleteWeakGlobalRef(_E, no->object);
            no->object = object;
             if (no->object) {
                if (no->destroy) {
                    (*_E)->CallVoidMethod(_E, no->object, no->destroy, NULL);
                    if ((*_E)->ExceptionCheck(_E)) {
                        /* Just clear any pending exceptions */
                        (*_E)->ExceptionClear(_E);
                        DBPRINTF("NativeObject_cleanup: exception calling destroy %p",
                                 no->object);
                    }
                }
                SET_IFIELD_J(0001, no->object, 0);
                (*_E)->DeleteLocalRef(_E, no->object);
                no->object = NULL;
            }
        }
#if SIGHT_APR_REFCOUNT
        refcount = apr_atomic_read32(&no->refcount);
        while (refcount) {
            apr_thread_yield();
            refcount = apr_atomic_read32(&no->refcount);
            if (ioc++ > OPTIMISTIC_LOCK_CNT) {
                /* Sleep one millisecond  */
                apr_sleep(1000L);
            }
            if (ioc > OPTIMISTIC_LOCK_MAX) {
                /* TODO: We have a zombie or a lengthy JNI op.
                 * Find a way to bail out without crushing JVM
                 */
                 break;
            }
        }
#endif
        if (no->clean) {
            (*no->clean)(POOL_CALLBACK, no);
            no->clean = NULL;
        }
        if (_E && no->cb.object) {
            (*_E)->DeleteGlobalRef(_E, no->cb.object);
            no->cb.object = NULL;
        }
        sight_clr_jnienv(em);
    }
    return rv;
}

SIGHT_EXPORT_DECLARE(jlong, NativeObject, alloc)(SIGHT_STDARGS)
{
    sight_object_t *no;

    UNREFERENCED_O;
    if (!(no = (sight_object_t *)sight_calloc(_E,
                                    sizeof(sight_object_t),
                                    THROW_FMARK))) {
        return 0;
    }
    no->cb.name = "callback";
    no->cb.msig = "(Ljava/lang/Object;)I";
#ifdef SIGHT_DO_STATS
    sight_cnt_native_alloc++;
#endif
    return P2J(no);
}

SIGHT_EXPORT_DECLARE(void, NativeObject, init0)(SIGHT_STDARGS,
                                                jobject thiz,
                                                jlong instance,
                                                jlong parent,
                                                jboolean lock)
{
    jclass c;
    apr_pool_t *pool = NULL;
    sight_object_t *no = J2P(instance, sight_object_t *);
    apr_pool_t *ppool = J2P(parent, apr_pool_t *);
    apr_status_t rc;
    apr_thread_mutex_t *mutex;

    UNREFERENCED_O;

#ifdef SIGHT_DO_STATS
    sight_cnt_native_create++;
#endif
    if (!no)
        return;
    if (!ppool) {
        /* Use the global pool */
        ppool = sight_global_pool;
    }
    if ((rc = sight_pool_lock(&mutex, ppool)) == APR_SUCCESS) {
        if (!sight_global_pool) {
            /* Global pool is destroyed */
            throwAprMemoryException(_E, THROW_FMARK, rc);
            apr_thread_mutex_unlock(mutex);
        }
        /* TODO: How to detect if parent pool was destroyed?
         */
        if ((rc = sight_pool_create(&pool, &no->mutex, ppool,
                                    lock)) != APR_SUCCESS) {
            throwAprMemoryException(_E, THROW_FMARK, rc);
            apr_thread_mutex_unlock(mutex);
            return;
        }

        c = (*_E)->GetObjectClass(_E, thiz);
        no->destroy = (*_E)->GetMethodID(_E, c, "onDestroy", "()V");
        no->object  = (*_E)->NewWeakGlobalRef(_E, thiz);
        no->pool    = pool;
        apr_pool_cleanup_register(pool, (const void *)no,
                                  native_object_cleanup,
                                  apr_pool_cleanup_null);
        SET_IFIELD_J(0001, no->object, P2J(pool));
        apr_thread_mutex_unlock(mutex);
    } else {
        /* Pool is locked or invalid */
        throwAprMemoryException(_E, THROW_FMARK, rc);
    }
}

SIGHT_EXPORT_DECLARE(void, NativeObject, cbset0)(SIGHT_STDARGS,
                                                 jlong instance,
                                                 jobject callback)
{
    jclass c;
    sight_object_t *no = J2P(instance, sight_object_t *);

    UNREFERENCED_O;

    if (!no)
        return;
    SIGHT_LOCAL_TRY(no) {
        if (no->cb.object) {
            (*_E)->DeleteGlobalRef(_E, no->cb.object);
            no->cb.object = NULL;
            no->cb.method = NULL;
        }
        if (callback) {
            c = (*_E)->GetObjectClass(_E, callback);
            no->cb.object = (*_E)->NewGlobalRef(_E, callback);
            no->cb.method = (*_E)->GetMethodID(_E, c, no->cb.name, no->cb.msig);
            DBPRINTF("NativeObject_cbset0: %s/%s", no->cb.name, no->cb.msig);
        }
    } SIGHT_LOCAL_END(no);
}

SIGHT_EXPORT_DECLARE(void, NativeObject, free0)(SIGHT_STDARGS,
                                                jlong instance)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    jobject object = NULL;
    apr_thread_mutex_t *mutex;
#if SIGHT_APR_REFCOUNT
    apr_uint32_t ioc = 0;
    apr_uint32_t refcount;
#endif

    UNREFERENCED_O;

#ifdef SIGHT_DO_STATS
    sight_cnt_native_free++;
#endif
    if (!no)
        return;
    /* Check if parent is locked */
    if (sight_pool_lock_parent(&mutex, no->pool) == APR_SUCCESS) {
        apr_pool_t *pool = no->pool;
#ifdef SIGHT_DO_STATS
        sight_cnt_native_destroyed++;
#endif
        apr_atomic_inc32(&no->interrupted);
        if (no->mutex) {
            /* Lock only if we are the owner of the mutex */
            apr_thread_mutex_lock(no->mutex);
        }
        no->pool = NULL;
        if (no->object) {
            object = (*_E)->NewLocalRef(_E, no->object);
            (*_E)->DeleteWeakGlobalRef(_E, no->object);
            no->object = object;
        }
#if SIGHT_APR_REFCOUNT
        refcount = apr_atomic_read32(&no->refcount);
        while (refcount) {
            apr_thread_yield();
            refcount = apr_atomic_read32(&no->refcount);
            if (ioc++ > OPTIMISTIC_LOCK_CNT) {
                /* Sleep one millisecond  */
                apr_sleep(1000L);
            }
            if (ioc > OPTIMISTIC_LOCK_MAX) {
                /* TODO: We have a zombie or a lengthy JNI op.
                 * Find a way to bail out without crushing JVM
                 */
#ifdef SIGHT_DO_STATS
                fprintf(stderr, "Native.object() call is locked ...\n");
                fprintf(stderr, "Bailing out !\n");
                fflush(stderr);
                exit(-1);
#endif
            }
        }
#endif
        if (pool)
            apr_pool_cleanup_kill(pool, no, native_object_cleanup);
        if (no->object) {
            (*_E)->CallVoidMethod(_E, no->object, no->destroy, NULL);
            if ((*_E)->ExceptionCheck(_E)) {
                /* Just clear any pending exceptions */
                (*_E)->ExceptionClear(_E);
            }
        }
        if (no->clean)
            (*no->clean)(POOL_DESTROY, no);
        if (pool)
            sight_pool_destroy(pool);
        if (object)
            (*_E)->DeleteLocalRef(_E, object);
        if (no->cb.object) {
            (*_E)->DeleteGlobalRef(_E, no->cb.object);
            no->cb.object = NULL;
        }
        if (no->mutex) {
            apr_thread_mutex_unlock(no->mutex);
            /* XXX: Check if we need some larger timeout here */
            apr_thread_yield();
            apr_thread_mutex_destroy(no->mutex);
        }
        if (mutex)
            apr_thread_mutex_unlock(mutex);
        free(no);
    }
}

SIGHT_EXPORT_DECLARE(void, NativeObject, clear0)(SIGHT_STDARGS,
                                                 jlong instance)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    apr_thread_mutex_t *mutex;
#if SIGHT_APR_REFCOUNT
    apr_uint32_t ioc = 0;
    apr_uint32_t refcount;
#endif
    UNREFERENCED_O;

#ifdef SIGHT_DO_STATS
    sight_cnt_native_clear++;
#endif
    if (!no)
        return;
    /* Lock the parent pool.
     * If the parent pool is inside clear or destroy
     * this call will block until finished.
     * By that time all pool cleanups will be run and
     * the objects pool will be set to zero
     */
    if (sight_pool_lock_parent(&mutex, no->pool) == APR_SUCCESS) {
        apr_pool_t *pool = no->pool;
#ifdef SIGHT_DO_STATS
        sight_cnt_native_cleared++;
#endif
        apr_atomic_inc32(&no->interrupted);
        if (no->mutex) {
            /* Lock only if we are the owner of the mutex */
            apr_thread_mutex_lock(no->mutex);
        }
#if SIGHT_APR_REFCOUNT
        refcount = apr_atomic_read32(&no->refcount);
        while (refcount) {
            apr_thread_yield();
            refcount = apr_atomic_read32(&no->refcount);
            if (ioc++ > OPTIMISTIC_LOCK_CNT) {
                /* Sleep one millisecond  */
                apr_sleep(1000L);
            }
            if (ioc > OPTIMISTIC_LOCK_MAX) {
                /* TODO: We have a zombie or a lengthy JNI op.
                 * Find a way to bail out without crushing JVM
                 */
#ifdef SIGHT_DO_STATS
                fprintf(stderr, "NativeObject.clear() call is locked ...\n");
                fprintf(stderr, "Bailing out !\n");
                fflush(stderr);
#endif
                throwAprException(_E, APR_TIMEUP);
                return;
            }
        }
#endif
        if (no->pool)
            apr_pool_cleanup_kill(no->pool, no, native_object_cleanup);
        if (no->clean)
            (*no->clean)(POOL_CLEAR, no);
        if (no->pool) {
            sight_pool_clear(no->pool);
            apr_pool_cleanup_register(no->pool, (const void *)no,
                                      native_object_cleanup,
                                      apr_pool_cleanup_null);
            no->opaque = NULL;
            no->native = NULL;
        }
        if (no->mutex)
            apr_thread_mutex_unlock(no->mutex);
        if (mutex)
            apr_thread_mutex_unlock(mutex);
        apr_atomic_set32(&no->interrupted, 0);
    }
}

SIGHT_EXPORT_DECLARE(void, NativeObject, intr0)(SIGHT_STDARGS,
                                                jlong instance)
{
    sight_object_t *no = J2P(instance, sight_object_t *);

    UNREFERENCED_STDARGS;
    if (no) {
        apr_atomic_inc32(&no->interrupted);
    }
}

/* Temporary debug method for testing long native operations
 */
SIGHT_EXPORT_DECLARE(void, NativeObject, sleep0)(SIGHT_STDARGS,
                                                 jlong instance,
                                                 jlong step,
                                                 jlong time)
{
    sight_object_t *no = J2P(instance, sight_object_t *);

    UNREFERENCED_STDARGS;
    if (!no)
        return;
    SIGHT_LOCAL_TRY(no) {
        jlong i;
        for (i = 0; i < step; i++) {
            if (SIGHT_LOCAL_IRQ(no)) {
                SIGHT_LOCAL_BRK(no);
                return;
            }
            apr_sleep(J2T(time));
        }

    } SIGHT_LOCAL_END(no);
}
