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

#define SIGHT_WANT_LATE_DLL
#include "sight_private.h"

#include <unistd.h>

#include <sys/mnttab.h>
#include <sys/statfs.h>
#include <sys/statvfs.h>

#include <sys/stat.h>
#include <sys/swap.h>

/*
 * Network adapter implementation
 */

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "Volume"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "Name",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "Description",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "MountPoint",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0003) = {
    NULL,
    "SectorsPerCluster",
    "I"
};

J_DECLARE_F_ID(0004) = {
    NULL,
    "BytesPerSector",
    "I"
};

J_DECLARE_F_ID(0005) = {
    NULL,
    "FreeBytesAvailable",
    "J"
};

J_DECLARE_F_ID(0006) = {
    NULL,
    "TotalNumberOfBytes",
    "J"
};

J_DECLARE_F_ID(0007) = {
    NULL,
    "TotalNumberOfFreeBytes",
    "J"
};

J_DECLARE_F_ID(0008) = {
    NULL,
    "NumberOfMountPoints",
    "I"
};

J_DECLARE_M_ID(0000) = {
    NULL,
    "setFSType",
    "(I)V"
};

J_DECLARE_M_ID(0001) = {
    NULL,
    "setFlags",
    "(I)V"
};

J_DECLARE_M_ID(0002) = {
    NULL,
    "setDType",
    "(I)V"
};


SIGHT_CLASS_LDEF(Volume)
{
    if (sight_load_class(_E, &_clazzn))
        return 1;
    J_LOAD_IFIELD(0000);
    J_LOAD_IFIELD(0001);
    J_LOAD_IFIELD(0002);
    J_LOAD_IFIELD(0003);
    J_LOAD_IFIELD(0004);
    J_LOAD_IFIELD(0005);
    J_LOAD_IFIELD(0006);
    J_LOAD_IFIELD(0007);
    J_LOAD_IFIELD(0008);
    J_LOAD_METHOD(0000);
    J_LOAD_METHOD(0001);
    J_LOAD_METHOD(0002);

    return 0;
}

SIGHT_CLASS_UDEF(Volume)
{
    sight_unload_class(_E, &_clazzn);
}

extern apr_pool_t *sight_temp_pool;

typedef struct volume_enum_t {
    apr_pool_t *pool;
    int         mounts_idx;
    int         num_mounts;
    struct sight_mnttab *ent;
    int         swap_idx;
    int         num_swap;
    swaptbl_t * swap_table;
} volume_enum_t;

#define MAXSTRSIZE 80
#define MNTOPT_RW "write"
#define MNTOPT_RO "read only"
#define MNTOPT_SUID "setuid"

struct sight_mnttab {
    struct mnttab ent;
    struct sight_mnttab *next;
};

/* Initialize volume enumeration */
SIGHT_EXPORT_DECLARE(jlong, Volume, enum0)(SIGHT_STDARGS,
                                           jlong pool)
{
    volume_enum_t *e;
    apr_status_t rc;

    UNREFERENCED_O;
    if (!(e = (volume_enum_t *)sight_calloc(_E,
                               sizeof(volume_enum_t),
                               THROW_FMARK))) {
        return 0;
    }
    if ((rc = sight_pool_create(&e->pool, NULL, sight_temp_pool, 0)) != APR_SUCCESS) {
        throwAprMemoryException(_E, THROW_FMARK, rc);
        return 0;
    }
    return P2J(e);
}

SIGHT_EXPORT_DECLARE(jint, Volume, enum1)(SIGHT_STDARGS,
                                          jlong handle)
{
    struct sight_mnttab *ent;
    struct sight_mnttab *previous;
    char *buf;
    int i;
    apr_status_t rc;
    struct mnttab mp;
    volume_enum_t *e = J2P(handle, volume_enum_t *);
    FILE *fp = fopen(MNTTAB, "r");

    UNREFERENCED_O;

    if (!e)
        return 0;
    if (fp == NULL) {
        throwAprException(_E, apr_get_os_error());
        return 0;
    }
    e->num_mounts = 0;
    ent = (struct sight_mnttab *) apr_palloc(e->pool, sizeof(struct sight_mnttab));
    ent->next = NULL;
    e->ent = ent;
    previous = ent;
    while (getmntent(fp, &mp) == 0) {
        ent->ent.mnt_special = apr_pstrdup(e->pool, mp.mnt_special);
        ent->ent.mnt_mountp = apr_pstrdup(e->pool, mp.mnt_mountp);
        ent->ent.mnt_fstype = apr_pstrdup(e->pool, mp.mnt_fstype);
        ent->ent.mnt_mntopts = apr_pstrdup(e->pool, mp.mnt_mntopts);
        ent->ent.mnt_time = apr_pstrdup(e->pool, mp.mnt_time);

        e->num_mounts++;
        previous = ent;
        ent->next = (struct sight_mnttab *) apr_palloc(e->pool, sizeof(struct sight_mnttab));
        ent = ent->next;
        ent->next = NULL;
    }
    fclose(fp);
    previous->next = NULL;

    ent = e->ent;
    while (ent != NULL) {
        ent = ent->next;
    }

    /* process swap */
    e->num_swap = swapctl(SC_GETNSWP, 0);
    e->swap_table = apr_palloc(e->pool, sizeof(swapent_t) * e->num_swap + sizeof(swaptbl_t));
    buf = apr_palloc(e->pool, MAXSTRSIZE * e->num_swap + 1);
    for (i = 0; i < e->num_swap + 1; i++) {
        e->swap_table->swt_ent[i].ste_path = buf + (i * MAXSTRSIZE);
    }
    e->swap_table->swt_n = e->num_swap;
    e->num_swap = swapctl(SC_LIST, e->swap_table);
    e->num_mounts = e->num_mounts + e->num_swap;

    e->swap_idx = 0;
    e->mounts_idx = 0;

    return e->num_mounts;
}

SIGHT_EXPORT_DECLARE(void, Volume, enum2)(SIGHT_STDARGS,
                                          jobject thiz,
                                          jlong handle)
{
    struct mnttab ent;
    struct statvfs sv;
    char buf[SIGHT_MBUFFER_SIZ];
    volume_enum_t *e = J2P(handle, volume_enum_t *);

    UNREFERENCED_O;

    if (!e || !thiz)
        return;
    if (e->swap_idx < e->num_swap) {
        int size = getpagesize();

        SET_IFIELD_S(0000, thiz, e->swap_table->swt_ent[e->swap_idx].ste_path);
        SET_IFIELD_S(0001, thiz, "partition");
        SET_IFIELD_S(0002, thiz, "swap");
        CALL_METHOD1(0000, thiz, SIGHT_FS_SWAP);
        CALL_METHOD1(0001, thiz, SIGHT_READ_WRITE_VOLUME);
        CALL_METHOD1(0002, thiz, SIGHT_DRIVE_SWAP);
        SET_IFIELD_J(0005, thiz, (jlong) e->swap_table->swt_ent[e->swap_idx].ste_pages * (jlong) size);
        SET_IFIELD_J(0006, thiz, (jlong) e->swap_table->swt_ent[e->swap_idx].ste_length * (jlong) size);
        SET_IFIELD_J(0007, thiz, (jlong) e->swap_table->swt_ent[e->swap_idx].ste_free * (jlong) size);
        e->swap_idx++;
        return;
    }
    if (e->mounts_idx < e->num_mounts) {
        struct sight_mnttab *ent;
        int i;
        int flags = 0;
        int dtype;
        ent = e->ent;
        for (i=1; i<e->mounts_idx; i++) {
            ent = ent->next;
        }
        e->mounts_idx++;
        dtype = sight_get_fs_type(ent->ent.mnt_fstype);
        SET_IFIELD_S(0000, thiz, ent->ent.mnt_special);
        SET_IFIELD_S(0001, thiz, ent->ent.mnt_fstype);
        SET_IFIELD_S(0002, thiz, ent->ent.mnt_mountp);
        CALL_METHOD1(0000, thiz, dtype);

        if (!statvfs(ent->ent.mnt_mountp, &sv)) {
            SET_IFIELD_I(0004, thiz, sv.f_bsize);
            SET_IFIELD_J(0005, thiz, (jlong) sv.f_frsize * (jlong) sv.f_bavail);
            SET_IFIELD_J(0006, thiz, (jlong) sv.f_frsize * (jlong) sv.f_blocks);
            SET_IFIELD_J(0007, thiz, (jlong) sv.f_frsize * (jlong) sv.f_bfree);
        }
        if (hasmntopt(&ent->ent, MNTOPT_RW))
            flags |= SIGHT_READ_WRITE_VOLUME;
        if (hasmntopt(&ent->ent, MNTOPT_RO))
            flags |= SIGHT_READ_ONLY_VOLUME;
        if (hasmntopt(&ent->ent, MNTOPT_SUID))
            flags |= SIGHT_SUID_VOLUME;
        CALL_METHOD1(0001, thiz, flags);
        switch (dtype) {
            case SIGHT_FS_UNKNOWN:
                CALL_METHOD1(0002, thiz, SIGHT_DRIVE_UNKNOWN);
            break;
            case SIGHT_FS_ISO9660:
                CALL_METHOD1(0002, thiz, SIGHT_DRIVE_CDROM);
            break;
            case SIGHT_FS_DEV:
            case SIGHT_FS_PROC:
            case SIGHT_FS_SYSFS:
            case SIGHT_FS_TMPFS:
            case SIGHT_FS_RAMFS:
                CALL_METHOD1(0002, thiz, SIGHT_DRIVE_RAMDISK);
            break;
            case SIGHT_FS_NFS:
            case SIGHT_FS_RPC:
            case SIGHT_FS_VMBLOCK:
                CALL_METHOD1(0002, thiz, SIGHT_DRIVE_REMOTE);
            break;
            case SIGHT_FS_USBFS:
                CALL_METHOD1(0002, thiz, SIGHT_DRIVE_REMOVABLE);
            break;
            case SIGHT_FS_SWAP:
                CALL_METHOD1(0002, thiz, SIGHT_DRIVE_SWAP);
            break;
            default:
                CALL_METHOD1(0002, thiz, SIGHT_DRIVE_FIXED);
            break;
        }

    }
}

SIGHT_EXPORT_DECLARE(jint, Volume, enum3)(SIGHT_STDARGS,
                                          jobject thiz,
                                          jlong handle)
{
    return 0;
}

/* Close volume enumeration */
SIGHT_EXPORT_DECLARE(void, Volume, enum4)(SIGHT_STDARGS,
                                          jlong handle)
{
    volume_enum_t *e = J2P(handle, volume_enum_t *);

    UNREFERENCED_STDARGS;
    if (e) {
        apr_pool_destroy(e->pool);
        free(e);
    }
}
