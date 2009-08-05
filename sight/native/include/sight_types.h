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

#ifndef SIGHT_TYPES_H
#define SIGHT_TYPES_H

#include "apr.h"
#include "apr_general.h"
#include "apr_lib.h"
#include "apr_pools.h"
#include "apr_time.h"
#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @file sight_types.h
 * @brief
 *
 * SIGHT Local structures
 *
 */

/* Default buffer sizes */
#define SIGHT_STYPE_SIZ       512
#define SIGHT_STYPE_LEN       (SIGHT_STYPE_SIZ - 1)

#define SIGHT_MTYPE_SIZ       1024
#define SIGHT_MTYPE_LEN       (SIGHT_MTYPE_SIZ - 1)

#define SIGHT_HTYPE_SIZ       8192
#define SIGHT_HTYPE_LEN       (SIGHT_HTYPE_SIZ - 1)

#define SIGHT_MAX_PROCESSES   65536

/* For now use APR reference counters */
#define SIGHT_APR_REFCOUNT 1

typedef struct JAVA_C_ID {
    jclass      i;
    jclass      a;
    const char  *n;
} JAVA_C_ID;

typedef struct JAVA_M_ID {
    jmethodID    i;
    const char  *n;
    const char  *s;
} JAVA_M_ID;

typedef struct JAVA_F_ID {
    jfieldID    i;
    const char  *n;
    const char  *s;
} JAVA_F_ID;

typedef struct sight_callback_t {
    jobject     object;
    jmethodID   method;
    jint        counter;
    const char  *name;
    const char  *msig;
    void        *opaque;
} sight_callback_t;

#define JSSIZE_MAX  1048576

typedef struct sight_str_t {
    jsize len;
    union {
        char  *c;
        jchar *w;
    } str;
} sight_str_t;

#define SIGHT_ARR_CCHAR  0x01000000
#define SIGHT_ARR_WCHAR  0x02000000
#define SIGHT_ARR_LMASK  0x00FFFFFF

typedef struct sight_arr_t {
    jsize siz;
    jsize len;
    char  **arr;
} sight_arr_t;

typedef struct sight_object_t sight_object_t;

/* org.jboss.sight.NativeObject instance */
struct sight_object_t {
#if SIGHT_APR_REFCOUNT
    volatile apr_uint32_t refcount;
#endif
    volatile apr_uint32_t interrupted;
    apr_pool_t           *pool;
    apr_thread_mutex_t   *mutex;
    void                 *native;
    void                 *opaque;
    jobject               object;
    jmethodID             destroy;
    sight_callback_t      cb;
    void                  (*clean)(int, sight_object_t *);
};

#define        CACHE_HASH_MASK        255
#define        CACHE_HASH_SIZE        256

typedef struct cache_entry_t cache_entry_t;
typedef struct cache_table_t cache_table_t;

struct cache_entry_t {
    cache_entry_t *next;                 /*  Next cache entry in bucket       */
    char           *key;                 /*  cache id                         */
    void           *data;
};

struct cache_table_t {
    /* Pointer to the linked list of cache entries */
    cache_entry_t  **list;
    /* Table of hash buckets            */
    cache_entry_t  *hash[CACHE_HASH_SIZE];
    /* Number of cache buckets defined  */
    size_t          siz;
    /* Maximum number of cache buckets  */
    size_t          len;
};


#define SIGHT_FS_UNKNOWN        0
#define SIGHT_FS_MSDOS          1
#define SIGHT_FS_VFAT           2
#define SIGHT_FS_NTFS           3
#define SIGHT_FS_ISO9660        4
#define SIGHT_FS_EXT2           5
#define SIGHT_FS_EXT3           6
#define SIGHT_FS_XFS            7
#define SIGHT_FS_XIAFS          8
#define SIGHT_FS_HPFS           9
#define SIGHT_FS_HFS           10
#define SIGHT_FS_JFS           11
#define SIGHT_FS_ROMFS         12
#define SIGHT_FS_UDF           13
#define SIGHT_FS_FFS           14
#define SIGHT_FS_SFS           15
#define SIGHT_FS_NFS           16
#define SIGHT_FS_RAMFS         17
#define SIGHT_FS_RAISERFS      18
#define SIGHT_FS_DEV           19
#define SIGHT_FS_PROC          20
#define SIGHT_FS_SYSFS         21
#define SIGHT_FS_TMPFS         22
#define SIGHT_FS_RPC           23
#define SIGHT_FS_USBFS         24
#define SIGHT_FS_VMHGFS        25
#define SIGHT_FS_VMBLOCK       26
#define SIGHT_FS_SWAP          27
#define SIGHT_FS_NONE          99

#define SIGHT_CASE_SENSITIVE_SEARCH      0x00000001
#define SIGHT_CASE_PRESERVED_NAMES       0x00000002
#define SIGHT_UNICODE_ON_DISK            0x00000004
#define SIGHT_FILE_COMPRESSION           0x00000010
#define SIGHT_VOLUME_QUOTAS              0x00000020
#define SIGHT_SUPPORTS_SPARSE_FILES      0x00000040
#define SIGHT_SUPPORTS_REPARSE_POINTS    0x00000080
#define SIGHT_VOLUME_IS_COMPRESSED       0x00008000
#define SIGHT_SUPPORTS_ENCRYPTION        0x00020000
#define SIGHT_READ_ONLY_VOLUME           0x00080000

/* Unix specific options
 * XXX: This needs to be revised if Windows changes
 * the API for  FILE_ and FS_
 */
#define SIGHT_READ_WRITE_VOLUME          0x01000000
#define SIGHT_SUID_VOLUME                0x02000000


#define SIGHT_DRIVE_UNKNOWN    -1
#define SIGHT_DRIVE_NONE        0
#define SIGHT_DRIVE_INVALID     1
#define SIGHT_DRIVE_REMOVABLE   2
#define SIGHT_DRIVE_FIXED       3
#define SIGHT_DRIVE_REMOTE      4
#define SIGHT_DRIVE_CDROM       5
#define SIGHT_DRIVE_RAMDISK     6
#define SIGHT_DRIVE_SWAP        7

/* Socket TCP states */
#define SIGHT_TCP_CLOSED        1
#define SIGHT_TCP_LISTENING     2
#define SIGHT_TCP_SYN_SENT      3
#define SIGHT_TCP_SYN_RCVD      4
#define SIGHT_TCP_ESTABLISHED   5
#define SIGHT_TCP_FIN_WAIT1     6
#define SIGHT_TCP_FIN_WAIT2     7
#define SIGHT_TCP_CLOSE_WAIT    8
#define SIGHT_TCP_CLOSING       9
#define SIGHT_TCP_LAST_ACK     10
#define SIGHT_TCP_TIME_WAIT    11
#define SIGHT_TCP_DELETE_TCB   12

#define SIGHT_IFO_UP            1
#define SIGHT_IFO_DOWN          2
#define SIGHT_IFO_TESTING       3
#define SIGHT_IFO_UNKNOWN       4
#define SIGHT_IFO_DORMANT       5
#define SIGHT_IFO_NOTPRESENT    6
#define SIGHT_IFO_LLDOWN        7

#ifdef __cplusplus
}
#endif

#endif /* SIGHT_TYPES_H */
