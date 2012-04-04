/*
 *  ModAdvertise - Apache Httpd advertising module
 *
 *  Copyright(c) 2008 Red Hat Middleware, LLC,
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
 */

#define CORE_PRIVATE

#include "mod_advertise.h"
#include "mod_core.h"
#include "util_script.h"

/*
 * Declare ourselves so the configuration routines can find and know us.
 * We'll fill it in at the end of the module.
 */
module AP_MODULE_DECLARE_DATA advertise_module;



/*
 * Server private data
 */
#if defined(WIN32)
static pid_t      ma_parent_pid     = -1;
#endif

/* Global (really) */

static int   ma_advertise_run  = 0;
static int   ma_advertise_stat = 0;
static server_rec *main_server = NULL;

/*
 * configuration.
 */
typedef struct mod_advertise_config
{
    server_rec *ma_advertise_server;
    char *ma_advertise_adrs;
    char *ma_advertise_adsi;
    char *ma_advertise_srvm;
    char *ma_advertise_srvh;
    char *ma_advertise_srvs;
    char *ma_advertise_srvi;
    char *ma_advertise_uuid;

    char *ma_advertise_skey;

    int   ma_bind_set;
    char *ma_bind_adrs;
    char *ma_bind_adsi;
    apr_port_t ma_bind_port;

    apr_port_t ma_advertise_port;
    apr_port_t ma_advertise_srvp;
    ma_advertise_e ma_advertise_mode;
    apr_interval_time_t ma_advertise_freq;
} mod_advertise_config;


/* Advertise sockets */
static apr_socket_t     *ma_mgroup_socket = NULL;
static apr_socket_t     *ma_listen_socket = NULL;
static apr_sockaddr_t   *ma_mgroup_sa     = NULL;
static apr_sockaddr_t   *ma_listen_sa     = NULL;
static apr_sockaddr_t   *ma_niface_sa     = NULL;

static server_rec       *ma_server_rec    = NULL;

/* Advertise sequence number */
static volatile apr_int64_t ma_sequence   = 0;


/* Parent and child manager thread statuses */
static volatile int is_mp_running = 0;
static volatile int is_mp_created = 0;

/*
 * Server global data
 */
typedef struct ma_global_data_t {
    unsigned char ssalt[APR_MD5_DIGESTSIZE];
    apr_uuid_t    suuid;
    char          srvid[APR_UUID_FORMATTED_LENGTH + 2];
    apr_pool_t   *ppool;
    apr_pool_t   *cpool;
} ma_global_data_t;

/*
 * Global data instance
 * For parent, registered in process pool
 */
static ma_global_data_t  *magd = NULL;

/* Evaluates to true if the (apr_sockaddr_t *) addr argument is the
 * IPv4 match-any-address, 0.0.0.0. */
#define IS_INADDR_ANY(addr) ((addr)->family == APR_INET && \
                             (addr)->sa.sin.sin_addr.s_addr == INADDR_ANY)

/* Evaluates to true if the (apr_sockaddr_t *) addr argument is the
 * IPv6 match-any-address, [::]. */
#define IS_IN6ADDR_ANY(addr) ((addr)->family == APR_INET6 && \
                        IN6_IS_ADDR_UNSPECIFIED(&(addr)->sa.sin6.sin6_addr))

/*--------------------------------------------------------------------------*/
/*                                                                          */
/* ServerAdvertise directive                                                */
/*                                                                          */
/*--------------------------------------------------------------------------*/
static const char *cmd_advertise_m(cmd_parms *cmd, void *dummy,
                                   const char *arg, const char *opt)
{
    mod_advertise_config *mconf = ap_get_module_config(cmd->server->module_config, &advertise_module);
    if (mconf->ma_advertise_srvs)
        return "Duplicate ServerAdvertise directives are not allowed";

    if (strcasecmp(arg, "Off") == 0)
        mconf->ma_advertise_mode = ma_advertise_off;
    else if (strcasecmp(arg, "On") == 0)
        mconf->ma_advertise_mode = ma_advertise_on;
    else
        return "ServerAdvertise must be Off or On";
    if (opt) {
        const char *p = ap_strstr_c(opt, "://");
        if (p) {
            mconf->ma_advertise_srvm = apr_pstrndup(cmd->pool, opt, p - opt);
            opt = p + 3;
        }
        if (apr_parse_addr_port(&mconf->ma_advertise_srvs,
                                &mconf->ma_advertise_srvi,
                                &mconf->ma_advertise_srvp,
                                opt, cmd->pool) != APR_SUCCESS ||
                                !mconf->ma_advertise_srvs ||
                                !mconf->ma_advertise_srvp)
            return "Invalid ServerAdvertise Address";
    }
    mconf->ma_advertise_server = cmd->server;
    return NULL;
}


/*--------------------------------------------------------------------------*/
/*                                                                          */
/* AdvertiseGroup directive                                                 */
/*                                                                          */
/*--------------------------------------------------------------------------*/
static const char *cmd_advertise_g(cmd_parms *cmd, void *dummy,
                                   const char *arg)
{
    mod_advertise_config *mconf = ap_get_module_config(cmd->server->module_config, &advertise_module);
    if (mconf->ma_advertise_port != MA_DEFAULT_ADVPORT &&
        mconf->ma_advertise_adrs != MA_DEFAULT_GROUP)
        return "Duplicate AdvertiseGroup directives are not allowed";

    if (apr_parse_addr_port(&mconf->ma_advertise_adrs,
                            &mconf->ma_advertise_adsi,
                            &mconf->ma_advertise_port,
                            arg, cmd->pool) != APR_SUCCESS)
        return "Invalid AdvertiseGroup address";
    if (!mconf->ma_advertise_adrs)
        return "Missing Ip part from AdvertiseGroup address";
    if (!mconf->ma_advertise_port)
        mconf->ma_advertise_port = MA_DEFAULT_ADVPORT;
    mconf->ma_advertise_server = cmd->server;
    return NULL;
}

/*--------------------------------------------------------------------------*/
/*                                                                          */
/* AdvertiseBindAddress directive                                           */
/*                                                                          */
/*--------------------------------------------------------------------------*/
static const char *cmd_bindaddr(cmd_parms *cmd, void *dummy,
                                   const char *arg)
{
    mod_advertise_config *mconf = ap_get_module_config(cmd->server->module_config, &advertise_module);
    if (mconf->ma_bind_set)
        return "Duplicate AdvertiseBindAddress directives are not allowed";

    if (apr_parse_addr_port(&mconf->ma_bind_adrs,
                            &mconf->ma_bind_adsi,
                            &mconf->ma_bind_port,
                            arg, cmd->pool) != APR_SUCCESS)
        return "Invalid AdvertiseBindAddress address";
    if (!mconf->ma_bind_adrs)
        return "Missing Ip part from AdvertiseBindAddress address";
    if (!mconf->ma_bind_port)
        mconf->ma_bind_port = MA_DEFAULT_ADVPORT;
    mconf->ma_bind_set = 1;
    mconf->ma_advertise_server = cmd->server;
    return NULL;
}
/*--------------------------------------------------------------------------*/
/*                                                                          */
/* AdvertiseFrequency directive                                             */
/*                                                                          */
/*--------------------------------------------------------------------------*/

static const char *cmd_advertise_f(cmd_parms *cmd, void *dummy,
                                   const char *arg)
{
    apr_time_t s, u = 0;
    const char *p;

    mod_advertise_config *mconf = ap_get_module_config(cmd->server->module_config, &advertise_module);
    if (mconf->ma_advertise_freq != MA_DEFAULT_ADV_FREQ)
        return "Duplicate AdvertiseFrequency directives are not allowed";
    if ((p = ap_strchr_c(arg, '.')) || (p = ap_strchr_c(arg, ',')))
        u = atoi(p + 1);

    s = atoi(arg);
    mconf->ma_advertise_freq = s * APR_USEC_PER_SEC + u * APR_TIME_C(1000);
    if (mconf->ma_advertise_freq == 0)
        return "Invalid AdvertiseFrequency value";

    mconf->ma_advertise_server = cmd->server;
    return NULL;
}

/*--------------------------------------------------------------------------*/
/*                                                                          */
/* AdvertiseSecurityKey directive                                           */
/*                                                                          */
/*--------------------------------------------------------------------------*/
static const char *cmd_advertise_k(cmd_parms *cmd, void *dummy,
                                   const char *arg)
{
    mod_advertise_config *mconf = ap_get_module_config(cmd->server->module_config, &advertise_module);
    if (mconf->ma_advertise_skey != NULL)
        return "Duplicate AdvertiseSecurityKey directives are not allowed";
    mconf->ma_advertise_skey = apr_pstrdup(cmd->pool, arg);
    mconf->ma_advertise_server = cmd->server;
    return NULL;
}

/*--------------------------------------------------------------------------*/
/*                                                                          */
/* AdvertiseManagerUrl directive                                            */
/*                                                                          */
/*--------------------------------------------------------------------------*/
static const char *cmd_advertise_h(cmd_parms *cmd, void *dummy,
                                   const char *arg)
{
    mod_advertise_config *mconf = ap_get_module_config(cmd->server->module_config, &advertise_module);

    if (mconf->ma_advertise_srvh != NULL)
        return "Duplicate AdvertiseManagerUrl directives are not allowed";
    mconf->ma_advertise_srvh = apr_pstrdup(cmd->pool, arg);
    mconf->ma_advertise_server = cmd->server;
    return NULL;
}

#define MA_ADVERTISE_SERVER_FMT \
        "HTTP/1.0 %s" CRLF \
        "Date: %s" CRLF \
        "Sequence: %" APR_INT64_T_FMT CRLF \
        "Digest: %s" CRLF \
        "Server: %s" CRLF

static const char *hex = "0123456789abcdef";

apr_status_t ma_advertise_server(server_rec *server, int type)
{
    char buf[MA_BSIZE];
    char dat[APR_RFC822_DATE_LEN];
    unsigned char msig[APR_MD5_DIGESTSIZE];
    unsigned char ssig[APR_MD5_DIGESTSIZE * 2 + 1];
    const char *asl;
    char *p = buf;
    int  i, c = 0;
    apr_size_t l = MA_BSIZE - 8;
    apr_size_t n = 0;
    apr_md5_ctx_t md;
    mod_advertise_config *mconf = ap_get_module_config(server->module_config, &advertise_module);

    ma_sequence++;
    if (ma_sequence < 1)
        ma_sequence = 1;
    sprintf(buf, "%" APR_INT64_T_FMT, ma_sequence);
    ap_recent_rfc822_date(dat, apr_time_now());
    asl = ap_get_status_line(ma_advertise_stat);

    /* Create MD5 digest
     * salt + date + sequence + srvid
     */
    apr_md5_init(&md);
    apr_md5_update(&md, magd->ssalt, APR_MD5_DIGESTSIZE);
    apr_md5_update(&md, dat, strlen(dat));
    apr_md5_update(&md, buf, strlen(buf));
    apr_md5_update(&md, magd->srvid + 1, strlen(magd->srvid) - 1);
    apr_md5_final(msig, &md);
    /* Convert MD5 digest to hex string */
    for (i = 0; i < APR_MD5_DIGESTSIZE; i++) {
        ssig[c++] = hex[msig[i] >> 4];
        ssig[c++] = hex[msig[i] & 0x0F];
    }
    ssig[c] = '\0';
    n = apr_snprintf(p, l, MA_ADVERTISE_SERVER_FMT,
                     asl, dat, ma_sequence, ssig, magd->srvid + 1);
    if (type == MA_ADVERTISE_SERVER) {
        l -= n;
        n += apr_snprintf(p + n, l,
                          "X-Manager-Address: %s:%u" CRLF
                          "X-Manager-Url: %s" CRLF
                          "X-Manager-Protocol: %s" CRLF
                          "X-Manager-Host: %s" CRLF,
                          mconf->ma_advertise_srvs,
                          mconf->ma_advertise_srvp,
                          mconf->ma_advertise_srvh,
                          mconf->ma_advertise_srvm,
                          server->server_hostname);

    }
    strcat(p, CRLF);
    n += 2;
    return apr_socket_sendto(ma_mgroup_socket,
                             ma_mgroup_sa, 0, buf, &n);
}

static apr_status_t ma_group_join(const char *addr, apr_port_t port,
                                  const char *bindaddr, apr_port_t bindport,
                                  apr_pool_t *pool, server_rec *s)
{
    apr_status_t rv;

    if ((rv = apr_sockaddr_info_get(&ma_mgroup_sa, addr,
                                    APR_UNSPEC, port,
                                    APR_UNSPEC, pool)) != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, rv, s,
                     "mod_advertise: ma_group_join apr_sockaddr_info_get(%s:%d) failed",
                     addr, port);
        return rv;
    }
    if ((rv = apr_sockaddr_info_get(&ma_listen_sa, bindaddr,
                                    ma_mgroup_sa->family, bindport,
                                    APR_UNSPEC, pool)) != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, rv, s,
                     "mod_advertise: ma_group_join apr_sockaddr_info_get(%s:%d) failed", bindaddr, bindport);
        return rv;
    }
    if ((rv = apr_sockaddr_info_get(&ma_niface_sa, NULL,
                                    ma_mgroup_sa->family, 0,
                                    APR_UNSPEC, pool)) != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, rv, s,
                     "mod_advertise: ma_group_join apr_sockaddr_info_get(0.0.0.0:0) failed");
        return rv;
    }
    if ((rv = apr_socket_create(&ma_mgroup_socket,
                                ma_mgroup_sa->family,
                                SOCK_DGRAM,
                                APR_PROTO_UDP,
                                pool)) != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, rv, s,
                     "mod_advertise: ma_group_join apr_socket_create failed");
        return rv;
    }
    if ((rv = apr_socket_opt_set(ma_mgroup_socket,
                                 APR_SO_REUSEADDR, 1)) != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, rv, s,
                     "mod_advertise: ma_group_join apr_socket_opt_set failed");
        return rv;
    }
    if ((rv = apr_socket_bind(ma_mgroup_socket, ma_listen_sa)) != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, rv, s,
                     "mod_advertise: ma_group_join apr_socket_bind failed");
        return rv;
    }
    if ((rv = apr_mcast_join(ma_mgroup_socket, ma_mgroup_sa,
                             ma_niface_sa, NULL)) != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_DEBUG, rv, s,
                     "mod_advertise: ma_group_join apr_mcast_join failed");
        if ((rv = apr_mcast_loopback(ma_mgroup_socket, 1)) != APR_SUCCESS) {
            ap_log_error(APLOG_MARK, APLOG_WARNING, rv, s,
                         "mod_advertise: ma_group_join apr_mcast_loopback failed");
            apr_socket_close(ma_mgroup_socket);
            return rv;
        }     
    }
    if ((rv = apr_mcast_hops(ma_mgroup_socket,
                             MA_ADVERTISE_HOPS)) != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, rv, s,
                     "mod_advertise: ma_group_join apr_mcast_hops failed");
        /* Due a bug in apr (fixed by r1309332) apr_mcast_hops may fail */
        apr_mcast_leave(ma_mgroup_socket, ma_mgroup_sa,
                        NULL, NULL);
        apr_socket_close(ma_mgroup_socket);
        return rv;
    }
    return APR_SUCCESS;
}

static void ma_group_leave()
{
    if (ma_mgroup_socket) {
        apr_mcast_leave(ma_mgroup_socket, ma_mgroup_sa,
                        NULL, NULL);
        apr_socket_close(ma_mgroup_socket);
        ma_mgroup_socket = NULL;
    }
}

static void * APR_THREAD_FUNC parent_thread(apr_thread_t *thd, void *data)
{
    static int current_status  = 0;
    int f_time = 1;
    apr_interval_time_t a_step = 0;
    server_rec *s = (server_rec *)data;
    mod_advertise_config *mconf = ap_get_module_config(s->module_config, &advertise_module);
    is_mp_created = 1;

    while (is_mp_running) {
        apr_sleep(MA_TM_RESOLUTION);
        if (!is_mp_running)
            break;
        if (ma_advertise_run) {
            a_step += MA_TM_RESOLUTION;
            if (current_status != ma_advertise_stat) {
                /* Force advertise on status change */
                current_status = ma_advertise_stat;
                f_time = 1;
            }
            if (a_step >= mconf->ma_advertise_freq || f_time) {
                /* Run advertise */
                ma_advertise_server(s, MA_ADVERTISE_SERVER);
                a_step = 0;
                f_time = 0;
            }
            if (!is_mp_running)
                break;
        }
        /* TODO: Implement actual work for parent thread */
        if (!is_mp_running)
            break;
    }
    is_mp_created = 0;
    return NULL;
}

static apr_status_t pconfig_cleanup(void *data);

static apr_status_t process_cleanup(void *data)
{
    int advertise_run = ma_advertise_run;

    is_mp_running     = 0;
    ma_advertise_run  = 0;
    if (advertise_run) {
        ma_advertise_stat = HTTP_FORBIDDEN;
        ma_advertise_server(ma_server_rec, MA_ADVERTISE_STATUS);
    }
    if (is_mp_created) {
        apr_sleep(1000);
        /* Wait for the parent maintenance thread to finish */
        while (is_mp_created) {
            apr_sleep(MA_TM_RESOLUTION);
        }
    }
    if (advertise_run) {
        ma_advertise_stat = HTTP_GONE;
        ma_advertise_server(ma_server_rec, MA_ADVERTISE_STATUS);
        ma_group_leave();
    }
    /* We don't need the post_config cleanup to run,
     */
    apr_pool_cleanup_kill(magd->cpool, magd, pconfig_cleanup);
    magd = NULL;

    return APR_SUCCESS;
}

static apr_status_t pconfig_cleanup(void *data)
{
    int advertise_run = ma_advertise_run;

    is_mp_running     = 0;
    ma_advertise_run  = 0;
    if (advertise_run) {
        ma_advertise_stat = HTTP_FORBIDDEN;
        ma_advertise_server(ma_server_rec, MA_ADVERTISE_STATUS);
    }

    if (is_mp_created) {
        apr_sleep(1000);
        /* Wait for the parent maintenance thread to finish */
        while (is_mp_created) {
            apr_sleep(MA_TM_RESOLUTION);
        }
    }
    if (advertise_run) {
        ma_advertise_stat = HTTP_FORBIDDEN;
        ma_advertise_server(ma_server_rec, MA_ADVERTISE_STATUS);
    }
    if (magd) {
        /* Remove the process_cleanup.
         * We need to reattach again because the
         * module can be reloaded on different address
         */
        apr_pool_cleanup_kill(magd->ppool, magd, process_cleanup);
    }
    return APR_SUCCESS;
}

/*--------------------------------------------------------------------------*/
/*                                                                          */
/* Post config hook.                                                        */
/* Create management thread in parent and initializes Manager               */
/*                                                                          */
/*--------------------------------------------------------------------------*/
static int post_config_hook(apr_pool_t *pconf, apr_pool_t *plog,
                            apr_pool_t *ptemp, server_rec *s)
{
    apr_status_t rv;
    const char *pk = "advertise_init_module_tag";
    apr_pool_t *pproc = s->process->pool;
    apr_thread_t *tp;
    mod_advertise_config *mconf = ap_get_module_config(s->module_config, &advertise_module);
    int advertisefound = 0;
    server_rec *server = s;

    /* Advertise directive in more than one VirtualHost: not supported */
    while (server) {
        mod_advertise_config *conf = ap_get_module_config(server->module_config, &advertise_module);
        if (conf->ma_advertise_server == server) {
            if (advertisefound) {
                ap_log_error(APLOG_MARK, APLOG_ERR, 0, s,
                         "mod_advertise: directive in more than one VirtualHost: not supported");
                return !OK;
            } else
                advertisefound = -1;
        }
        server = server->next;
    }

    /* Our server */
    server = s;
    while (server) {
        mconf = ap_get_module_config(server->module_config, &advertise_module);
        if (mconf->ma_advertise_server == server)
            break;
        server = server->next;
    }

    apr_pool_userdata_get((void *)&magd, pk, pproc);
    if (!magd) {
        if (!(magd = apr_pcalloc(pproc, sizeof(ma_global_data_t))))
            return apr_get_os_error();
        apr_pool_create(&magd->ppool, pproc);
        apr_pool_userdata_set(magd, pk, apr_pool_cleanup_null, pproc);
        /* First time config phase -- skip. */
        return OK;
    }
#if defined(WIN32)
    {
        const char *ppid = getenv("AP_PARENT_PID");
        if (ppid) {
            ma_parent_pid = atol(ppid);
            ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, s,
                "[%" APR_PID_T_FMT " - %" APR_PID_T_FMT
                "] in child post config hook",
                getpid(), ma_parent_pid);
            return OK;
        }
    }
#endif
    ma_server_rec = server; 
    if (mconf->ma_advertise_skey) {
        apr_md5_ctx_t mc;
        apr_md5_init(&mc);
        apr_md5_update(&mc, mconf->ma_advertise_skey, strlen(mconf->ma_advertise_skey));
        apr_md5_final(magd->ssalt, &mc);
    }
    apr_uuid_get(&magd->suuid);
    magd->srvid[0] = '/';
    apr_uuid_format(&magd->srvid[1], &magd->suuid);
    if (!mconf->ma_advertise_srvh)
        mconf->ma_advertise_srvh = magd->srvid;
    /* Check if we have advertise set */
    if (mconf->ma_advertise_mode != ma_advertise_off &&
        mconf->ma_advertise_adrs) {
        rv = ma_group_join(mconf->ma_advertise_adrs, mconf->ma_advertise_port, mconf->ma_bind_adrs, mconf->ma_bind_port, pconf, s);
        if (rv != APR_SUCCESS) {
            ap_log_error(APLOG_MARK, APLOG_ERR, rv, s,
                         "mod_advertise: multicast join failed for %s:%d.",
                         mconf->ma_advertise_adrs, mconf->ma_advertise_port);
            ma_advertise_run = 0;
        }
        else {
            ma_advertise_run  = 1;
            ma_advertise_stat = 200;
        }
    }

    /* Fill default values */
    if (!mconf->ma_advertise_srvm)  {
        if (ma_server_rec && ma_server_rec->server_scheme) {
            /* ServerName scheme://fully-qualified-domain-name[:port] */
            mconf->ma_advertise_srvm = apr_pstrdup(pconf, ma_server_rec->server_scheme);
        } else {
            mconf->ma_advertise_srvm = apr_pstrdup(pconf, "http");
        }
    }

    if (mconf->ma_advertise_srvs == NULL && ma_server_rec) {
        /*
         * That is not easy just use ServerAdvertise with the server parameter
         * if the code below doesn't work
         */
        char *ptr = NULL;
        int port = DEFAULT_HTTP_PORT;
        if (ma_server_rec->addrs && ma_server_rec->addrs->host_addr &&
            ma_server_rec->addrs->host_addr->next == NULL) {
            ptr = apr_psprintf(pproc, "%pI", ma_server_rec->addrs->host_addr);
        }
        /* Use don't use any as local address too */
        if (ptr == NULL || strncmp(ptr,"0.0.0.0", 7) == 0 || strncmp(ptr,"::",2) == 0) {
            if  ( ma_server_rec->port == 0 || ma_server_rec->port == 1) {
                if (ma_server_rec->addrs->host_addr->port != 0)
                    port = ma_server_rec->addrs->host_addr->port;
            } else {
                port = ma_server_rec->port;
             }
            ptr = apr_psprintf(pproc, "%s:%lu", ma_server_rec->server_hostname, port);
        }
        rv = apr_parse_addr_port(&mconf->ma_advertise_srvs,
                                 &mconf->ma_advertise_srvi,
                                 &mconf->ma_advertise_srvp,
                                 ptr, pproc);
        if (rv != APR_SUCCESS || !mconf->ma_advertise_srvs ||
                                 !mconf->ma_advertise_srvp) {
            ap_log_error(APLOG_MARK, APLOG_CRIT, rv, s,
                         "mod_advertise: Invalid ServerAdvertise Address %s",
                         ptr);
            return rv;
        }
    }

    /* prevent X-Manager-Address: (null):0  */
    if (!mconf->ma_advertise_srvs || !mconf->ma_advertise_srvp) {
            ap_log_error(APLOG_MARK, APLOG_WARNING, 0, s,
                         "mod_advertise: ServerAdvertise Address or Port not defined, Advertise disabled!!!");
            return OK;
    }

    /* Create parent management thread */
    is_mp_running = 1;
    rv = apr_thread_create(&tp, NULL, parent_thread, server, pconf);
    if (rv != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_CRIT, rv, s,
                     "mod_advertise: parent apr_thread_create");
        return rv;
    }
    apr_thread_detach(tp);

    /* Create cleanup pool that will be destroyed first
     * in future use new apr_pool_pre_cleanup_register from APR 1.3
     */
    apr_pool_create(&magd->cpool, pconf);
    apr_pool_cleanup_register(magd->cpool, magd, pconfig_cleanup,
                              apr_pool_cleanup_null);

    ap_log_error(APLOG_MARK, APLOG_NOTICE, 0, s,
                 "Advertise initialized for process %" APR_PID_T_FMT,
                 getpid());

    apr_pool_cleanup_register(magd->ppool, magd, process_cleanup,
                              apr_pool_cleanup_null);



    return OK;
}

static void  child_init_hook(apr_pool_t *p, server_rec *s)
{
    main_server = s;
}

/*
 * Provide information for "status" logic
 */
static void advertise_info(request_rec *r)
{
    server_rec *s = main_server;
    /* Find the VirtualHost (Server) that does the Advertise */
    while (s) {
        void *sconf = s->module_config;
        mod_advertise_config *mconf = ap_get_module_config(sconf, &advertise_module);
        ap_rprintf(r, "Server: %s ", s->server_hostname);
        if (s->is_virtual && s->addrs) {
           server_addr_rec *srec = s->addrs;
           ap_rprintf(r, "VirtualHost: %s:%d", srec->virthost, srec->host_port);
        }
        if (mconf->ma_advertise_server != NULL) {
            ap_rprintf(r, " Advertising on Group %s Port %d ", mconf->ma_advertise_adrs, mconf->ma_advertise_port);
            ap_rprintf(r, "for %s://%s:%d every %d seconds<br/>",
                       mconf->ma_advertise_srvm, mconf->ma_advertise_srvs,
                       mconf-> ma_advertise_srvp,
                       apr_time_sec(mconf->ma_advertise_freq)
                       );
        } else {
            ap_rputs("<br/>", r);
        }
        s = s->next;
    }
}


/*--------------------------------------------------------------------------*/
/*                                                                          */
/* List of directives specific to our module.                               */
/*                                                                          */
/*--------------------------------------------------------------------------*/
static const command_rec cmd_table[] =
{
    AP_INIT_TAKE12(
        "ServerAdvertise",                  /* directive name               */
        cmd_advertise_m,                    /* config action routine        */
        NULL,                               /* argument to include in call  */
        RSRC_CONF,                          /* where available              */
        "Server advertise mode: On | Off [Address]"
    ),
    AP_INIT_TAKE1(
        "AdvertiseGroup",                   /* directive name               */
        cmd_advertise_g,                    /* config action routine        */
        NULL,                               /* argument to include in call  */
        RSRC_CONF,                          /* where available              */
        "Multicast group address"
    ),
    AP_INIT_TAKE1(
        "AdvertiseFrequency",               /* directive name               */
        cmd_advertise_f,                    /* config action routine        */
        NULL,                               /* argument to include in call  */
        RSRC_CONF,                          /* where available              */
        "Advertise frequency in seconds[.miliseconds]"
    ),
    AP_INIT_TAKE1(
        "AdvertiseSecurityKey",             /* directive name               */
        cmd_advertise_k,                    /* config action routine        */
        NULL,                               /* argument to include in call  */
        RSRC_CONF,                          /* where available              */
        "Advertise security key"
    ),
    AP_INIT_TAKE1(
        "AdvertiseManagerUrl",              /* directive name               */
        cmd_advertise_h,                    /* config action routine        */
        NULL,                               /* argument to include in call  */
        RSRC_CONF,                          /* where available              */
        "Advertise manager url"
    ),
    AP_INIT_TAKE1(
        "AdvertiseBindAddress",             /* directive name               */
        cmd_bindaddr,                       /* config action routine        */
        NULL,                               /* argument to include in call  */
        RSRC_CONF,                          /* where available              */
        "Local adress to bind to for Multicast logic"
    ),
    {NULL}

};

/*--------------------------------------------------------------------------*/
/*                                                                          */
/* Which functions are responsible for which hooks in the server.           */
/*                                                                          */
/*--------------------------------------------------------------------------*/
static void register_hooks(apr_pool_t *p)
{

    /* Post config handling
     */
    ap_hook_post_config(post_config_hook,
                        NULL,
                        NULL,
                        APR_HOOK_LAST);
    ap_hook_child_init(child_init_hook, NULL, NULL, APR_HOOK_MIDDLE);

    /* Provider for the "status" page */
    ap_register_provider(p, "advertise" , "info", "0", &advertise_info);

}

/* Create a default conf structure */
static void *create_advertise_server_config(apr_pool_t *p, server_rec *s)
{
    mod_advertise_config *mconf = apr_pcalloc(p, sizeof(*mconf));

    /* Set default values */
    mconf->ma_advertise_server  = NULL;
    mconf->ma_advertise_adrs = MA_DEFAULT_GROUP;
    mconf->ma_advertise_adsi = NULL;
    mconf->ma_advertise_srvm = NULL;
    mconf->ma_advertise_srvh = NULL;
    mconf->ma_advertise_srvs = NULL;
    mconf->ma_advertise_srvi = NULL;
    mconf->ma_advertise_uuid = NULL;

    mconf->ma_advertise_skey = NULL;

    mconf->ma_bind_set  = 0;
    mconf->ma_bind_adrs = NULL;
    mconf->ma_bind_adsi = NULL;
    mconf->ma_bind_port = MA_DEFAULT_ADVPORT;

    mconf->ma_advertise_port = MA_DEFAULT_ADVPORT;
    mconf->ma_advertise_srvp = 0;
    mconf->ma_advertise_mode = ma_advertise_on;
    mconf->ma_advertise_freq = MA_DEFAULT_ADV_FREQ;

    return mconf;
}

/*--------------------------------------------------------------------------*/
/*                                                                          */
/* The list of callback routines and data structures that provide           */
/* the static hooks into our module from the other parts of the server.     */
/*                                                                          */
/*--------------------------------------------------------------------------*/
module AP_MODULE_DECLARE_DATA advertise_module =
{
    STANDARD20_MODULE_STUFF,
    NULL,                    /* per-directory config creator                */
    NULL,                    /* dir config merger                           */
    create_advertise_server_config,                    /* server config creator                       */
    NULL,                     /* server config merger                        */
    cmd_table,               /* command table                               */
    register_hooks           /* set up other request processing hooks       */
};
