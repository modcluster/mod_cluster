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

/**
 * POSIX syslog implementation
 *
 */

#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"

#include <syslog.h>
#include <stdarg.h>

#ifndef LOG_WARN
#define LOG_WARN LOG_WARNING
#endif

#define LOG_MSG_DOMAIN  "Sight"

SIGHT_EXPORT_DECLARE(void, Syslog, init0)(SIGHT_STDARGS,
                                          jstring domain)
{
    const char *d;
    SIGHT_ALLOC_CSTRING(domain);

    UNREFERENCED_O;
    if ((d = J2S(domain)) == NULL)
        d = LOG_MSG_DOMAIN;

    openlog(d, LOG_CONS | LOG_PID, LOG_LOCAL0);
    SIGHT_FREE_CSTRING(domain);
}

SIGHT_EXPORT_DECLARE(void, Syslog, close0)(SIGHT_STDARGS)
{

    UNREFERENCED_STDARGS;
    closelog();
}

SIGHT_EXPORT_DECLARE(void, Syslog, log0)(SIGHT_STDARGS,
                                         jint level,
                                         jstring msg)
{
    SIGHT_ALLOC_CSTRING(msg);
    int id = LOG_DEBUG;

    UNREFERENCED_O;

    switch (level) {
        case SIGHT_LOG_EMERG:
            id = LOG_EMERG;
        break;
        case SIGHT_LOG_ERROR:
            id = LOG_ERR;
        break;
        case SIGHT_LOG_NOTICE:
            id = LOG_NOTICE;
        break;
        case SIGHT_LOG_WARN:
            id = LOG_WARN;
        break;
        case SIGHT_LOG_INFO:
            id = LOG_INFO;
        break;
    }
    syslog (id, "%s", J2S(msg));
    SIGHT_FREE_CSTRING(msg);
}
