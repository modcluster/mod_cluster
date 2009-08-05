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

#ifndef SIGHT_LOCAL_H
#define SIGHT_LOCAL_H

#include "apr.h"
#include "apr_general.h"
#include "apr_pools.h"
#include "apr_portable.h"
#include "apr_strings.h"
#include <jni.h>

#include "sight_types.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @file sight_local.h
 * @brief
 *
 * SIGHT Local functions and defines
 *
 */

#define SIGHT_TIMEUP      APR_OS_START_USERERR + 1
#define SIGHT_EAGAIN      APR_OS_START_USERERR + 2
#define SIGHT_EINTR       APR_OS_START_USERERR + 3
#define SIGHT_EINPROGRESS APR_OS_START_USERERR + 4
#define SIGHT_ETIMEDOUT   APR_OS_START_USERERR + 5
#define SIGHT_ENOMEM      APR_OS_START_USERERR + 6

#define SIGHT_MIN(a, b) ((a) < (b) ? (a) : (b))
#define SIGHT_MAX(a, b) ((a) > (b) ? (a) : (b))

/* Default buffer sizes */
#define SIGHT_SBUFFER_SIZ       512
#define SIGHT_SBUFFER_LEN       (SIGHT_SBUFFER_SIZ - 1)

#define SIGHT_MBUFFER_SIZ       1024
#define SIGHT_MBUFFER_LEN       (SIGHT_MBUFFER_SIZ - 1)

#define SIGHT_HBUFFER_SIZ       8192
#define SIGHT_HBUFFER_LEN       (SIGHT_HBUFFER_SIZ - 1)

#define SIGHT_KBS(N)    ((N) * 1024)
#define SIGHT_KB        (1024)
#define SIGHT_MB        (SIGHT_KB * SIGHT_KB)
#define SIGHT_GB        (SIGHT_MB * SIGHT_KB)
#define SIGHT_TB        (SIGHT_GB * SIGHT_KB)

#define SIGHT_BENDIAN   1
#define SIGHT_LENDIAN   0

#define POOL_CALLBACK   0
#define POOL_DESTROY    1
#define POOL_CLEAR      2


#define CSTR_TO_JSTRING(V)      (*_E)->NewStringUTF(_E, (char *)(V))
#define WSTR_TO_JSTRING(V)      (*_E)->NewString(_E, (jchar *)(V), (jsize)wcslen((jchar *)(V)))
#define ZSTR_TO_JSTRING(V, L)   (*_E)->NewString(_E, (jchar *)(V), (L))


#define RETURN_JCSTR(V) \
    if ((V)) return (*_E)->NewStringUTF((_E), (V));  \
    else return NULL

#define RETURN_JWSTR(V) \
    if ((V)) return (*_E)->NewString((_E), (V), wcslen((V)));  \
    else return NULL


/* Exception throw helper classes */
#define THROW_FMARK  __FILE__, __LINE__
#define THROW_NMARK  NULL, 0

#define SIGHT_ENOPOOL    1
#define SIGHT_ENOFILE    2
#define SIGHT_ENULL      3
#define SIGHT_ENOMUTEX   4
#define SIGHT_ENOPROC    5

#define SIGHT_LOG_EMERG  1
#define SIGHT_LOG_ERROR  2
#define SIGHT_LOG_NOTICE 3
#define SIGHT_LOG_WARN   4
#define SIGHT_LOG_INFO   5
#define SIGHT_LOG_DEBUG  6

char *sight_strerror(int);
void throwClass(JNIEnv *, const char *, const char *);
void throwDebug(JNIEnv *, const char *, const char *, int, const char *);
void throwException(JNIEnv *, const char *);
void throwIOException(JNIEnv *, const char *);
void throwMemoryException(JNIEnv *, const char *, int, const char *);
void throwNullPointerException(JNIEnv *, const char *, int, const char *);
void throwArgumentException(JNIEnv *, const char *, int, const char *);
void throwAprException(JNIEnv *, apr_status_t);
void throwAprExceptionEx(JNIEnv *, const char *, int, apr_status_t);
void throwAprIOException(JNIEnv *, apr_status_t);
void throwAprIOExceptionEx(JNIEnv *, const char *, int, apr_status_t);
void throwAprMemoryException(JNIEnv *, const char *, int, apr_status_t);
void throwOSException(JNIEnv *, const char *);

jint         sight_get_jnienv(JNIEnv **);
void         sight_clr_jnienv(jint);
jsize        sight_strparts(const char *);
jsize        sight_wcsparts(const jchar *);
char       **sight_szparray(apr_pool_t *, const char *, jsize *);
char       **sight_szarray(const char *, jsize *);
jsize        sight_wcslen(const jchar *);
char        *sight_trim(char *);
char        *sight_ltrim(char *);
char        *sight_rtrim(char *);
int          sight_islast(const char *, int);
int          sight_byteorder();
sight_str_t  sight_pstrdupw(apr_pool_t *, const jchar *);
sight_str_t  sight_strdupw(const jchar *);
char        *sight_strdupj(JNIEnv *, jstring);
char        *sight_pstrdupj(apr_pool_t *, JNIEnv *, jstring);
char        *sight_strup(char *);
void        *sight_malloc(JNIEnv *, apr_size_t, const char *, int);
void        *sight_realloc(JNIEnv *, void *, apr_size_t, const char *, int);
void        *sight_calloc(JNIEnv *, apr_size_t, const char *, int);
void        *sight_palloc(JNIEnv *, apr_pool_t *, apr_size_t,
                          const char *, int);
void        *sight_pcalloc(JNIEnv *, apr_pool_t *, apr_size_t,
                           const char *, int);
char        *sight_strdup(JNIEnv *, const char *,
                          const char *, int);
int          sight_wmatch(const char *, const char *);

/* Array prototypes */
sight_arr_t *sight_arr_new(jsize);
int          sight_arr_add(sight_arr_t *, const char *);
void         sight_arr_free(sight_arr_t *);
sight_arr_t *sight_arr_rload(const char *);
sight_arr_t *sight_arr_cload(const char *, const char *);
sight_arr_t *sight_arr_lload(const char *, int);

#define     SIGHT_FREE(x)  if ((x)) free((x))

/* jnu.c utils prototypes */
char        *sight_fread(const char *);
jlong        sight_strtoi64(const char *);
jint         sight_strtoi32(const char *);
char        *sight_strtok_c(char *, int, char **);

apr_table_t *sight_ftable(const char *, int, apr_pool_t *);

char        *sight_table_get_s(apr_table_t *, const char *);
jint         sight_table_get_i(apr_table_t *, const char *);
jlong        sight_table_get_j(apr_table_t *, const char *);
jlong        sight_table_get_x(apr_table_t *, const char *);
jdouble      sight_table_get_d(apr_table_t *, const char *);
jboolean     sight_table_get_z(apr_table_t *, const char *);
sight_str_t  sight_table_get_w(apr_table_t *, const char *);

char        *sight_table_get_sp(apr_table_t *, int, const char *);
jint         sight_table_get_ip(apr_table_t *, int, const char *);
jlong        sight_table_get_jp(apr_table_t *, int, const char *);
jlong        sight_table_get_xp(apr_table_t *, int, const char *);


/* String array helpers */
jobjectArray sight_mc_to_sa(JNIEnv *, const char *);
jobjectArray sight_mw_to_sa(JNIEnv *, const jchar *);
jobjectArray sight_ac_to_sa(JNIEnv *, const char **, jsize);
jobjectArray sight_aw_to_sa(JNIEnv *, const jchar **, jsize);

typedef int (sight_parse_callback_fn_t)(int rec, const char *line,
                                        void *user);

int          sight_fparse(const char *, apr_pool_t *, void *,
                          sight_parse_callback_fn_t *);

#define IS_JOBJECT_NULL(E, O)  \
        (!(O) || ((*(E))->IsSameObject((E), (O), NULL) == JNI_TRUE))

#define IS_JOBJECT_VALID(E, O)  \
        ((O) && !((*(E))->IsSameObject((E), (O), NULL) == JNI_TRUE))


apr_status_t sight_main(apr_pool_t *);


/* Load and refernce class */
int  sight_load_class(JNIEnv *, JAVA_C_ID *);
void sight_unload_class(JNIEnv *, JAVA_C_ID *);
int  sight_load_classes(JNIEnv *);
void sight_unload_classes(JNIEnv *);

#define SIGHT_CLASS_LDEF(CL)  \
    int sight_class_##CL##_load(JNIEnv *_E)

#define SIGHT_CLASS_UDEF(CL)  \
    void sight_class_##CL##_unload(JNIEnv *_E)

#define SIGHT_CLASS_LDEC(CL)  \
    extern int sight_class_##CL##_load(JNIEnv *);   \
    extern int sight_class_##CL##_unload(JNIEnv *)

#define SIGHT_CLASS_LCAL(CL)  \
    if (sight_class_##CL##_load(_E)) return 1;


#define SIGHT_CLASS_UCAL(CL)  \
    sight_class_##CL##_unload(_E)

#define J_DECLARE_CLAZZ   static JAVA_C_ID _clazzn
#define J_DECLARE_F_ID(I) static JAVA_F_ID _f##I##n
#define J_DECLARE_M_ID(I) static JAVA_M_ID _m##I##n

#define J_LOAD_METHOD(I)    \
    if (_m##I##n.i == NULL) {                                               \
        _m##I##n.i = (*_E)->GetMethodID(_E, _clazzn.i, _m##I##n.n,          \
                                        _m##I##n.s);                        \
        if ((*_E)->ExceptionCheck(_E) || _m##I##n.i == NULL) {              \
            return 1;                                                       \
        }                                                                   \
    } else (void)(0)

#define J_LOAD_MLOCAL(I)    \
    if (_m##I##n.i == NULL) {                                               \
        _m##I##n.i = (*_E)->GetStaticMethodID(_E, _clazzn.i, _m##I##n.n,    \
                                             _m##I##n.s);                   \
        if ((*_E)->ExceptionCheck(_E) || _m##I##.i == NULL) {               \
            return 1;                                                       \
        }                                                                   \
    } else (void)(0)

#define J_LOAD_IFIELD(I)   \
    if (_f##I##n.i == NULL) {                                               \
        _f##I##n.i = (*_E)->GetFieldID(_E, _clazzn.i, _f##I##n.n,           \
                                       _f##I##n.s);                         \
        if ((*_E)->ExceptionCheck(_E) || _f##I##n.i == NULL) {              \
            return 1;                                                       \
        }                                                                   \
    } else (void)(0)

#define J_LOAD_SFIELD(I)   \
    if (_f##I##n.i == NULL) {                                               \
        _f##I##n.i = (*_E)->GetStaticFieldID(_E, _clazzn.i, _f##I##n.n,     \
                                            _f##I##n.s);                    \
        if ((*_E)->ExceptionCheck(_E) || _f##I##n.i == NULL) {              \
            return 1;                                                       \
        }                                                                   \
    } else (void)(0)


#define SET_IFIELD_J(I, O, V)  \
    if (_f##I##n.i) {                                                       \
        (*_E)->SetLongField(_E, (O), _f##I##n.i, (jlong)(V));               \
    } else (void)(0)

#define SET_IFIELD_I(I, O, V)  \
    if (_f##I##n.i) {                                                       \
        (*_E)->SetIntField(_E, (O), _f##I##n.i, (jint)(V));                 \
    } else (void)(0)

#define SET_IFIELD_D(I, O, V)  \
    if (_f##I##n.i) {                                                       \
        (*_E)->SetDoubleField(_E, (O), _f##I##n.i, (jdouble)(V));           \
    } else (void)(0)

#define SET_IFIELD_Z(I, O, V)  \
    if (_f##I##n.i) {                                                       \
        if ((V)) (*_E)->SetBooleanField(_E, (O), _f##I##n.i, JNI_TRUE);     \
        else     (*_E)->SetBooleanField(_E, (O), _f##I##n.i, JNI_FALSE);    \
    } else (void)(0)

#define SET_IFIELD_S(I, O, V)  \
    if (_f##I##n.i && (V)) {                                                \
        jstring _str = (*_E)->NewStringUTF(_E, (V));                        \
        (*_E)->SetObjectField(_E, (O), _f##I##n.i, _str);                   \
        (*_E)->DeleteLocalRef(_E, _str);                                    \
    } else (void)(0)

#define SET_IFIELD_N(I, O, V)  \
    if (_f##I##n.i && (V) && *(V)) {                                        \
        jstring _str = (*_E)->NewStringUTF(_E, (V));                        \
        (*_E)->SetObjectField(_E, (O), _f##I##n.i, _str);                   \
        (*_E)->DeleteLocalRef(_E, _str);                                    \
    } else (void)(0)

#define SET_IFIELD_W(I, O, V)  \
    if (_f##I##n.i && (V)) {                                                \
        jstring _str = (*_E)->NewString(_E, (V), (jsize)wcslen((V)));       \
        (*_E)->SetObjectField(_E, (O), _f##I##n.i, _str);                   \
        (*_E)->DeleteLocalRef(_E, _str);                                    \
    } else (void)(0)

#define SET_IFIELD_O(I, O, V)  \
    if (_f##I##n.i) {                                                       \
        (*_E)->SetObjectField(_E, (O), _f##I##n.i, (jobject)(V));           \
    } else (void)(0)

#define SET_SFIELD_Z(I, V)  \
    if (_f##I##n.i) {                                                                   \
        if ((V)) (*_E)->SetStaticBooleanField(_E, _clazzn.i, _f##I##n.i, JNI_TRUE);     \
        else     (*_E)->SetStaticBooleanField(_E, _clazzn.i, _f##I##n.i, JNI_FALSE);    \
    } else (void)(0)


#define CALL_METHOD1(I, O, V)  \
    if (_m##I##n.i) {                                                       \
        (*_E)->CallVoidMethod(_E, (O), _m##I##n.i, (V), NULL);              \
    } else (void)(0)

#define CALL_METHOD2(I, O, V, X)  \
    if (_m##I##n.i) {                                                       \
        (*_E)->CallVoidMethod(_E, (O), _m##I##n.i, (V), (X), NULL);         \
    } else (void)(0)

#define CALL_METHOD3(I, O, V, X, Y)  \
    if (_m##I##n.i) {                                                       \
        (*_E)->CallVoidMethod(_E, (O), _m##I##n.i, (V), (X), (Y), NULL);    \
    } else (void)(0)


/* Standard Java classes */
typedef enum {
    SIGHT_CC_OBJECT,
    SIGHT_CC_STRING,
    SIGHT_CC_ZARRAY,
    SIGHT_CC_BARRAY,
    SIGHT_CC_CARRAY,
    SIGHT_CC_IARRAY,
    SIGHT_CC_JARRAY,
    SIGHT_CC_TARRAY,
    SIGHT_CC_OARRAY,
    SIGHT_CC_MAX
} sight_cclass_e;

typedef enum {
    SIGHT_PROC_U, /* Unknown */
    SIGHT_PROC_R, /* Running or runnable (on run queue) */
    SIGHT_PROC_S, /* Interruptible sleep (waiting for an event to complete) */
    SIGHT_PROC_D, /*  Uninterruptible sleep (usually IO) */
    SIGHT_PROC_Z, /* Defunct ("zombie") process, terminated but not reaped by its parent */
    SIGHT_PROC_T, /* Stopped, either by a job control signal or because it is being traced. */
    SIGHT_PROC_W, /* paging */
    SIGHT_PROC_X  /* dead */
    /* XXX : BSD as more */
} sight_procstate_e;

apr_status_t sight_pool_create(apr_pool_t **, apr_thread_mutex_t **,
                               apr_pool_t *, int);
apr_status_t sight_pool_clear(apr_pool_t *);
apr_status_t sight_pool_destroy(apr_pool_t *);
apr_status_t sight_pool_lock(apr_thread_mutex_t **, apr_pool_t *);
apr_status_t sight_pool_lock_parent(apr_thread_mutex_t **, apr_pool_t *);

jobjectArray sight_new_cc_array(JNIEnv *, sight_cclass_e, jsize);
jbyteArray   sight_new_byte_array(JNIEnv *, jbyte *, jsize);

/* Atomic operations */
extern volatile apr_uint32_t *sight_global_atomic;
apr_uint64_t sight_atomic_inc64();

#if defined(DEBUG) || defined(_DEBUG)
/* In DEBUG mode always use statistics */
#ifndef SIGHT_DO_STATS
#define SIGHT_DO_STATS 1
#endif
#endif

#if SIGHT_APR_REFCOUNT
#define SIGHT_GLOBAL_TRY   if (apr_atomic_inc32(sight_global_atomic))
#define SIGHT_GLOBAL_END() apr_atomic_dec32(sight_global_atomic)
#define SIGHT_GLOBAL_BRK() apr_atomic_dec32(sight_global_atomic)

/* More then 256 references is insane */
#define SIGHT_MAXLREF      256

#define SIGHT_LOCAL_TRY(N) if ((N) && apr_atomic_inc32(&(N)->refcount) < SIGHT_MAXLREF) { \
                               if (apr_atomic_read32(&(N)->interrupted) == 0)

#define SIGHT_LOCAL_END(N) } if ((N)) apr_atomic_dec32(&(N)->refcount)
#define SIGHT_LOCAL_BRK(N) apr_atomic_dec32(&(N)->refcount)

#else
#define SIGHT_GLOBAL_TRY
#define SIGHT_GLOBAL_END()  (void)0
#define SIGHT_GLOBAL_BRK()  (void)0
#define SIGHT_LOCAL_TRY(N)  if ((N))
#define SIGHT_LOCAL_END(N)  (void)0
#define SIGHT_LOCAL_BRK(N)  (void)0

#endif /* SIGHT_APR_REFCOUNT */

#define SIGHT_LOCAL_IRQ(N) apr_atomic_read32(&(N)->interrupted)

/* Statistics counters */
extern volatile apr_uint64_t sight_cnt_native_alloc;
extern volatile apr_uint64_t sight_cnt_native_create;
extern volatile apr_uint64_t sight_cnt_native_free;
extern volatile apr_uint64_t sight_cnt_native_destroyed;
extern volatile apr_uint64_t sight_cnt_native_pcleanup;
extern volatile apr_uint64_t sight_cnt_native_clear;
extern volatile apr_uint64_t sight_cnt_native_cleared;
extern volatile apr_uint64_t sight_cnt_native_clrcall;

extern void     dbprintf(char *format, ...);

#ifdef SIGHT_DO_TRACE
#define DBPRINTF    dbprintf
#else
#define DBPRINTF    //

#endif

/* Prototypes from cache.c */
cache_table_t *cache_new(size_t);
cache_entry_t *cache_add(cache_table_t *, const char *);
cache_entry_t *cache_find(cache_table_t *, const char *);
void           cache_free(cache_table_t *, void (*destroy)(const char *, void *));

/* Prototypes from finfo.c */
jobject      sight_new_finfo_class(SIGHT_STDARGS);
void         sight_finfo_fill(SIGHT_STDARGS, apr_finfo_t *);

/* Prototypes from  netaddr.c */

#ifndef AF_MAX
#define AF_MAX      (AF_INET6 + 5)
#endif
#ifndef AF_LOCAL
#ifdef  AF_UNIX
#define AF_LOCAL    AF_UNIX
#else
#define AF_LOCAL    (AF_MAX + 1)
#endif
#endif
#ifndef AF_HARDWARE
#define AF_HARDWARE (AF_MAX + 2)
#endif

const char  *sight_inet_ntop4(const unsigned char *, char *, size_t);
const char  *sight_inet_ntop6(const unsigned char *, char *, size_t);
int          sight_hex2bin(const char *, unsigned char *, size_t);

jobject      sight_new_netaddr_class(SIGHT_STDARGS);
jobjectArray sight_new_netaddr_array(SIGHT_STDARGS, jsize);
void         sight_netaddr_set_host(SIGHT_STDARGS, const char *);
void         sight_netaddr_set_serv(SIGHT_STDARGS, const char *);
void         sight_netaddr_set_addr(SIGHT_STDARGS, const char *);
void         sight_netaddr_set_mask(SIGHT_STDARGS, const char *);
void         sight_netaddr_set_llt(SIGHT_STDARGS, jlong);
void         sight_netaddr_set_port(SIGHT_STDARGS, jint);
void         sight_netaddr_set_family(SIGHT_STDARGS, jint);

int          sight_get_fs_type(const char *);

/* Prototypes from  tcpconn.c */
jobject      sight_new_tcpconn_class(SIGHT_STDARGS);
jobjectArray sight_new_tcpconn_array(SIGHT_STDARGS, jsize);
void         sight_tcpconn_set_local(SIGHT_STDARGS, jobject);
void         sight_tcpconn_set_remote(SIGHT_STDARGS, jobject);
void         sight_tcpconn_set_pid(SIGHT_STDARGS, jint);
void         sight_tcpconn_set_cts(SIGHT_STDARGS, jlong);
void         sight_tcpconn_set_tmo(SIGHT_STDARGS, jint);
void         sight_tcpconn_set_state(SIGHT_STDARGS, jint);

/* Prototypes from  udpconn.c */
jobject      sight_new_udpconn_class(SIGHT_STDARGS);
jobjectArray sight_new_udpconn_array(SIGHT_STDARGS, jsize);
void         sight_udpconn_set_local(SIGHT_STDARGS, jobject);
void         sight_udpconn_set_remote(SIGHT_STDARGS, jobject);
void         sight_udpconn_set_pid(SIGHT_STDARGS, jint);
void         sight_udpconn_set_cts(SIGHT_STDARGS, jlong);
void         sight_udpconn_set_tmo(SIGHT_STDARGS, jint);
void         sight_udpconn_set_state(SIGHT_STDARGS, jint);


#ifdef __cplusplus
}
#endif

#endif /* SIGHT_LOCAL_H */
