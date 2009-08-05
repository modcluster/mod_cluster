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
#include "sight_types.h"

static apr_status_t callback_object_cleanup(void *data)
{
    apr_status_t rv = APR_SUCCESS;
    sight_callback_t *cb = (sight_callback_t *)data;

    if (data) {
        JNIEnv *_E = NULL;
        jint em = sight_get_jnienv(&_E);
        if (_E && cb->object) {
            /* TODO: See if wee need to detach in case
             * callback call native methods
             */
            if (cb->method) {
                rv = (*_E)->CallIntMethod(_E, cb->object, cb->method, NULL);
                if ((*_E)->ExceptionCheck(_E)) {
                    /* Just clear any pending exceptions */
                    (*_E)->ExceptionClear(_E);
                }
            }
            (*_E)->DeleteGlobalRef(_E, cb->object);
        }
        sight_clr_jnienv(em);
        free(cb);
    }
    return rv;
}

SIGHT_EXPORT_DECLARE(jlong, Pool, register0)(SIGHT_STDARGS,
                                             jlong instance,
                                             jobject obj)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_callback_t *cb = NULL;
    jclass c;

    UNREFERENCED_O;
    if (!no || !no->pool) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOPOOL));
        return 0;
    }
    if (!(cb = (sight_callback_t *)sight_calloc(_E,
                                    sizeof(sight_callback_t),
                                    THROW_FMARK))) {
        return 0;
    }
    SIGHT_LOCAL_TRY(no) {
        c = (*_E)->GetObjectClass(_E, obj);
        cb->name   = "cleanup";
        cb->msig   = "()I";
        cb->object = (*_E)->NewGlobalRef(_E, obj);
        cb->method = (*_E)->GetMethodID(_E, c, cb->name, cb->msig);

        apr_pool_cleanup_register(no->pool, (const void *)cb,
                                  callback_object_cleanup,
                                  apr_pool_cleanup_null);
    } SIGHT_LOCAL_END(no);
    return P2J(cb);
}

SIGHT_EXPORT_DECLARE(void, Pool, kill0)(SIGHT_STDARGS,
                                        jlong instance,
                                        jlong data)
{
    sight_object_t *no   = J2P(instance, sight_object_t *);
    sight_callback_t *cb = J2P(data, sight_callback_t *);

    UNREFERENCED_O;
    if (!no || !no->pool) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOPOOL));
        return;
    }
    if (!cb) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENULL));
        return;
    }
    SIGHT_LOCAL_TRY(no) {
        apr_pool_cleanup_kill(no->pool, cb, callback_object_cleanup);
    } SIGHT_LOCAL_END(no);
    if (cb->object)
        (*_E)->DeleteGlobalRef(_E, cb->object);
    free(cb);
}
