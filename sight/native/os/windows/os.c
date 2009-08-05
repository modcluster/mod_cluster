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

#define SIGHT_WANT_LATE_DLL
#include "sight_private.h"


SIGHT_EXPORT_DECLARE(jboolean, OS, is)(SIGHT_STDARGS, jint type)
{
    UNREFERENCED_STDARGS;
#ifdef _WIN64
    if (type == SIGHT_OS_WIN64)
        return JNI_TRUE;
    else
#endif
    if (type == SIGHT_OS_WINDOWS)
        return JNI_TRUE;
    else if (type == SIGHT_OS_WOW64) {
        BOOL is = FALSE;
        IsWow64Process(GetCurrentProcess(), &is);
        return V2Z(is);
    }
    else
        return JNI_FALSE;
}

SIGHT_EXPORT_DECLARE(jstring, OS, getSysname)(SIGHT_STDARGS)
{
    return CSTR_TO_JSTRING("Windows");
}

SIGHT_EXPORT_DECLARE(jstring, OS, getVersion)(SIGHT_STDARGS)
{
    char buf[SIGHT_SBUFFER_SIZ];

    if (sight_osver->dwMajorVersion == 4) {
       strcpy(buf, "NT4");
    }
    else if (sight_osver->dwMajorVersion == 5) {
        if (sight_osver->dwMinorVersion == 0) {
            strcpy(buf, "2000");
        }
        else if (sight_osver->dwMinorVersion == 2) {
            strcpy(buf, "2003");
        }
        else {
            strcpy(buf, "XP");
        }
    }
    else if (sight_osver->dwMajorVersion == 6) {
        strcpy(buf, "VISTA");
    }
    else {
        strcpy(buf, "UNKNOWN");
    }
    if (sight_osver->szCSDVersion[0]) {
        strcat(buf, " (");
        strcat(buf, sight_osver->szCSDVersion);
        strcat(buf, ")");
    }
    return CSTR_TO_JSTRING(buf);
}

SIGHT_EXPORT_DECLARE(jstring, OS, getRelease)(SIGHT_STDARGS)
{
    char buf[32];

    sprintf(buf, "%d.%d.%d", sight_osver->dwMajorVersion,
                             sight_osver->dwMinorVersion,
                             sight_osver->dwBuildNumber);
    return CSTR_TO_JSTRING(buf);
}

SIGHT_EXPORT_DECLARE(jstring, OS, getMachine)(SIGHT_STDARGS)
{
    char buf[32];

    if (sight_osinf->wProcessorArchitecture == PROCESSOR_ARCHITECTURE_AMD64)
        strcpy(buf, "x86_64");
    else if (sight_osinf->wProcessorArchitecture == PROCESSOR_ARCHITECTURE_IA64)
        strcpy(buf, "ia64");
    else if (sight_osinf->wProcessorArchitecture == PROCESSOR_ARCHITECTURE_INTEL) {
        int pl = sight_osinf->wProcessorLevel;
        if (pl < 8)
            sprintf(buf, "i%d86", sight_osinf->wProcessorLevel);
        else {
            /* TODO: Figure out the proper names for EMT64
             * and other Intel processors
             */
            strcpy(buf, "i786");
        }
    }
    else
        return NULL;
    return CSTR_TO_JSTRING(buf);
}

SIGHT_EXPORT_DECLARE(jstring, OS, getNodename)(SIGHT_STDARGS)
{
    char buf[MAX_COMPUTERNAME_LENGTH + 1] = { 0 };
    DWORD len = MAX_COMPUTERNAME_LENGTH;

    if (GetComputerName(buf, &len)) {
        return CSTR_TO_JSTRING(buf);
    }
    else {
        return NULL;
    }
}
