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

void dbprintf(char *format, ...)
{
    va_list args;
    char buffer[1024];
    int  len;
#if defined(WIN32)
    char tid[1024 + 128];

    if (!format) {
        len = FormatMessageA(FORMAT_MESSAGE_FROM_SYSTEM |
                             FORMAT_MESSAGE_IGNORE_INSERTS,
                             NULL,
                             GetLastError(),
                             MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
                             buffer,
                             1024,
                             NULL);
    }
    else
#endif
    {
        va_start(args, format);
        len = vsprintf(buffer, format, args);
        va_end(args);
    }
    if (len > 0) {
#if defined(WIN32)
        sprintf(tid, "[%04X] %s", GetCurrentThreadId(), buffer);
        OutputDebugStringA(tid);
#else
        fputs(buffer, stderr);
        fflush(stderr);
#endif
    }
}

char *sight_strerror(int code)
{
    switch (code) {
        case SIGHT_ENOPOOL:
            return "APR was not provided a pool with which to allocate memory";
        case SIGHT_ENOFILE:
            return "Invalid APR file handle";
        case SIGHT_ENOMUTEX:
            return "Invalid APR proc mutex handle";
        case SIGHT_ENOPROC:
            return "Invalid APR process handle";
        case SIGHT_ENULL:
            return "Missing parameter for the specified operation";
        case SIGHT_ENOMEM:
            return "Insufficient kernel memory was available";
        default:
            return "Unknown error";
    }
}

/*
 * Convenience function to help throw any class
 */
void throwClass(JNIEnv *env, const char *clazz, const char *msg)
{
    jclass ec;

    ec = (*env)->FindClass(env, clazz);
    if (ec == NULL) {
        fprintf(stderr, "Cannot find %s class\n", clazz);
        return;
    }
    (*env)->ThrowNew(env, ec, msg);
    (*env)->DeleteLocalRef(env, ec);
}

/*
 * Convenience function to help throw an java.lang.OutOfMemoryError.
 */
void throwDebug(JNIEnv *env, const char *clazz, const char *file,
                int line, const char *msg)
{
    if (file) {
        char fmt[SIGHT_HBUFFER_SIZ];
        char *f = (char *)(file + strlen(file) - 1);
        while (f != file && '\\' != *f && '/' != *f) {
            f--;
        }
        if (f != file) {
            f++;
        }
        if (msg)
            sprintf(fmt, "%s (%s)+%d", msg, f, line);
        else
            sprintf(fmt, "(%s)+%d", msg, f, line);
        throwClass(env, clazz, &fmt[0]);
    }
    else {
        throwClass(env, clazz, msg);
    }
}


/*
 * Convenience function to help throw an java.lang.Exception.
 */
void throwException(JNIEnv *env, const char *msg)
{
    throwClass(env, "java/lang/Exception", msg);
}

/*
 * Convenience function to help throw an java.io.IOException.
 */
void throwIOException(JNIEnv *env, const char *msg)
{
    throwClass(env, "java/io/IOException", msg);
}

/*
 * Convenience function to help throw an java.lang.OutOfMemoryError.
 */
void throwMemoryException(JNIEnv *env, const char *file, int line,
                          const char *msg)
{
    throwDebug(env, "java/lang/OutOfMemoryError", file, line, msg);
}

/*
 * Convenience function to help throw an java.lang.NullPointerException.
 */
void throwNullPointerException(JNIEnv *env, const char *file, int line,
                               const char *msg)
{
    throwDebug(env, "java/lang/NullPointerException", file, line, msg);
}

/*
 * Convenience function to help throw an java.lang.IllegalArgumentException.
 */
void throwArgumentException(JNIEnv *env, const char *file, int line,
                            const char *msg)
{
    throwDebug(env, "java/lang/IllegalArgumentException", file, line, msg);
}


/*
 * Convenience function to help throw an java.lang.Exception
 * with string representation of APR error code.
 */
void throwAprException(JNIEnv *env, apr_status_t err)
{
    char msg[SIGHT_MBUFFER_SIZ] = { 0 };

    apr_strerror(err, msg, SIGHT_MBUFFER_SIZ);
    throwClass(env, SIGHT_CLASS_PATH "OperatingSystemException", msg);
}

void throwOSException(JNIEnv *env, const char *msg)
{
    throwClass(env, SIGHT_CLASS_PATH "OperatingSystemException", msg);
}

void throwAprMemoryException(JNIEnv *env, const char *file, int line,
                             apr_status_t err)
{
    char msg[SIGHT_MBUFFER_SIZ] = { 0 };

    apr_strerror(err, msg, SIGHT_MBUFFER_SIZ);
    throwDebug(env, "java/lang/OutOfMemoryError", file, line, msg);
}

/*
 * Convenience function to help throw an java.io.IOException
 * with string representation of APR error code.
 */
void throwAprExceptionEx(JNIEnv *env, const char *file, int line,
                           apr_status_t err)
{
    char msg[SIGHT_MBUFFER_SIZ] = { 0 };

    apr_strerror(err, msg, SIGHT_MBUFFER_SIZ);
    throwDebug(env, SIGHT_CLASS_PATH "OperatingSystemException",
               file, line, msg);
}

/*
 * Convenience function to help throw an java.lang.Exception
 * with string representation of APR error code.
 */
void throwAprIOException(JNIEnv *env, apr_status_t err)
{
    char msg[SIGHT_MBUFFER_SIZ] = { 0 };

    apr_strerror(err, msg, SIGHT_MBUFFER_SIZ);
    throwIOException(env, msg);
}

/*
 * Convenience function to help throw an java.io.IOException
 * with string representation of APR error code.
 */
void throwAprIOExceptionEx(JNIEnv *env, const char *file, int line,
                           apr_status_t err)
{
    char msg[SIGHT_MBUFFER_SIZ] = { 0 };

    apr_strerror(err, msg, SIGHT_MBUFFER_SIZ);
    throwDebug(env, "java/io/IOException", file, line, msg);
}

SIGHT_EXPORT_DECLARE(jint, Error, getOsError)(SIGHT_STDARGS)
{
    UNREFERENCED_STDARGS;
    return (jint)apr_get_os_error();
}

SIGHT_EXPORT_DECLARE(jint, Error, getNetworkError)(SIGHT_STDARGS)
{
    UNREFERENCED_STDARGS;
    return (jint)apr_get_netos_error();
}

SIGHT_EXPORT_DECLARE(jstring, Error, getError)(SIGHT_STDARGS, jint err)
{
    char serr[512] = {0};
    UNREFERENCED_O;

    apr_strerror(err, serr, 512);
    RETURN_JCSTR(serr);
}

/* Merge IS_ETIMEDOUT with APR_TIMEUP
 */
#define SIGHT_STATUS_IS_ETIMEDOUT(x) (APR_STATUS_IS_ETIMEDOUT((x)) || ((x) == APR_TIMEUP))

SIGHT_EXPORT_DECLARE(jboolean, Status, is)(SIGHT_STDARGS, jint err, jint idx)
{
#define APR_IS(I, E) case I: if (E(err)) return JNI_TRUE; break
#define APR_ISX(I, E, T) case I: if (E(err) || (err == T)) return JNI_TRUE; break

    UNREFERENCED_STDARGS;
    switch (idx) {
        APR_IS(1,  APR_STATUS_IS_ENOSTAT);
        APR_IS(2,  APR_STATUS_IS_ENOPOOL);
        /* empty slot: +3 */
        APR_IS(4,  APR_STATUS_IS_EBADDATE);
        APR_IS(5,  APR_STATUS_IS_EINVALSOCK);
        APR_IS(6,  APR_STATUS_IS_ENOPROC);
        APR_IS(7,  APR_STATUS_IS_ENOTIME);
        APR_IS(8,  APR_STATUS_IS_ENODIR);
        APR_IS(9,  APR_STATUS_IS_ENOLOCK);
        APR_IS(10, APR_STATUS_IS_ENOPOLL);
        APR_IS(11, APR_STATUS_IS_ENOSOCKET);
        APR_IS(12, APR_STATUS_IS_ENOTHREAD);
        APR_IS(13, APR_STATUS_IS_ENOTHDKEY);
        APR_IS(14, APR_STATUS_IS_EGENERAL);
        APR_IS(15, APR_STATUS_IS_ENOSHMAVAIL);
        APR_IS(16, APR_STATUS_IS_EBADIP);
        APR_IS(17, APR_STATUS_IS_EBADMASK);
        /* empty slot: +18 */
        APR_IS(19, APR_STATUS_IS_EDSOOPEN);
        APR_IS(20, APR_STATUS_IS_EABSOLUTE);
        APR_IS(21, APR_STATUS_IS_ERELATIVE);
        APR_IS(22, APR_STATUS_IS_EINCOMPLETE);
        APR_IS(23, APR_STATUS_IS_EABOVEROOT);
        APR_IS(24, APR_STATUS_IS_EBADPATH);
        APR_IS(25, APR_STATUS_IS_EPATHWILD);
        APR_IS(26, APR_STATUS_IS_ESYMNOTFOUND);
        APR_IS(27, APR_STATUS_IS_EPROC_UNKNOWN);
        APR_IS(28, APR_STATUS_IS_ENOTENOUGHENTROPY);


        /* APR_Error */
        APR_IS(51, APR_STATUS_IS_INCHILD);
        APR_IS(52, APR_STATUS_IS_INPARENT);
        APR_IS(53, APR_STATUS_IS_DETACH);
        APR_IS(54, APR_STATUS_IS_NOTDETACH);
        APR_IS(55, APR_STATUS_IS_CHILD_DONE);
        APR_IS(56, APR_STATUS_IS_CHILD_NOTDONE);
        APR_ISX(57, APR_STATUS_IS_TIMEUP, SIGHT_TIMEUP);
        APR_IS(58, APR_STATUS_IS_INCOMPLETE);
        /* empty slot: +9 */
        /* empty slot: +10 */
        /* empty slot: +11 */
        APR_IS(62, APR_STATUS_IS_BADCH);
        APR_IS(63, APR_STATUS_IS_BADARG);
        APR_IS(64, APR_STATUS_IS_EOF);
        APR_IS(65, APR_STATUS_IS_NOTFOUND);
        /* empty slot: +16 */
        /* empty slot: +17 */
        /* empty slot: +18 */
        APR_IS(69, APR_STATUS_IS_ANONYMOUS);
        APR_IS(70, APR_STATUS_IS_FILEBASED);
        APR_IS(71, APR_STATUS_IS_KEYBASED);
        APR_IS(72, APR_STATUS_IS_EINIT);
        APR_IS(73, APR_STATUS_IS_ENOTIMPL);
        APR_IS(74, APR_STATUS_IS_EMISMATCH);
        APR_IS(75, APR_STATUS_IS_EBUSY);
        /* Socket errors */
        APR_ISX(90, APR_STATUS_IS_EAGAIN, SIGHT_EAGAIN);
        APR_ISX(91, SIGHT_STATUS_IS_ETIMEDOUT, SIGHT_ETIMEDOUT);
        APR_IS(92, APR_STATUS_IS_ECONNABORTED);
        APR_IS(93, APR_STATUS_IS_ECONNRESET);
        APR_ISX(94, APR_STATUS_IS_EINPROGRESS, SIGHT_EINPROGRESS);
        APR_ISX(95, APR_STATUS_IS_EINTR, SIGHT_EINTR);
        APR_IS(96, APR_STATUS_IS_ENOTSOCK);
        APR_IS(97, APR_STATUS_IS_EINVAL);
    }
    return JNI_FALSE;
}
