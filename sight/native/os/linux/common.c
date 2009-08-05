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
#include "sight_private.h"
#include <unistd.h>

/* Common posix code */

#define PROC_FS_BASE "/proc"
#define PROC_FS_DIR  "/proc/"

int sight_stat_by_inode(ino_t inode, uid_t uid, struct stat *sb, pid_t *pid)
{
    DIR *pd;
    struct dirent *pent, pbuf;
    int rv = ENOENT;

    if (!(pd = opendir(PROC_FS_BASE)))
        return errno;

    while (!readdir_r(pd, &pbuf, &pent)) {
        DIR *fd;
        struct dirent *fent, fbuf;
        char pname[64];

        if (!pent)
            break;
        if (!apr_isdigit(*pent->d_name)) {
            /* Skip non pid directories */
            continue;
        }
        strcpy(pname, PROC_FS_DIR);
        strcat(pname, pent->d_name);
        if (uid >= 0) {
            struct stat us;
            if (stat(pname, &us) < 0)
                continue;
            if (us.st_uid != uid)
                continue;
        }
        strcat(pname, "/fd");
        if (!(fd = opendir(pname)))
            continue;
        while (!readdir_r(fd, &fbuf, &fent)) {
            char fname[64];
            if (!fent)
                break;
            if (!apr_isdigit(*fent->d_name)) {
                /* Skip non numeric files */
                continue;
            }

            strcpy(fname, pname);
            strcat(fname, "/");
            strcat(fname, fent->d_name);

            if (stat(fname, sb) < 0)
                continue;
            if (sb->st_ino == inode) {
                /* Wow, we found it */
                struct stat lb;
                *pid = (pid_t)strtoul(pent->d_name, NULL, 10);
                /* Get file times */
                if (!lstat(fname, &lb)) {
                    sb->st_atime = lb.st_atime;
                    sb->st_ctime = lb.st_ctime;
                    sb->st_mtime = lb.st_mtime;
                }
                rv = 0;
                break;
            }
        }
        closedir(fd);
        if (!rv)
            break;
    }
    closedir(pd);
    return rv;
}
