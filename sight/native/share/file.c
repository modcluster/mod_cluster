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

static void file_cleanup(int mode, sight_object_t *no)
{
    if (no && no->native) {
        apr_file_close((apr_file_t *)no->native);
        no->native = NULL;
    }
}

SIGHT_EXPORT_DECLARE(jint, File, create0)(SIGHT_STDARGS,
                                          jlong instance,
                                          jstring name,
                                          jint flag,
                                          jint perm)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    apr_status_t rv;
    apr_file_t *f = NULL;
    apr_int32_t fflag = (apr_int32_t)flag | APR_FOPEN_NOCLEANUP;
    SIGHT_ALLOC_CSTRING(name);

    UNREFERENCED_O;
    if (!no || !no->pool) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOPOOL));
        rv = APR_ENOPOOL;
        goto cleanup;
    }
    SIGHT_LOCAL_TRY(no) {
        if ((rv = apr_file_open(&f, J2S(name), fflag,
                         (apr_fileperms_t)perm, no->pool)) != APR_SUCCESS) {
            throwAprException(_E, rv);
        }
        else {
            no->clean  = file_cleanup;
            no->native = f;
        }
    } SIGHT_LOCAL_END(no);
cleanup:
    SIGHT_FREE_CSTRING(name);
    return rv;
}

SIGHT_EXPORT_DECLARE(jint, File, attach0)(SIGHT_STDARGS,
                                          jlong instance,
                                          jlong fh)
{
    sight_object_t *no = J2P(instance, sight_object_t *);

    UNREFERENCED_O;
    if (!no || !no->pool) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOPOOL));
        return APR_ENOPOOL;
    }
    no->clean  = NULL;
    no->native = J2P(fh, apr_file_t *);

    return APR_SUCCESS;
}

SIGHT_EXPORT_DECLARE(jint, File, mktemp0)(SIGHT_STDARGS,
                                          jlong instance,
                                          jstring templ,
                                          jint flag)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    apr_status_t rv = APR_ENOPOOL;
    apr_file_t *f = NULL;
    apr_int32_t fflag = (apr_int32_t)flag | APR_FOPEN_NOCLEANUP;
    char *ctempl = NULL;

    UNREFERENCED_O;
    if (!no || !no->pool) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOPOOL));
        rv = APR_ENOPOOL;
        goto cleanup;
    }
    if (!(ctempl = sight_strdupj(_E, templ))) {
        rv = APR_ENOMEM;
        goto cleanup;
    }
    SIGHT_LOCAL_TRY(no) {
        if ((rv = apr_file_mktemp(&f, ctempl,
                         fflag, no->pool)) != APR_SUCCESS) {
            throwAprException(_E, rv);
        }
        else {
            no->clean  = file_cleanup;
            no->native = f;
        }
    } SIGHT_LOCAL_END(no);
cleanup:
    SIGHT_FREE(ctempl);
    return rv;
}

SIGHT_EXPORT_DECLARE(jint, File, getstd0)(SIGHT_STDARGS,
                                          jlong instance,
                                          jint which)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    apr_status_t rv = APR_ENOPOOL;
    apr_file_t *f = NULL;

    UNREFERENCED_O;
    if (!no || !no->pool) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOPOOL));
        return APR_ENOPOOL;
    }
    SIGHT_LOCAL_TRY(no) {
        switch (which) {
            case 0:
                rv = apr_file_open_stdin(&f, no->pool);
            break;
            case 1:
                rv = apr_file_open_stdout(&f, no->pool);
            break;
            default:
                rv = apr_file_open_stderr(&f, no->pool);
            break;

        }
        if (rv == APR_SUCCESS) {
            no->clean  = NULL;
            no->native = f;
        }
        else
            throwAprException(_E, rv);
    } SIGHT_LOCAL_END(no);
    return rv;
}

SIGHT_EXPORT_DECLARE(jlong, File, seek0)(SIGHT_STDARGS,
                                         jlong instance,
                                         jint where,
                                         jlong offset)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    apr_off_t pos = (apr_off_t)offset;
    apr_seek_where_t w;
    apr_status_t rv;

    UNREFERENCED_O;
    if (!no || !no->native) {
        throwIOException(_E, sight_strerror(SIGHT_ENOFILE));
        return 0;
    }
    SIGHT_LOCAL_TRY(no) {
        switch (where) {
            case 1:
                w = APR_CUR;
                break;
            case 2:
                w = APR_END;
                break;
            default:
                w = APR_SET;
                break;
        }
        if ((rv = apr_file_seek((apr_file_t *)no->native,
                                w, &pos)) != APR_SUCCESS) {
            throwAprIOException(_E, rv);
        }
    } SIGHT_LOCAL_END(no);
    return (jlong)pos;
}

SIGHT_EXPORT_DECLARE(jstring, File, name0)(SIGHT_STDARGS,
                                           jlong instance)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    const char *fname = NULL;

    UNREFERENCED_O;
    if (!no || !no->native) {
        return NULL;
    }
    if (apr_file_name_get(&fname, (apr_file_t *)no->native) == APR_SUCCESS) {
        RETURN_JCSTR(fname);
    }
    else
        return NULL;
}

SIGHT_EXPORT_DECLARE(jint, File, trunc0)(SIGHT_STDARGS,
                                         jlong instance,
                                         jlong offset)
{
    sight_object_t *no = J2P(instance, sight_object_t *);

    UNREFERENCED_STDARGS;
    if (!no || !no->native) {
        return APR_EINVAL;
    }
    return apr_file_trunc((apr_file_t *)no->native, (apr_off_t)offset);
}

SIGHT_EXPORT_DECLARE(jint, File, flags0)(SIGHT_STDARGS,
                                         jlong instance)
{
    sight_object_t *no = J2P(instance, sight_object_t *);

    UNREFERENCED_STDARGS;
    if (!no || !no->native) {
        return 0;
    }
    return (jint)apr_file_flags_get((apr_file_t *)no->native);
}

SIGHT_EXPORT_DECLARE(jint, File, eof0)(SIGHT_STDARGS,
                                       jlong instance)
{
    sight_object_t *no = J2P(instance, sight_object_t *);

    UNREFERENCED_STDARGS;
    if (!no || !no->native) {
        return APR_EINVAL;
    }
    return apr_file_eof((apr_file_t *)no->native);
}

SIGHT_EXPORT_DECLARE(jint, File, lock0)(SIGHT_STDARGS,
                                        jlong instance,
                                        jint flags)
{
    sight_object_t *no = J2P(instance, sight_object_t *);

    UNREFERENCED_STDARGS;
    if (!no || !no->native) {
        return APR_EINVAL;
    }
    return apr_file_lock((apr_file_t *)no->native, (int)flags);
}

SIGHT_EXPORT_DECLARE(jint, File, unlock0)(SIGHT_STDARGS,
                                          jlong instance)
{
    sight_object_t *no = J2P(instance, sight_object_t *);

    UNREFERENCED_STDARGS;
    if (!no || !no->native) {
        return APR_EINVAL;
    }
    return apr_file_unlock((apr_file_t *)no->native);
}

SIGHT_EXPORT_DECLARE(jint, File, flush0)(SIGHT_STDARGS,
                                         jlong instance)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    apr_status_t rv;

    UNREFERENCED_O;
    if (!no || !no->native) {
        throwIOException(_E, sight_strerror(SIGHT_ENOFILE));
        return APR_EINVAL;
    }
    if ((rv = apr_file_flush((apr_file_t *)no->native)) != APR_SUCCESS) {
        throwAprIOException(_E, rv);
    }
    return rv;
}

SIGHT_EXPORT_DECLARE(jint, File, putc0)(SIGHT_STDARGS,
                                        jlong instance,
                                        jint ch)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    apr_status_t rv;

    UNREFERENCED_O;
    if (!no || !no->native) {
        throwIOException(_E, sight_strerror(SIGHT_ENOFILE));
        return APR_EINVAL;
    }
    if ((rv = apr_file_putc((char)ch,
                            (apr_file_t *)no->native)) != APR_SUCCESS) {
        throwAprIOException(_E, rv);
    }
    return rv;
}

SIGHT_EXPORT_DECLARE(jint, File, write0)(SIGHT_STDARGS,
                                         jlong instance,
                                         jbyteArray buf,
                                         jint offset,
                                         jint towrite)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    apr_size_t nbytes;
    apr_size_t nwr = 0;
    apr_status_t rv;

    UNREFERENCED_O;
    if (!no || !no->native) {
        throwIOException(_E, sight_strerror(SIGHT_ENOFILE));
        return 0;
    }
    if (towrite < 0)
        towrite = (*_E)->GetArrayLength(_E, buf);
    nbytes = (apr_size_t)towrite;
    if (towrite <= SIGHT_HBUFFER_SIZ) {
        jbyte sb[SIGHT_HBUFFER_SIZ];
        (*_E)->GetByteArrayRegion(_E, buf, offset, towrite, &sb[0]);
        rv = apr_file_write_full((apr_file_t *)no->native,
                                 &sb[0], nbytes, &nwr);
    }
    else {
        jbyte *sb = (jbyte *)sight_malloc(_E, nbytes, THROW_FMARK);
        if (sb == NULL)
            return 0;
        (*_E)->GetByteArrayRegion(_E, buf, offset, towrite, sb);
        rv = apr_file_write_full((apr_file_t *)no->native,
                                 sb, nbytes, &nwr);
        SIGHT_FREE(sb);
    }
    if (rv != APR_SUCCESS)
        throwAprIOException(_E, rv);

    return (jint)nwr;
}

SIGHT_EXPORT_DECLARE(jint, File, getc0)(SIGHT_STDARGS,
                                        jlong instance)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    apr_status_t rv;
    unsigned char ch;

    UNREFERENCED_O;
    if (!no || !no->native) {
        throwIOException(_E, sight_strerror(SIGHT_ENOFILE));
        return -1;
    }
    if ((rv = apr_file_getc((char *)&ch, (apr_file_t *)no->native)) != APR_SUCCESS) {
        if (rv != APR_EOF)
            throwAprIOException(_E, rv);
        return -1;
    }
    return (jint)ch;
}

SIGHT_EXPORT_DECLARE(jint, File, read0)(SIGHT_STDARGS,
                                        jlong instance,
                                        jbyteArray buf,
                                        jint offset,
                                        jint toread)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    apr_size_t nbytes;
    apr_status_t rv;

    UNREFERENCED_O;
    if (!no || !no->native) {
        throwIOException(_E, sight_strerror(SIGHT_ENOFILE));
        return -1;
    }
    if (toread < 0)
        toread = (*_E)->GetArrayLength(_E, buf);
    nbytes = (apr_size_t)toread;
    if (toread <= SIGHT_HBUFFER_SIZ) {
        jbyte sb[SIGHT_HBUFFER_SIZ];
        rv = apr_file_read((apr_file_t *)no->native,
                           &sb[0], &nbytes);
        if (rv == APR_SUCCESS || rv == APR_EOF) {
            if (nbytes > 0 ) {
                (*_E)->SetByteArrayRegion(_E, buf, offset,
                                          (jsize)nbytes, (jbyte*)&sb[0]);
                return (jint)nbytes;
            }
        }
        else {
            throwAprIOException(_E, rv);
        }
    }
    else {
        jbyte *bytes = (*_E)->GetByteArrayElements(_E, buf, NULL);
        rv = apr_file_read((apr_file_t *)no->native,
                           bytes + offset, &nbytes);
        if (rv == APR_SUCCESS || rv == APR_EOF) {
            if (nbytes > 0) {
                (*_E)->ReleaseByteArrayElements(_E, buf, bytes, 0);
                return (jint)nbytes;
            }
        }
        else {
            (*_E)->ReleaseByteArrayElements(_E, buf, bytes, JNI_ABORT);
             throwAprIOException(_E, rv);
        }
    }
    return -1;
}

SIGHT_EXPORT_DECLARE(jint, File, dup0)(SIGHT_STDARGS,
                                       jlong instance,
                                       jlong srcinst)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_object_t *ns = J2P(srcinst,  sight_object_t *);
    apr_status_t rv;
    apr_file_t *f = NULL;

    UNREFERENCED_O;
    if (!no || !no->pool) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOPOOL));
        return APR_ENOPOOL;
    }
    if (!ns || !ns->native) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOFILE));
        return APR_EINVAL;
    }
    rv = apr_file_dup(&f, (apr_file_t *)ns->native, no->pool);
    if (rv == APR_SUCCESS) {
        no->clean  = file_cleanup;
        no->native = f;
    }
    else {
        no->clean  = NULL;
        no->native = NULL;
        throwAprException(_E, rv);
    }
    return rv;
}

SIGHT_EXPORT_DECLARE(jint, File, dup2)(SIGHT_STDARGS,
                                       jlong instance,
                                       jlong srcinst)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_object_t *ns = J2P(srcinst,  sight_object_t *);
    apr_status_t rv;

    UNREFERENCED_O;
    if (!no || !no->pool) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOPOOL));
        return APR_ENOPOOL;
    }
    if (!no->native || !ns || !ns->native) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOFILE));
        return APR_EINVAL;
    }
    rv = apr_file_dup2((apr_file_t *)no->native, (apr_file_t *)ns->native,
                       no->pool);
    if (rv != APR_SUCCESS)
        throwAprException(_E, rv);
    return rv;
}

SIGHT_EXPORT_DECLARE(jint, File, attrs0)(SIGHT_STDARGS,
                                         jstring name,
                                         jint attr,
                                         jint mask)
{
    apr_fileattrs_t fa = (apr_fileattrs_t)attr;
    apr_fileattrs_t fm = (apr_fileattrs_t)mask;
    apr_status_t rv = APR_ENOTIMPL;
    apr_pool_t *p = NULL;
    SIGHT_ALLOC_CSTRING(name);

    UNREFERENCED_O;
    SIGHT_GLOBAL_TRY {
        if (!sight_temp_pool) {
            throwNullPointerException(_E, THROW_FMARK,
                                      sight_strerror(SIGHT_ENOPOOL));
            rv = APR_ENOPOOL;
            goto cleanup;
        }
        if ((rv = apr_pool_create(&p, sight_temp_pool)) != APR_SUCCESS) {
            throwAprException(_E, rv);
            p = NULL;
            goto cleanup;
        }
        rv = apr_file_attrs_set(J2S(name), fa, fm, p);
        if (rv != APR_SUCCESS)
            throwAprException(_E, rv);
    cleanup:
        if (sight_temp_pool && p)
            apr_pool_destroy(p);
    } SIGHT_GLOBAL_END();
    SIGHT_FREE_CSTRING(name);
    return rv;
}

SIGHT_EXPORT_DECLARE(jint, File, perms0)(SIGHT_STDARGS,
                                         jstring name,
                                         jint perms)
{
    apr_fileperms_t fp = (apr_fileperms_t)perms;
    apr_status_t rv = APR_ENOTIMPL;
    SIGHT_ALLOC_CSTRING(name);

    UNREFERENCED_O;
    rv = apr_file_perms_set(J2S(name), fp);
    SIGHT_FREE_CSTRING(name);
    return rv;
}

SIGHT_EXPORT_DECLARE(jobject, File, finfo0)(SIGHT_STDARGS,
                                            jlong instance,
                                            jint wanted)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    apr_finfo_t info;
    apr_file_t *f;
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
        f = (apr_file_t *)no->native;
    SIGHT_LOCAL_TRY(no) {
        if ((fi = sight_new_finfo_class(_E, _O))) {
            rc = apr_file_info_get(&info, wanted, f);
            if (rc == APR_SUCCESS || APR_STATUS_IS_INCOMPLETE(rc)) {
                sight_finfo_fill(_E, fi, &info);
            }
            else {
                (*_E)->DeleteLocalRef(_E, fi);
                /* Do not throw exceptions */
                fi = NULL;
                throwAprException(_E, rc);
            }
        }
    } SIGHT_LOCAL_END(no);
    return fi;
}
