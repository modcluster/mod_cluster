/*
 *  Advertise (test for multicast)
 *
 *  Copyright(c) 2009 Red Hat Middleware, LLC,
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
 * @author Jean-Frederic Clere
 */

#include "apr.h"
#include "apr_network_io.h"
#include "apr_strings.h"
#include "apr_thread_proc.h"

static apr_sockaddr_t   *ma_mgroup_sa;
static apr_socket_t     *ma_mgroup_socket;

static void * APR_THREAD_FUNC parent_thread(apr_thread_t *thd, void *data) {
    char buf[20];
    apr_size_t n;
    char *s;
    apr_status_t rv;
    apr_pool_t *pool = data;

    apr_sleep(APR_TIME_C(100000));
    s = apr_psprintf(pool, "to %pI", ma_mgroup_sa);
    printf("apr_socket_sendto %s\n", s);

    n = apr_snprintf(buf, 20, "Advertize !!!\n");
    rv = apr_socket_sendto(ma_mgroup_socket,
                            ma_mgroup_sa, 0, buf, &n);
   if (rv != APR_SUCCESS) {
        printf("apr_socket_sendto failed %d %s\n", n, apr_strerror(rv, buf, 20));
        return;
    }
    return;
}
int main(int argc, char **argv)
{
    apr_pool_t *pool;
    apr_status_t rv;
    apr_sockaddr_t   *ma_listen_sa;
    apr_sockaddr_t   *ma_niface_sa;
    apr_thread_t *tp;

    apr_initialize();
    atexit(apr_terminate);

    apr_pool_create(&pool, NULL);

    rv = apr_sockaddr_info_get(&ma_mgroup_sa, "224.0.1.105", APR_INET, 23364, APR_UNSPEC, pool);
    if (rv != APR_SUCCESS) {
        printf("apr_sockaddr_info_get failed %d\n", rv);
        return 1;
    }
    rv = apr_sockaddr_info_get(&ma_listen_sa, NULL, ma_mgroup_sa->family, 0, APR_UNSPEC, pool);
    if (rv != APR_SUCCESS) {
        printf("apr_sockaddr_info_get(NULL) failed %d\n", rv);
        return 1;
    }
    rv = apr_sockaddr_info_get(&ma_niface_sa, NULL, ma_mgroup_sa->family, 0, APR_UNSPEC, pool);
    if (rv != APR_SUCCESS) {
        printf("apr_sockaddr_info_get(NULL) failed %d\n", rv);
        return 1;
    }

    rv = apr_socket_create(&ma_mgroup_socket, ma_mgroup_sa->family, SOCK_DGRAM, APR_PROTO_UDP, pool);
    if (rv != APR_SUCCESS) {
        printf("apr_socket_create failed %d\n", rv);
        return 1;
    }
    rv =  apr_socket_bind(ma_mgroup_socket, ma_listen_sa);
    if (rv != APR_SUCCESS) {
        printf("apr_socket_bind failed %d\n", rv);
        return 1;
    }
    char *s;
    s = apr_psprintf(pool, "on %pI", ma_listen_sa);
    printf("apr_socket_bind %s\n", s);
    s = apr_psprintf(pool, "on %pI", ma_niface_sa);
    printf("apr_mcast_join %s\n", s);

    rv = apr_mcast_join(ma_mgroup_socket, ma_mgroup_sa, ma_niface_sa, NULL);
    if (rv != APR_SUCCESS) {
        printf("apr_mcast_join failed %d\n", rv);
        return 1;
    }
/*
    rv = apr_mcast_loopback(ma_mgroup_socket, 1);
    if (rv != APR_SUCCESS) {
        printf("apr_mcast_loopback failed %d\n", rv);
        return 1;
    }
 */
    rv = apr_mcast_hops(ma_mgroup_socket, 10);
    if (rv != APR_SUCCESS) {
        printf("apr_mcast_hops failed %d\n", rv);
        return 1;
    }
    rv = apr_thread_create(&tp, NULL, parent_thread, pool, pool);
    if (rv != APR_SUCCESS) {
        printf("apr_thread_create failed %d\n", rv);
        return 1;
    }
    apr_thread_detach(tp);
    apr_sleep(APR_TIME_C(150000));
    return 0;
}
