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

/*
 * Network
 */

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "Network"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "HostName",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "DomainName",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "DnsServerAddresses",
    "[L" SIGHT_CLASS_PATH "NetworkAddress;"
};

SIGHT_CLASS_LDEF(Network)
{
    if (sight_load_class(_E, &_clazzn))
        return 1;
    J_LOAD_IFIELD(0000);
    J_LOAD_IFIELD(0001);
    J_LOAD_IFIELD(0002);

    return 0;
}

SIGHT_CLASS_UDEF(Network)
{
    sight_unload_class(_E, &_clazzn);
}

SIGHT_EXPORT_DECLARE(void, Network, info0)(SIGHT_STDARGS,
                                           jobject thiz,
                                           jlong pool)

{
    int i;
    sight_arr_t *tdns;
    char buf[SIGHT_SBUFFER_SIZ];
    char *pd;
    jsize len = 0, idx = 0;
    UNREFERENCED_O;

    if (gethostname(buf, SIGHT_SBUFFER_LEN)) {
        throwAprException(_E, apr_get_os_error());
        return;
    }
    if ((pd = strchr(buf, '.'))) {
        *pd++ = '\0';
        SET_IFIELD_S(0000, thiz, buf);
        SET_IFIELD_S(0001, thiz, pd);
    }
    else {
        /* Use domainname if hostname misses dot */
        SET_IFIELD_S(0000, thiz, buf);
        if (getdomainname(buf, SIGHT_SBUFFER_LEN)) {
            throwAprException(_E, apr_get_os_error());
            return;
        }
        SET_IFIELD_S(0001, thiz, buf);
    }
    if ((tdns = sight_arr_rload("/etc/resolv.conf"))) {
        for (i = 0; i < tdns->siz; i++) {
            if (strstr(tdns->arr[i], "nameserver")) {
                len++;
            }
        }
        if (len) {
            jobject addr;
            jobjectArray aaddr;
            aaddr = sight_new_netaddr_array(_E, _O, len);
            if (!aaddr || (*_E)->ExceptionCheck(_E)) {
                goto cleanup;
            }
            for (i = 0; i < tdns->siz; i++) {
                if ((pd = strstr(tdns->arr[i], "nameserver"))) {
                    pd += 10;
                    addr = sight_new_netaddr_class(_E, _O);
                    if (!addr || (*_E)->ExceptionCheck(_E)) {
                        goto cleanup;
                    }
                    sight_netaddr_set_addr(_E, addr, sight_trim(pd));
                    sight_netaddr_set_family(_E, addr, AF_INET);
                    (*_E)->SetObjectArrayElement(_E, aaddr, idx++, addr);
                    (*_E)->DeleteLocalRef(_E, addr);
                }
            }
            SET_IFIELD_O(0002, thiz, aaddr);
            (*_E)->DeleteLocalRef(_E, aaddr);
        }
    }
cleanup:
    if (tdns)
        sight_arr_free(tdns);
}

