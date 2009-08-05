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

typedef struct volume_enum_t {
    DWORD                 dwDrives;     // Drives bitmask
    DWORD                 dwVolumes;    // Number of volumes
    DWORD                 dwIndex;      // Current drive index
    DWORD                 dwMount;      // Current mount index
    HANDLE                hPt;          // handle for mount point scan
    BOOL                  bVolume;
    CHAR                  szMount[MAX_PATH];
    CHAR                  szVname[MAX_PATH];
} volume_enum_t;

/* Initialize volume enumeration */
SIGHT_EXPORT_DECLARE(jlong, Volume, enum0)(SIGHT_STDARGS,
                                           jlong pool)
{
    DWORD i;
    char buff[MAX_PATH];
    char desc[MAX_PATH];
    DWORD flags;
    volume_enum_t *e;
    UINT em;

    UNREFERENCED_O;
    if (!(e = (volume_enum_t *)sight_calloc(_E,
                               sizeof(volume_enum_t),
                               THROW_FMARK))) {
        return 0;
    }
    em = SetErrorMode(SEM_FAILCRITICALERRORS);
    e->dwDrives = GetLogicalDrives();
    for (i = 0; i < 26; i++) {
        char name[4] = " :\\";
        if ((e->dwDrives >> i) & 1) {
            name[0] = 'A' + (char)i;
            e->dwVolumes++;
           if (GetVolumeInformationA(name, desc, MAX_PATH, NULL, NULL,
                                    &flags, buff, MAX_PATH)) {
                if (flags & FILE_SUPPORTS_REPARSE_POINTS) {
                    HANDLE vol = FindFirstVolumeMountPointA(name, buff,
                                                            MAX_PATH);
                    if (!IS_INVALID_HANDLE(vol)) {
                        do {
                            e->dwVolumes++;
                        } while(FindNextVolumeMountPointA(vol, buff,
                                                          MAX_PATH));
                        FindVolumeMountPointClose(vol);
                    }
                }
            }
        }
    }
    SetErrorMode(em);
    return P2J(e);
}

SIGHT_EXPORT_DECLARE(jint, Volume, enum1)(SIGHT_STDARGS,
                                          jlong handle)
{
    volume_enum_t *e = J2P(handle, volume_enum_t *);

    UNREFERENCED_STDARGS;

    if (!e)
        return 0;
    else
        return e->dwVolumes;

}

SIGHT_EXPORT_DECLARE(void, Volume, enum2)(SIGHT_STDARGS,
                                          jobject thiz,
                                          jlong handle)
{
    DWORD i;
    volume_enum_t *e = J2P(handle, volume_enum_t *);
    UINT em;
    char buff[MAX_PATH];
    char desc[MAX_PATH];
    DWORD flags;
    UNREFERENCED_O;

    if (!e || !thiz)
        return;
    em = SetErrorMode(SEM_FAILCRITICALERRORS);
    for (i = e->dwIndex; i < 26; i++) {
        char name[4] = " :\\";
        if ((e->dwDrives >> i) & 1) {
            DWORD sz[4];
            ULARGE_INTEGER ul[3];
            name[0] = 'A' + (char)i;

            if (GetVolumeNameForVolumeMountPointA(name, buff, MAX_PATH)) {
                SET_IFIELD_S(0000, thiz, buff);
            }
            SET_IFIELD_S(0002, thiz, name);

           if (GetVolumeInformationA(name, desc, MAX_PATH, NULL, NULL,
                                    &flags, buff, MAX_PATH)) {
                if (*desc) {
                    SET_IFIELD_S(0001, thiz, desc);
                }
                CALL_METHOD1(0000, thiz, sight_get_fs_type(buff));
            }
            if (GetDiskFreeSpaceA(name, &sz[0], &sz[1], &sz[2], &sz[3])) {
                SET_IFIELD_I(0003, thiz, sz[0]);
                SET_IFIELD_I(0004, thiz, sz[1]);
            }
            if (GetDiskFreeSpaceExA(name, &ul[0], &ul[1], &ul[2])) {
                SET_IFIELD_J(0005, thiz, ul[0].QuadPart);
                SET_IFIELD_J(0006, thiz, ul[1].QuadPart);
                SET_IFIELD_J(0007, thiz, ul[2].QuadPart);
            }
            if (flags & FILE_SUPPORTS_REPARSE_POINTS) {
                DWORD num = 0;
                HANDLE vol = FindFirstVolumeMountPointA(name, buff,
                                                        MAX_PATH);
                if (!IS_INVALID_HANDLE(vol)) {
                    strcpy(e->szVname, name);
                    strcat(e->szVname, buff);
                    do {
                        num++;
                    } while(FindNextVolumeMountPointA(vol, buff,
                                                      MAX_PATH));
                    FindVolumeMountPointClose(vol);
                }
                if (num)
                    e->hPt = FindFirstVolumeMountPointA(name, buff,
                                                        MAX_PATH);
                SET_IFIELD_I(0008, thiz, num);
            }
            if (!(flags & FILE_READ_ONLY_VOLUME)) {
                flags |= SIGHT_READ_WRITE_VOLUME;
                flags |= SIGHT_SUID_VOLUME;
            }
            CALL_METHOD1(0001, thiz, flags);
            strcpy(e->szMount, name);
            CALL_METHOD1(0002, thiz, GetDriveType(name));
            break;
        }
    }
    e->dwIndex = i + 1;

    SetErrorMode(em);
}

SIGHT_EXPORT_DECLARE(jint, Volume, enum3)(SIGHT_STDARGS,
                                          jobject thiz,
                                          jlong handle)
{
    jint rv = 0;
    volume_enum_t *e = J2P(handle, volume_enum_t *);
    UINT em;
    char name[MAX_PATH];
    char buff[MAX_PATH];
    char desc[MAX_PATH];
    DWORD flags;
    DWORD sz[4];
    ULARGE_INTEGER ul[3];
    UNREFERENCED_O;

    if (!e || !thiz)
        return 0;
    if (IS_INVALID_HANDLE(e->hPt)) {
        return 0;
    }

    em = SetErrorMode(SEM_FAILCRITICALERRORS);
    GetVolumeNameForVolumeMountPointA(e->szVname, name, MAX_PATH);
    SET_IFIELD_S(0000, thiz, name);
    SET_IFIELD_S(0002, thiz, e->szVname);

   if (GetVolumeInformationA(name, desc, MAX_PATH, NULL, NULL,
                            &flags, buff, MAX_PATH)) {
        if (*desc) {
            SET_IFIELD_S(0001, thiz, desc);
        }
        CALL_METHOD1(0000, thiz, sight_get_fs_type(buff));
    }
    if (GetDiskFreeSpaceA(name, &sz[0], &sz[1], &sz[2], &sz[3])) {
        SET_IFIELD_I(0003, thiz, sz[0]);
        SET_IFIELD_I(0004, thiz, sz[1]);
    }
    if (GetDiskFreeSpaceExA(name, &ul[0], &ul[1], &ul[2])) {
        SET_IFIELD_J(0005, thiz, ul[0].QuadPart);
        SET_IFIELD_J(0006, thiz, ul[1].QuadPart);
        SET_IFIELD_J(0007, thiz, ul[2].QuadPart);
    }
    CALL_METHOD1(0001, thiz, flags);
    CALL_METHOD1(0002, thiz, GetDriveType(e->szVname));

    if (!FindNextVolumeMountPointA(e->hPt, buff, MAX_PATH)) {
        FindVolumeMountPointClose(e->hPt);
        e->hPt = NULL;
        rv = -1;
    }
    else {
        strcpy(e->szVname, e->szMount);
        strcat(e->szVname, buff);
        rv = 1;
    }
    SetErrorMode(em);
    return rv;
}

/* Close volume enumeration */
SIGHT_EXPORT_DECLARE(void, Volume, enum4)(SIGHT_STDARGS,
                                          jlong handle)
{
    volume_enum_t *e = J2P(handle, volume_enum_t *);

    UNREFERENCED_STDARGS;
    if (e) {
        if (!IS_INVALID_HANDLE(e->hPt))
            FindVolumeMountPointClose(e->hPt);
        free(e);
    }
}
