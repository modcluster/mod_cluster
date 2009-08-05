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

#include <mntent.h>
#include <sys/statfs.h>
#include <sys/statvfs.h>

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

#define PROC_SWAPS_FS   "/proc/swaps"

typedef struct volume_enum_t {
    int         num_mounts;
    FILE        *fp;
    sight_arr_t *swaps;
    int         swap_idx;
} volume_enum_t;

/* Initialize volume enumeration */
SIGHT_EXPORT_DECLARE(jlong, Volume, enum0)(SIGHT_STDARGS,
                                           jlong pool)
{
    volume_enum_t *e;
    char buf[SIGHT_MBUFFER_SIZ];

    UNREFERENCED_O;
    if (!(e = (volume_enum_t *)sight_calloc(_E,
                               sizeof(volume_enum_t),
                               THROW_FMARK))) {
        return 0;
    }
    return P2J(e);
}

SIGHT_EXPORT_DECLARE(jint, Volume, enum1)(SIGHT_STDARGS,
                                          jlong handle)
{
    struct mntent ent;
    char buf[SIGHT_MBUFFER_SIZ];
    volume_enum_t *e = J2P(handle, volume_enum_t *);

    UNREFERENCED_O;

    if (!e)
        return 0;
    if (!(e->fp = setmntent(MOUNTED, "r"))) {
        throwAprException(_E, apr_get_os_error());
        return 0;
    } else {
        e->num_mounts = 0;
        while (getmntent_r(e->fp, &ent, buf, SIGHT_MBUFFER_LEN)) {
            e->num_mounts++;
        }
        endmntent(e->fp);
        e->fp = setmntent(MOUNTED, "r");
        /* Now read the swaps */
        if ((e->swaps = sight_arr_rload(PROC_SWAPS_FS))) {
            if (e->swaps->siz) {
                e->num_mounts += (e->swaps->siz - 1);
                e->swap_idx = 1;
            }
        }
        return e->num_mounts;
    }
}

SIGHT_EXPORT_DECLARE(void, Volume, enum2)(SIGHT_STDARGS,
                                          jobject thiz,
                                          jlong handle)
{
    struct mntent ent;
    struct statvfs sv;
    char buf[SIGHT_MBUFFER_SIZ];
    volume_enum_t *e = J2P(handle, volume_enum_t *);

    UNREFERENCED_O;

    if (!e || !thiz)
        return;
    if (e->swaps && e->swap_idx < e->swaps->siz) {
        char path[256];
        char type[32];
        unsigned long size, used;
        int priority;

        if (sscanf(e->swaps->arr[e->swap_idx],
                   "%250s %30s %lu %lu %d",
                   path, type, &size, &used, &priority) == 5) {
            SET_IFIELD_S(0000, thiz, path);
            SET_IFIELD_S(0001, thiz, type);
            SET_IFIELD_S(0002, thiz, "swap");
            CALL_METHOD1(0000, thiz, SIGHT_FS_SWAP);
            CALL_METHOD1(0001, thiz, SIGHT_READ_WRITE_VOLUME);
            CALL_METHOD1(0002, thiz, SIGHT_DRIVE_SWAP);
            SET_IFIELD_J(0005, thiz, size - used);
            SET_IFIELD_J(0006, thiz, size);
            SET_IFIELD_J(0007, thiz, size - used);
        }
        e->swap_idx++;
        return;
    }
    if (getmntent_r(e->fp, &ent, buf, SIGHT_MBUFFER_LEN)) {
        int flags = 0;
        int dtype = sight_get_fs_type(ent.mnt_type);
        SET_IFIELD_S(0000, thiz, ent.mnt_fsname);
        SET_IFIELD_S(0002, thiz, ent.mnt_dir);
        CALL_METHOD1(0000, thiz, dtype);

        if (!statvfs(ent.mnt_dir, &sv)) {
            SET_IFIELD_I(0004, thiz, sv.f_bsize);
            SET_IFIELD_J(0005, thiz, sv.f_frsize * sv.f_bavail);
            SET_IFIELD_J(0006, thiz, sv.f_frsize * sv.f_blocks);
            SET_IFIELD_J(0007, thiz, sv.f_frsize * sv.f_bfree);
        }
        if (hasmntopt(&ent, MNTOPT_RW))
            flags |= SIGHT_READ_WRITE_VOLUME;
        if (hasmntopt(&ent, MNTOPT_RO))
            flags |= SIGHT_READ_ONLY_VOLUME;
        if (hasmntopt(&ent, MNTOPT_SUID))
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
        if (e->fp)
            endmntent(e->fp);
        if (e->swaps)
            sight_arr_free(e->swaps);
        free(e);
    }
}
