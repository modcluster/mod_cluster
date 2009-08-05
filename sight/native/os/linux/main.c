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
#include <sys/sysinfo.h>
#include <sys/utsname.h>
#include <unistd.h>

static apr_int64_t sight_ticks_ms = 0L;
static apr_int64_t sight_ticks_us = 0L;

int kernel_major = 0;
int kernel_minor = 0;
int kernel_patch = 0;

apr_int64_t TCK2MS(apr_int64_t t)
{
    return (t * sight_ticks_ms);
}

apr_int64_t TCK2US(apr_int64_t t)
{
    return (t * sight_ticks_us);
}

apr_status_t sight_main(apr_pool_t *pool)
{
    struct utsname uts;
    sight_ticks_ms = 1000L    / sysconf(_SC_CLK_TCK);
    sight_ticks_us = 1000000L / sysconf(_SC_CLK_TCK);

    if (!uname(&uts)) {
        if (sscanf(uts.release, "%d.%d.%d",
                   &kernel_major, &kernel_minor, &kernel_patch) != 3) {
            kernel_major = 0;
            kernel_minor = 0;
            kernel_patch = 0;
        }
    }
    return APR_SUCCESS;
}
