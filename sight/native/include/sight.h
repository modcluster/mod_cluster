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

#ifndef SIGHT_H
#define SIGHT_H

#if (defined WIN32)
#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0500
#endif
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif

#include <windows.h>
#if defined(_M_IA64)
/* Fix bug in PlatforSDK headers */
#define TMP_IA64 _M_IA64
#undef _M_IA64
#include <winternl.h>
#define _M_IA64 TMP_IA64
#else
#include <winternl.h>
#endif
#include <winsock2.h>
#include <mswsock.h>
#include <ws2tcpip.h>
#include <psapi.h>
#include <tlhelp32.h>
#include <aclapi.h>
#include <lm.h>
#include <sddl.h>
#include <iphlpapi.h>
#include <userenv.h>
#include <shellapi.h>
#endif

#include "apr.h"
#include "apr_general.h"
#include "apr_lib.h"
#include "apr_atomic.h"
#include "apr_pools.h"
#include "apr_portable.h"
#include "apr_network_io.h"
#include "apr_strings.h"
#include "apr_file_io.h"
#include "apr_file_info.h"

#ifndef APR_HAS_THREADS
#error "Missing APR_HAS_THREADS support from APR."
#endif

#include "apr_thread_mutex.h"

#if APR_HAVE_STDLIB_H
#include <stdlib.h>
#endif
#if APR_HAVE_STDIO_H
#include <stdio.h>
#endif

#include <jni.h>

/**
 * SIGHT_DECLARE_EXPORT is defined when building the ANNEX dynamic library,
 * so that all public symbols are exported.
 *
 * SIGHT_DECLARE_STATIC is defined when including the TCN public headers,
 * to provide static linkage when the dynamic library may be unavailable.
 *
 * SIGHT_DECLARE_STATIC and SIGHT_DECLARE_EXPORT are left undefined when
 * including the TCN public headers, to import and link the symbols from
 * the dynamic TCN library and assure appropriate indirection and calling
 * conventions at compile time.
 */

#if !defined(WIN32)
/**
 * The public TCN functions are declared with SIGHT_DECLARE(), so they may
 * use the most appropriate calling convention.  Public APR functions with
 * variable arguments must use SIGHT_DECLARE_NONSTD().
 *
 * @deffunc SIGHT_DECLARE(rettype) apr_func(args);
 */
#define SIGHT_DECLARE(type)            type
/**
 * The public TCN functions using variable arguments are declared with
 * SIGHT_DECLARE_NONSTD(), as they must use the C language calling convention.
 *
 * @deffunc SIGHT_DECLARE_NONSTD(rettype) apr_func(args, ...);
 */
#define SIGHT_DECLARE_NONSTD(type)     type
/**
 * The public TCN variables are declared with SIGHT_DECLARE_DATA.
 * This assures the appropriate indirection is invoked at compile time.
 *
 * @deffunc SIGHT_DECLARE_DATA type apr_variable;
 * @tip extern SIGHT_DECLARE_DATA type apr_variable; syntax is required for
 * declarations within headers to properly import the variable.
 */
#define SIGHT_DECLARE_DATA
#elif defined(SIGHT_DECLARE_STATIC)
#define SIGHT_DECLARE(type)            type __stdcall
#define SIGHT_DECLARE_NONSTD(type)     type
#define SIGHT_DECLARE_DATA
#elif defined(SIGHT_DECLARE_EXPORT)
#define SIGHT_DECLARE(type)            __declspec(dllexport) type __stdcall
#define SIGHT_DECLARE_NONSTD(type)     __declspec(dllexport) type
#define SIGHT_DECLARE_DATA             __declspec(dllexport)
#else
/**
 * The public TCN functions are declared with SIGHT_DECLARE(), so they may
 * use the most appropriate calling convention.  Public APR functions with
 * variable arguments must use SIGHT_DECLARE_NONSTD().
 *
 */
#define SIGHT_DECLARE(type)            __declspec(dllimport) type __stdcall
/**
 * The public TCN functions using variable arguments are declared with
 * SIGHT_DECLARE_NONSTD(), as they must use the C language calling convention.
 *
 */
#define SIGHT_DECLARE_NONSTD(type)     __declspec(dllimport) type
/**
 * The public TCN variables are declared with SIGHT_DECLARE_DATA.
 * This assures the appropriate indirection is invoked at compile time.
 *
 * @remark extern SIGHT_DECLARE_DATA type apr_variable; syntax is required for
 * declarations within headers to properly import the variable.
 */
#define SIGHT_DECLARE_DATA             __declspec(dllimport)
#endif

#if !defined(WIN32) || defined(SIGHT_MODULE_DECLARE_STATIC)
/**
 * Declare a dso module's exported module structure as SIGHT_MODULE_DECLARE_DATA.
 *
 * Unless SIGHT_MODULE_DECLARE_STATIC is defined at compile time, symbols
 * declared with SIGHT_MODULE_DECLARE_DATA are always exported.
 * @code
 * module SIGHT_MODULE_DECLARE_DATA mod_tag
 * @endcode
 */
#if defined(WIN32)
#define SIGHT_MODULE_DECLARE(type)            type __stdcall
#else
#define SIGHT_MODULE_DECLARE(type)            type
#endif
#define SIGHT_MODULE_DECLARE_NONSTD(type)     type
#define SIGHT_MODULE_DECLARE_DATA
#else
/**
 * SIGHT_MODULE_DECLARE_EXPORT is a no-op.  Unless contradicted by the
 * SIGHT_MODULE_DECLARE_STATIC compile-time symbol, it is assumed and defined.
 */
#define SIGHT_MODULE_DECLARE_EXPORT
#define SIGHT_MODULE_DECLARE(type)          __declspec(dllexport) type __stdcall
#define SIGHT_MODULE_DECLARE_NONSTD(type)   __declspec(dllexport) type
#define SIGHT_MODULE_DECLARE_DATA           __declspec(dllexport)
#endif

#define SIGHT_OS_UNIX                       1
#define SIGHT_OS_WINDOWS                    2
#define SIGHT_OS_WIN64                      3
#define SIGHT_OS_WOW64                      4
#define SIGHT_OS_LINUX                      5
#define SIGHT_OS_SOLARIS                    6
#define SIGHT_OS_BSD                        7

#define SIGHT_CLASS_PATH                    "org/jboss/sight/"
#define SIGHT_NATIVE_CLASS_PATH             SIGHT_CLASS_PATH "share/"
#define UNREFERENCED(P)                     (P) = (P)
#define UNREFERENCED_STDARGS                _E = _E; _O = _O
#define UNREFERENCED_O                      _O = _O
#ifdef WIN32
#define LLT(X)                              (X)
#else
#define LLT(X)                              ((long)(X))
#endif
#define P2J(P)                              ((jlong)LLT(P))
#define J2P(P, T)                           ((T)LLT((jlong)P))
#define V2P(T, V)                           ((T)((T)0 + (V)))
#define V2Z(V)                              ((V) ? JNI_TRUE : JNI_FALSE)

#define SIGHT_STDARGS                       JNIEnv *_E, jobject _O


/* SIGHT_ALIGN() is only to be used to align on a power of 2 boundary */
#define SIGHT_ALIGN(size, boundary) \
    (((size) + ((boundary) - 1)) & ~((boundary) - 1))

/** Default alignment */
#define SIGHT_ALIGN_DEFAULT(size) SIGHT_ALIGN(size, 8)


#define SIGHT_EXPORT_DECLARE(RT, CL, FN)  \
    JNIEXPORT RT JNICALL Java_org_jboss_sight_##CL##_##FN

#define SIGHT_EXPORT_GET(RT, CL, FN)  \
    JNIEXPORT RT JNICALL Java_org_jboss_sight_##CL##_get##FN
#define SIGHT_EXPORT_GETI(CL, FN)  \
    JNIEXPORT jint JNICALL Java_org_jboss_sight_##CL##_get##FN
#define SIGHT_EXPORT_GETJ(CL, FN)  \
    JNIEXPORT jlong JNICALL Java_org_jboss_sight_##CL##_get##FN
#define SIGHT_EXPORT_GETS(CL, FN)  \
    JNIEXPORT jstring JNICALL Java_org_jboss_sight_##CL##_get##FN
#define SIGHT_EXPORT_GETD(CL, FN)  \
    JNIEXPORT jdouble JNICALL Java_org_jboss_sight_##CL##_get##FN
#define SIGHT_EXPORT_GETZ(CL, FN)  \
    JNIEXPORT jboolean JNICALL Java_org_jboss_sight_##CL##_get##FN

#define J2S(V)                              _c##V
#define J2W(V)                              _w##V
#define J2C(V)                              _wc##V
#define JWL(V)                              _wl##V
#define J2T(T)                              (apr_time_t)((T))
#define SIGHT_CC                            const char
#define SIGHT_WC                            const jchar
#define SIGHT_BEGIN_MACRO                   if (1) {
#define SIGHT_END_MACRO                     } else (void)(0)

#define SIGHT_ALLOC_CSTRING(V)     \
    SIGHT_CC *_c##V = V ? (SIGHT_CC *)((*_E)->GetStringUTFChars(_E, V, 0)) : NULL

#define SIGHT_INIT_CSTRING(V)

#define SIGHT_FREE_CSTRING(V)      \
    if (_c##V) (*_E)->ReleaseStringUTFChars(_E, V, _c##V)

#define SIGHT_ALLOC_WSTRING(V)                                                  \
    jsize _wl##V = V ? (*_E)->GetStringLength(_E, V) : 0;                       \
    SIGHT_WC *_w##V = V ? (SIGHT_WC *)((*_E)->GetStringChars(_E, V, 0)) : NULL; \
    jchar *_wc##V = NULL

#define SIGHT_INIT_WSTRING(V)                                                   \
        if (_w##V) {                                                            \
            _wc##V = (jchar *)malloc((_wl##V + 1) * sizeof(jchar));             \
            if (_wc##V) {                                                       \
                if (_wl##V)                                                     \
                    wcsncpy(_wc##V, _w##V, _wl##V);                             \
                _wc##V[_wl##V] = 0;                                             \
            }                                                                   \
        } else (void)(0)

#define SIGHT_FREE_WSTRING(V)                           \
    if (_w##V) (*_E)->ReleaseStringChars(_E, V, _w##V); \
    if (_wc##V) free (_wc##V)

#if defined(_DEBUG) || defined(DEBUG)
#include <assert.h>
#define SIGHT_ASSERT(x)  assert((x))
#else
#define SIGHT_ASSERT(x) (void)0
#endif


#ifdef __cplusplus
extern "C" {
#endif

/**
 * @file sight.h
 * @brief
 *
 * SIGHT Public API
 */

#ifdef __cplusplus
}
#endif

#endif /* SIGHT_H */
