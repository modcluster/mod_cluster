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

extern apr_pool_t *sight_global_pool;
extern apr_pool_t *sight_temp_pool;

static void dir_cleanup(int mode, sight_object_t *no)
{
    /*
     * In case this is a pool callback do not
     * close the directory. It will be closed
     * by the original apr pool callback
     */
    if (mode != POOL_CALLBACK && no && no->native) {
        apr_dir_close((apr_dir_t *)no->native);
        no->native = NULL;
    }
}

SIGHT_EXPORT_DECLARE(jint, Directory, open0)(SIGHT_STDARGS,
                                             jlong instance,
                                             jstring name)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    apr_status_t rv = APR_ENOPOOL;
    apr_dir_t *d = NULL;
    SIGHT_ALLOC_CSTRING(name);

    UNREFERENCED_O;

    if (!no || !no->pool) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOPOOL));
        rv = APR_ENOPOOL;
        goto cleanup;
    }
    SIGHT_LOCAL_TRY(no) {
        if ((rv = apr_dir_open(&d, J2S(name), no->pool)) != APR_SUCCESS) {
            throwAprException(_E, rv);
        }
        else {
            no->clean  = dir_cleanup;
            no->native = d;
        }
    } SIGHT_LOCAL_END(no);
cleanup:
    SIGHT_FREE_CSTRING(name);
    return rv;
}

SIGHT_EXPORT_DECLARE(jint, Directory, attach0)(SIGHT_STDARGS,
                                               jlong instance,
                                               jlong dh)
{
    sight_object_t *no = J2P(instance, sight_object_t *);

    UNREFERENCED_O;
    if (!no || !no->pool) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOPOOL));
        return APR_ENOPOOL;
    }
    no->clean  = NULL;
    no->native = J2P(dh, apr_dir_t *);

    return APR_SUCCESS;
}

SIGHT_EXPORT_DECLARE(jint, Directory, rewind0)(SIGHT_STDARGS,
                                               jlong instance)
{
    sight_object_t *no = J2P(instance, sight_object_t *);

    UNREFERENCED_O;
    if (!no || !no->native) {
        return APR_EINVAL;
    }
    return apr_dir_rewind((apr_dir_t *)no->native);
}

SIGHT_EXPORT_DECLARE(jobject, Directory, read0)(SIGHT_STDARGS,
                                                jlong instance,
                                                jint wanted)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    apr_finfo_t info;
    apr_dir_t *d;
    jobject fi = NULL;
    apr_status_t rc;

    if (!no || !no->pool) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOPOOL));
        return NULL;
    }
    if (!no->native) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOFILE));
        return NULL;
    }
    else
        d = (apr_dir_t *)no->native;
    SIGHT_LOCAL_TRY(no) {
        if (!(fi = sight_new_finfo_class(_E, _O))) {
            SIGHT_LOCAL_BRK(no);
            return NULL;
        }
        rc = apr_dir_read(&info, wanted, d);
        if (rc == APR_SUCCESS || APR_STATUS_IS_INCOMPLETE(rc)) {
            sight_finfo_fill(_E, fi, &info);
        }
        else {
            (*_E)->DeleteLocalRef(_E, fi);
            /* Do not throw exceptions */
            fi = NULL;
        }
    } SIGHT_LOCAL_END(no);
    return fi;
}

SIGHT_EXPORT_DECLARE(jint, Directory, create0)(SIGHT_STDARGS,
                                               jstring name,
                                               jint perms,
                                               jboolean recursive)
{
    apr_status_t rv = APR_ENOPOOL;
    apr_pool_t *p = NULL;
    apr_fileperms_t perm = (apr_fileperms_t)perms;
    SIGHT_ALLOC_CSTRING(name);

    UNREFERENCED_O;

    SIGHT_GLOBAL_TRY {
        if ((rv = apr_pool_create(&p, sight_temp_pool)) != APR_SUCCESS) {
            throwAprException(_E, rv);
            SIGHT_GLOBAL_BRK();
            goto cleanup;
        }
        if (recursive)
            rv = apr_dir_make_recursive(J2S(name), perm, p);
        else
            rv = apr_dir_make(J2S(name), perm, p);
        if (p)
            apr_pool_destroy(p);
    } SIGHT_GLOBAL_END();
cleanup:
    SIGHT_FREE_CSTRING(name);
    return rv;
}

static apr_off_t calc_size_r(sight_object_t *no, apr_dir_t *dir, const char *base, apr_pool_t *pool)
{

    apr_off_t nsize = 0;
    apr_finfo_t  fi;
    apr_status_t rc;

    do {
        rc = apr_dir_read(&fi, APR_FINFO_SIZE | APR_FINFO_TYPE, dir);
        if (rc == APR_SUCCESS) {
            if (fi.filetype == APR_DIR && fi.name) {
                char *subdir;
                apr_pool_t *sp;
                apr_dir_t  *sd;
                /* Strip out '.' and '..' */
                if (fi.name[0] == '.') {
                    if (!fi.name[1])
                        continue;
                    else if (fi.name[1] == '.' && !fi.name[2])
                        continue;
                }
                /* Create directory subpool so that we don't grow
                 * memory too much.
                 */
                if (apr_pool_create(&sp, pool) != APR_SUCCESS)
                    continue;
                subdir = apr_pstrcat(sp, base, "/", fi.name, NULL);
                if (apr_dir_open(&sd, subdir, sp) == APR_SUCCESS)
                    nsize += calc_size_r(no, sd, subdir, sp);
                apr_pool_destroy(sp);
            }
            else {
                nsize += fi.size;
            }
        }
        if (SIGHT_LOCAL_IRQ(no)) {
            return nsize;
        }

    } while (rc == APR_SUCCESS || APR_STATUS_IS_INCOMPLETE(rc));

    return nsize;
}

static apr_off_t calc_size_d(sight_object_t *no)
{

    apr_off_t nsize = 0;
    apr_finfo_t  fi;
    apr_status_t rc;
    apr_dir_t *dir = (apr_dir_t *)no->native;
    do {
        rc = apr_dir_read(&fi, APR_FINFO_SIZE | APR_FINFO_TYPE, dir);
        if (rc == APR_SUCCESS) {
            if (fi.filetype != APR_DIR) {
               nsize += fi.size;
            }
        }
        if (SIGHT_LOCAL_IRQ(no)) {
            return nsize;
        }
    } while (rc == APR_SUCCESS || APR_STATUS_IS_INCOMPLETE(rc));

    return nsize;
}

SIGHT_EXPORT_DECLARE(jlong, Directory, size0)(SIGHT_STDARGS,
                                              jlong instance,
                                              jboolean recursive,
                                              jstring path)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    apr_off_t len = 0;
    SIGHT_ALLOC_CSTRING(path);

    UNREFERENCED_O;
    if (!no || !no->pool) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOPOOL));
        goto cleanup;
    }
    if (!no->native) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOFILE));
        goto cleanup;
    }
    SIGHT_LOCAL_TRY(no) {
        if (recursive)
            len = calc_size_r(no, (apr_dir_t *)no->native, J2S(path), no->pool);
        else
            len = calc_size_d(no);
    } SIGHT_LOCAL_END(no);

cleanup:
    SIGHT_FREE_CSTRING(path);
    return (jlong)len;
}

SIGHT_EXPORT_DECLARE(jint, Directory, remove0)(SIGHT_STDARGS,
                                               jstring name)
{
    apr_status_t rv = APR_ENOPOOL;
    SIGHT_ALLOC_CSTRING(name);

    UNREFERENCED_O;

    SIGHT_GLOBAL_TRY {
        rv = apr_dir_remove(J2S(name), NULL);
    } SIGHT_GLOBAL_END();
    SIGHT_FREE_CSTRING(name);
    return rv;
}
