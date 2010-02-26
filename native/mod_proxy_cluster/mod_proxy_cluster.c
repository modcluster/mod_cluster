/*
 *  mod_cluster
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
 * @author Jean-Frederic Clere
 * @version $Revision$
 */

#include "apr_strings.h"
#include "apr_version.h"

#include "httpd.h"
#include "http_config.h"
#include "http_log.h"
#include "http_main.h"
#include "http_request.h"
#include "http_protocol.h"
#include "http_core.h"
#include "ap_mpm.h"
#include "mod_proxy.h"

#include "mod_proxy_cluster.h"

#include "slotmem.h"

#include "node.h"
#include "host.h"
#include "context.h"
#include "balancer.h"
#include "sessionid.h"
#include "domain.h"

#if APR_HAVE_UNISTD_H
/* for getpid() */
#include <unistd.h>
#endif

/* define HAVE_CLUSTER_EX_DEBUG to have extented debug in mod_cluster */
#define HAVE_CLUSTER_EX_DEBUG 0

struct proxy_cluster_helper {
    int count_active; /* currently active request using the worker */
#if AP_MODULE_MAGIC_AT_LEAST(20051115,4)
#else
    apr_interval_time_t ping_timeout;
    char ping_timeout_set;
#endif
 
};
typedef struct  proxy_cluster_helper proxy_cluster_helper;

static struct node_storage_method *node_storage = NULL; 
static struct host_storage_method *host_storage = NULL; 
static struct context_storage_method *context_storage = NULL; 
static struct balancer_storage_method *balancer_storage = NULL; 
static struct sessionid_storage_method *sessionid_storage = NULL; 
static struct domain_storage_method *domain_storage = NULL; 

static apr_thread_mutex_t *lock = NULL;

static server_rec *main_server = NULL;
#define CREAT_ALL  0 /* create balancers/workers in all VirtualHost */
#define CREAT_NONE 1 /* don't create balancers (but add workers) */
#define CREAT_ROOT 2 /* Only create balancers/workers in the main server */
static int creat_bal = CREAT_ROOT;

static int use_alias = 0; /* 1 : Compare Alias with server_name */

static apr_time_t lbstatus_recalc_time = apr_time_from_sec(5); /* recalcul the lbstatus based on number of request in the time interval */

#define WAITFORREMOVE 10 /* seconds */

#define TIMESESSIONID 300                    /* after 5 minutes the sessionid have probably timeout */
#define TIMEDOMAIN    300                    /* after 5 minutes the sessionid have probably timeout */

/* reslist constructor */
/* XXX: Should use the proxy_util one. */
static apr_status_t connection_constructor(void **resource, void *params,
                                           apr_pool_t *pool)
{
    apr_pool_t *ctx;
    apr_pool_t *scpool;
    proxy_conn_rec *conn;
    proxy_worker *worker = (proxy_worker *)params;

    /*
     * Create the subpool for each connection
     * This keeps the memory consumption constant
     * when disconnecting from backend.
     */
    apr_pool_create(&ctx, pool);
    conn = apr_pcalloc(pool, sizeof(proxy_conn_rec));

    /*
     * Create another subpool that manages the data for the
     * socket and the connection member of the proxy_conn_rec struct as we
     * destroy this data more frequently than other data in the proxy_conn_rec
     * struct like hostname and addr (at least in the case where we have
     * keepalive connections that timed out).
     */
#if AP_MODULE_MAGIC_AT_LEAST(20051115,13)
    apr_pool_create(&scpool, ctx);
    apr_pool_tag(scpool, "proxy_conn_scpool");
    conn->scpool = scpool;
#endif

    conn->pool   = ctx;
    conn->worker = worker;
#if APR_HAS_THREADS
    conn->inreslist = 1;
#endif
    *resource = conn;

    return APR_SUCCESS;
}

#if APR_HAS_THREADS /* only needed when threads are used */
/* reslist destructor */
/* XXX: Should use the proxy_util one. */
static apr_status_t connection_destructor(void *resource, void *params,
                                          apr_pool_t *pool)
{
    proxy_conn_rec *conn = (proxy_conn_rec *)resource;

    /* Destroy the pool only if not called from reslist_destroy */
    if (conn->worker->cp->pool) {
        apr_pool_destroy(conn->pool);
    }

    return APR_SUCCESS;
}
#endif

/* XXX: Should use the proxy_util one. */
#if APR_HAS_THREADS
static apr_status_t conn_pool_cleanup(void *theworker)
{
    proxy_worker *worker = (proxy_worker *)theworker;
    if (worker->cp->res) {
        worker->cp->pool = NULL;
    }
    return APR_SUCCESS;
}
#endif

/* XXX: Should use the proxy_util one. */
static void init_conn_pool(apr_pool_t *p, proxy_worker *worker)
{
    apr_pool_t *pool;
    proxy_conn_pool *cp;

    /*
     * Create a connection pool's subpool.
     * This pool is used for connection recycling.
     * Once the worker is added it is never removed but
     * it can be disabled.
     */
    apr_pool_create(&pool, p);
    apr_pool_tag(pool, "proxy_worker_cp");
    /*
     * Alloc from the same pool as worker.
     * proxy_conn_pool is permanently attached to the worker.
     */
    cp = (proxy_conn_pool *)apr_pcalloc(p, sizeof(proxy_conn_pool));
    cp->pool = pool;
    worker->cp = cp;
}

/*
 * Create/Get the worker before using it
 * XXX: Contains code of ap_proxy_initialize_worker (proxy_util.c)
 * XXX: If something goes wrong the worker can't be used and we leak memory... in a pool
 */
static apr_status_t create_worker(proxy_server_conf *conf, proxy_balancer *balancer,
                          server_rec *server, proxy_worker **worker,
                          nodeinfo_t *node, apr_pool_t *pool)
{
    char *url;
    char *ptr;
    int reuse = 0;
    apr_status_t rv = APR_SUCCESS;
#if AP_MODULE_MAGIC_AT_LEAST(20051115,4)
#else
    proxy_cluster_helper *helperping;
#endif
#if APR_HAS_THREADS
    int mpm_threads;
#endif

    /* build the name (scheme and port) when needed */
    url = apr_pstrcat(pool, node->mess.Type, "://", node->mess.Host, ":", node->mess.Port, NULL);

    *worker = ap_proxy_get_worker(conf->pool, conf, url);
    if ((*worker) == NULL) {

        /* creates it */ 
        proxy_cluster_helper *helper;
        const char *err = ap_proxy_add_worker(worker, conf->pool, conf, url);
        if (err) {
            ap_log_error(APLOG_MARK, APLOG_NOTICE|APLOG_NOERRNO, 0, server,
                         "Created: worker for %s failed: %s", url, err);
            return APR_EGENERAL;
        }
        (*worker)->opaque = apr_pcalloc(conf->pool,  sizeof(proxy_cluster_helper));
        if (!(*worker)->opaque)
            return APR_EGENERAL;
        helper = (*worker)->opaque;
        helper->count_active = 0;
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
                     "Created: worker for %s", url);
    } else  if ((*worker)->id == 0) {
        /* We are going to reuse a removed one */
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
                     "Created: reusing worker for %s", url);
        if ((*worker)->cp->pool == NULL) {
            init_conn_pool(conf->pool, *worker);
        }
        reuse = 1;
    } else {
        /* Check if the shared memory goes to the right place */
        char *pptr = (char *) node;
        pptr = pptr + node->offset;
        if ((*worker)->id == node->mess.id && (*worker)->s == (proxy_worker_stat *) pptr) {
            /* the share memory may have been removed and recreated */
            if (!(*worker)->s->status) {
                (*worker)->s->status = PROXY_WORKER_INITIALIZED;
                strncpy((*worker)->s->route, node->mess.JVMRoute, PROXY_WORKER_MAX_ROUTE_SIZ);
                (*worker)->s->route[PROXY_WORKER_MAX_ROUTE_SIZ] = '\0';
                /* XXX: We need that information from TC */
                (*worker)->s->redirect[0] = '\0';
                (*worker)->s->lbstatus = 0;
                (*worker)->s->lbfactor = -1; /* prevent using the node using status message */
            }
            return APR_SUCCESS; /* Done Already existing */
        }
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
                     "Created: can't reuse worker as it for %s cleaning...", url);
        if ((*worker)->cp->pool) {
            /* destroy and create a new one */
            apr_pool_destroy((*worker)->cp->pool);
            (*worker)->cp->pool = NULL;
            init_conn_pool(conf->pool, *worker);
        }
        reuse = 1;
    }

    /* Get the shared memory for this worker */
    ptr = (char *) node;
    ptr = ptr + node->offset;
    (*worker)->s = (proxy_worker_stat *) ptr;

    (*worker)->id = node->mess.id;
    (*worker)->route = apr_pstrdup(conf->pool, node->mess.JVMRoute);
    (*worker)->redirect = apr_pstrdup(conf->pool, "");
    (*worker)->smax = node->mess.smax;
    (*worker)->ttl = node->mess.ttl;
    if (node->mess.timeout) {
        (*worker)->timeout_set = 1;
        (*worker)->timeout = node->mess.timeout;
    }
    (*worker)->flush_packets = node->mess.flushpackets;
    (*worker)->flush_wait = node->mess.flushwait;
#if AP_MODULE_MAGIC_AT_LEAST(20051115,4)
    (*worker)->ping_timeout = node->mess.ping;
    (*worker)->ping_timeout_set = 1;
    (*worker)->acquire_set = 1;
#else
    helperping = (*worker)->opaque;
    helperping->ping_timeout = node->mess.ping;
    helperping->ping_timeout_set = 1;
#endif
    (*worker)->keepalive = 1;
    (*worker)->keepalive_set = 1;
    (*worker)->is_address_reusable = 1;
    (*worker)->acquire = apr_time_make(0, 2 * 1000); /* 2 ms */
    (*worker)->retry = apr_time_from_sec(PROXY_WORKER_DEFAULT_RETRY);

    /* from ap_proxy_initialize_worker() */
#if APR_HAS_THREADS
    ap_mpm_query(AP_MPMQ_MAX_THREADS, &mpm_threads);
    if (mpm_threads > 1) {
        /* Set hard max to no more then mpm_threads */
        if ((*worker)->hmax == 0 || (*worker)->hmax > mpm_threads) {
            (*worker)->hmax = mpm_threads;
        }
        if ((*worker)->smax == -1 || (*worker)->smax > (*worker)->hmax) {
            (*worker)->smax = (*worker)->hmax;
        }
        /* Set min to be lower then smax */
        if ((*worker)->min > (*worker)->smax) {
            (*worker)->min = (*worker)->smax;
        }
    }
    else {
        /* This will supress the apr_reslist creation */
        (*worker)->min = (*worker)->smax = (*worker)->hmax = 0;
    }

    if ((*worker)->hmax) {
        rv = apr_reslist_create(&((*worker)->cp->res),
                                (*worker)->min, (*worker)->smax,
                                (*worker)->hmax, (*worker)->ttl,
                                connection_constructor, connection_destructor,
                                (*worker), (*worker)->cp->pool);

        apr_pool_cleanup_register((*worker)->cp->pool, (void *)(*worker),
                                  conn_pool_cleanup,
                                  apr_pool_cleanup_null);

        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
            "proxy: initialized worker %d in child %" APR_PID_T_FMT " for (%s) min=%d max=%d smax=%d",
             (*worker)->id, getpid(), (*worker)->hostname, (*worker)->min,
             (*worker)->hmax, (*worker)->smax);

#if (APR_MAJOR_VERSION > 0)
        /* Set the acquire timeout */
        if (rv == APR_SUCCESS && (*worker)->acquire_set) {
            apr_reslist_timeout_set((*worker)->cp->res, (*worker)->acquire);
        }
#endif
    }
    else
#endif
    {

        rv = connection_constructor((void **)&((*worker)->cp->conn), (*worker), (*worker)->cp->pool);
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
             "proxy: initialized single connection worker %d in child %" APR_PID_T_FMT " for (%s)",
             (*worker)->id, getpid(), (*worker)->hostname);
    }
    /* end from ap_proxy_initialize_worker() */

    /*
     * The Shared datastatus may already contains a valid information
     */
    if (!(*worker)->s->status) {
        (*worker)->s->status = PROXY_WORKER_INITIALIZED;
        strncpy((*worker)->s->route, node->mess.JVMRoute, PROXY_WORKER_MAX_ROUTE_SIZ);
        (*worker)->s->route[PROXY_WORKER_MAX_ROUTE_SIZ] = '\0';
        /* XXX: We need that information from TC */
        (*worker)->s->redirect[0] = '\0';
        (*worker)->s->lbstatus = 0;
        (*worker)->s->lbfactor = -1; /* prevent using the node using status message */
    }

    if (!reuse) {
        /*
         * Create the corresponding balancer worker information
         * copying for proxy_util.c ap_proxy_add_worker_to_balancer
         */
        proxy_worker *runtime;
        runtime = apr_array_push(balancer->workers);
        memcpy(runtime, (*worker), sizeof(proxy_worker));
    } else {
        /* Update the corresponding balancer worker information */
        proxy_worker *runtime;
        int i;

        runtime = (proxy_worker *)balancer->workers->elts;
        for (i = 0; i < balancer->workers->nelts; i++, runtime++) {
            if (runtime->name) {
                if (strcmp(url, runtime->name) == 0) {
                    memcpy(runtime, (*worker), sizeof(proxy_worker));
                }
            }
        }
    }
    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
                 "Created: worker for %s %d (status): %d", url, (*worker)->id, (*worker)->s->status);
    return rv;
}

/**
 * Add balancer to the proxy_server_conf.
 * NOTE: pool is the request pool or any temporary pool. Use conf->pool for any data that live longer.
 * @param node the pointer to the node structure (contains the balancer information).
 * @param conf a proxy_server_conf.
 * @param balancer the balancer to update or NULL to create it.
 * @param name the name of the balancer.
 * @param pool a temporary pool.
 * @server the server rec for logging purposes.
 *
 */
static proxy_balancer *add_balancer_node(nodeinfo_t *node, proxy_server_conf *conf,
                                         apr_pool_t *pool, server_rec *server)
{
    proxy_balancer *balancer = NULL;
    char *name = apr_pstrcat(pool, "balancer://", node->mess.balancer, NULL);

    balancer = ap_proxy_get_balancer(pool, conf, name);
    if (!balancer) {
        ap_log_error(APLOG_MARK, APLOG_DEBUG|APLOG_NOERRNO, 0, server,
                      "add_balancer_node: Create balancer %s", name);
        balancer = apr_array_push(conf->balancers);
        memset(balancer, 0, sizeof(proxy_balancer));
        balancer->name = apr_pstrdup(conf->pool, name);
        balancer->lbmethod = ap_lookup_provider(PROXY_LBMETHOD, "byrequests", "0");
        balancer->workers = apr_array_make(conf->pool, 5, sizeof(proxy_worker));
        /* XXX Is this a right place to create mutex */
#if APR_HAS_THREADS
        if (apr_thread_mutex_create(&(balancer->mutex),
                    APR_THREAD_MUTEX_DEFAULT, conf->pool) != APR_SUCCESS) {
            /* XXX: Do we need to log something here */
            ap_log_error(APLOG_MARK, APLOG_NOTICE|APLOG_NOERRNO, 0, server,
                          "add_balancer_node: Can't create lock for balancer");
        }
#endif
    } else {
        ap_log_error(APLOG_MARK, APLOG_DEBUG|APLOG_NOERRNO, 0, server,
                      "add_balancer_node: Using balancer %s", name);
    }

    if (balancer && balancer->workers->nelts == 0) {
        /* Logic to copy the shared memory information to the balancer */
        int sizebal, i;
        int *bal;
        bal = apr_pcalloc(pool, sizeof(int) * balancer_storage->get_max_size_balancer());
        sizebal = balancer_storage->get_ids_used_balancer(bal);
        for (i=0; i<sizebal; i++) {
            balancerinfo_t *balan;
            balancer_storage->read_balancer(bal[i], &balan);
            /* Something like balancer://cluster1 and cluster1 */
            if (strcmp(balan->balancer, &balancer->name[11]) == 0) {
                /* XXX: StickySession, StickySessionRemove not in */
                balancer->sticky  = apr_psprintf(conf->pool, "%s|%s", balan->StickySessionCookie,
                                                 balan->StickySessionPath);
                balancer->sticky_force = 0;
                if (balan->StickySession)
                    balancer->sticky_force += STSESSION;
                if (balan->StickySessionForce)
                    balancer->sticky_force += STSESSFOR;
                if (balan->StickySessionRemove)
                    balancer->sticky_force += STSESSREM;
                balancer->timeout = balan->Timeout;

                balancer->max_attempts = balan->Maxattempts;
                balancer->max_attempts_set = 1;
                break;
            }
        }
    }
    return balancer;
}
/**
 * Add a node to the worker conf
 * NOTE: pool is the request pool or any temporary pool. Use conf->pool for any data that live longer.
 * @param node the pointer to the node structure
 * @param conf a proxy_server_conf.
 * @param balancer the balancer to update.
 * @param pool a temporary pool.
 * @server the server rec for logging purposes.
 *
 */
static void add_workers_node(nodeinfo_t *node, proxy_server_conf *conf, proxy_balancer *balancer,
                             apr_pool_t *pool, server_rec *server)
{
    proxy_worker *worker = NULL;
    create_worker(conf, balancer, server, &worker, node, pool);
}
/*
 * Adds the balancers and the workers to the VirtualHosts corresponding to node
 * Note that the calling routine should lock before calling us.
 * @param node the node information to add.
 * @param pool  temporary pool to use for temporary buffer.
 */
static void add_balancers_workers(nodeinfo_t *node, apr_pool_t *pool)
{
    server_rec *s = main_server;
    char *name = apr_pstrcat(pool, "balancer://", node->mess.balancer, NULL);
            
    while (s) {
        void *sconf = s->module_config;
        proxy_server_conf *conf = (proxy_server_conf *)ap_get_module_config(sconf, &proxy_module);
        proxy_balancer *balancer = ap_proxy_get_balancer(pool, conf, name);

        if (!balancer && (creat_bal == CREAT_NONE ||
            creat_bal == CREAT_ROOT && s!=main_server)) {
            s = s->next;
            continue;
        }
        if (!balancer)
            balancer = add_balancer_node(node, conf, pool, s);
        if (balancer)
            add_workers_node(node, conf, balancer, pool, s);
        s = s->next;
    }
}
/*
 * Remove a node from the worker conf
 */
static int remove_workers_node(nodeinfo_t *node, proxy_server_conf *conf, apr_pool_t *pool, server_rec *server)
{
    int i;
    proxy_cluster_helper *helper;
    proxy_worker *worker = (proxy_worker *)conf->workers->elts;
    for (i = 0; i < conf->workers->nelts; i++) {
        if (worker->id == node->mess.id)
            break;
        worker++;
    }
    if (i == conf->workers->nelts) {
        /* XXX: Another process may use it, can't do: node_storage->remove_node(node); */
        return 0; /* Done */
    }

    /* prevent other threads using it */
    worker->s->status |= PROXY_WORKER_IN_ERROR;

    /* apr_reslist_acquired_count */
    i = 0;

#if APU_MAJOR_VERSION > 1 || (APU_MAJOR_VERSION == 1 && APU_MINOR_VERSION >= 3)
    if (worker->cp->res)
        i = apr_reslist_acquired_count(worker->cp->res);
    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
             "remove_workers_node (reslist) %d %s", i, node->mess.JVMRoute);
#else
    helper = worker->opaque;
    if (helper) {
        i = helper->count_active;
    }
    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
             "remove_workers_node (helper) %d %s", i, node->mess.JVMRoute);
#endif

    if (i == 0) {
        /* No connection in use: clean the worker */
        proxy_balancer *balancer;
        char *name = apr_pstrcat(pool, "balancer://", node->mess.balancer, NULL); 

        /* mark the worker removed in the apr_array of the balancer */
        balancer = (proxy_balancer *)conf->balancers->elts;
        for (i = 0; i < conf->balancers->nelts; i++, balancer++) {
            if (strcmp(balancer->name, name) == 0) {
                int j;
                proxy_worker *searched = (proxy_worker *)balancer->workers->elts;
                for (j = 0; j < balancer->workers->nelts; j++, searched++) {
                    if (searched->id == worker->id) {
                        searched->id = 0; /* mark it removed */
                    }
                }
            }
        }

        /* Clear the connection pool (close the sockets) */
        if (worker->cp->pool) {
            apr_pool_destroy(worker->cp->pool);
            worker->cp->pool = NULL;
        }

        /* XXX: Shouldnn't we remove the mutex too (worker->mutex) */

        worker->id = 0; /* mark it removed */

        return (0);
    } else {
        node->mess.lastcleantry = apr_time_now();
        return (1); /* We should retry later */
    }
}
/*
 * Create/Remove workers corresponding to updated nodes.
 * NOTE: It is called from proxy_cluster_watchdog_func and other locations
 *       It shouldn't call worker_nodes_are_updated() because there may be several VirtualHosts.
 */
static void update_workers_node(proxy_server_conf *conf, apr_pool_t *pool, server_rec *server, int check)
{
    int *id, size, i;
    apr_time_t last;

    /* Check if we have to do something */
    apr_thread_mutex_lock(lock);
    if (check)
        last = node_storage->worker_nodes_need_update(main_server, pool);
    else
        last = 1;

    /* nodes_need_update will return 1 if last_updated is zero: first time we are called */
    if (last == 0) {
        apr_thread_mutex_unlock(lock);
        return;
    }

    /* read the ident of the nodes */
    id = apr_pcalloc(pool, sizeof(int) * node_storage->get_max_size_node());
    size = node_storage->get_ids_used_node(id);

    /* XXX: How to skip the balancer that aren't controled by mod_manager */

    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
             "update_workers_node starting");

    /* Only process the nodes that have been updated since our last update */
    for (i=0; i<size; i++) {
        nodeinfo_t *ou;
        if (node_storage->read_node(id[i], &ou) != APR_SUCCESS)
            continue;
        if (ou->updatetime >= last) {
            /* The node has changed */
            add_balancers_workers(ou, pool);
        } 
    } 

    apr_thread_mutex_unlock(lock);
    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
             "update_workers_node done");
}

static proxy_worker *get_worker_from_id(proxy_server_conf *conf, int id)
{
    int i;
    proxy_worker *worker;

    worker = (proxy_worker *)conf->workers->elts;
    for (i = 0; i < conf->workers->nelts; i++) {
        if (worker->id == id) {
            return worker;
        }
        worker++;
    }
    return NULL;
}

/*
 * Do a ping/pong to the node
 * XXX: ajp_handle_cping_cpong should come from a provider as
 * it is already in modules/proxy/ajp_utils.c
 */
static apr_status_t ajp_handle_cping_cpong(apr_socket_t *sock,
                                           request_rec *r,
                                           apr_interval_time_t timeout)
{
    char buf[5];
    apr_size_t written = 5;
    apr_interval_time_t org; 
    apr_status_t status;
    apr_status_t rv;

    /* built the cping message */
    buf[0] = 0x12;
    buf[1] = 0x34;
    buf[2] = (apr_byte_t) 0;
    buf[3] = (apr_byte_t) 1;
    buf[4] = (unsigned char)10;

    status = apr_socket_send(sock, buf, &written);
    if (status != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, status, NULL,
                      "ajp_cping_cpong(): send failed");
        return status;
    }
    status = apr_socket_timeout_get(sock, &org);
    if (status != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, status, NULL,
                      "ajp_cping_cpong(): apr_socket_timeout_get failed");
        return status;
    }
    status = apr_socket_timeout_set(sock, timeout);
    written = 5;
    status = apr_socket_recv(sock, buf, &written);
    if (status != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, 0, r->server,
               "ajp_cping_cpong: apr_socket_recv failed");
        goto cleanup;
    }
    if (buf[0] != 0x41 || buf[1] != 0x42 || buf[2] != 0 || buf[3] != 1  || buf[4] != (unsigned char)9) {
        ap_log_error(APLOG_MARK, APLOG_ERR, 0, r->server,
               "ajp_cping_cpong: awaited CPONG, received %02x %02x %02x %02x %02x",
               buf[0] & 0xFF,
               buf[1] & 0xFF,
               buf[2] & 0xFF,
               buf[3] & 0xFF,
               buf[4] & 0xFF);
        status = APR_EGENERAL;
    }
cleanup:
    rv = apr_socket_timeout_set(sock, org);
    if (rv != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, 0, r->server,
               "ajp_cping_cpong: apr_socket_timeout_set failed");
        return rv;
    }

    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                         "ajp_cping_cpong: Done");
    return status;
}
static apr_status_t proxy_cluster_try_pingpong(request_rec *r, proxy_worker *worker,
                                               char *url, proxy_server_conf *conf,
                                               apr_interval_time_t ping, apr_interval_time_t workertimeout)
{
    apr_status_t status;
    apr_interval_time_t timeout;
    apr_interval_time_t savetimeout;
    char savetimeout_set;
#if AP_MODULE_MAGIC_AT_LEAST(20051115,4)
#else
    proxy_cluster_helper *helperping;
#endif
    proxy_conn_rec *backend = NULL;
    char server_portstr[32];
    char *locurl = url;
    apr_uri_t *uri;

     /* create space for state information */
    status = ap_proxy_acquire_connection(worker->scheme, &backend, worker, r->server);
    if (status != OK) {
        if (backend) {
            backend->close_on_recycle = 1;
            ap_proxy_release_connection(worker->scheme, backend, r->server);
        }
        return status;
    }

    /* Step One: Determine Who To Connect To */
    uri = apr_palloc(r->pool, sizeof(*uri)); /* We don't use it anyway */
    status = ap_proxy_determine_connection(r->pool, r, conf, worker, backend,
                                           uri, &locurl, worker->hostname, worker->port,
                                           server_portstr,
                                           sizeof(server_portstr));
    if (status != OK) {
        ap_proxy_release_connection(worker->scheme, backend, r->server);
        return status;
    }

    /* Set the timeout: Note that the default timeout logic in the proxy_util.c is:
     * 1 - worker->timeout (if timeout_set timeout=n in the worker)
     * 2 - conf->timeout (if timeout_set ProxyTimeout 300)
     * 3 - s->timeout (TimeOut 300).
     * We hack it... Via 1
     * Since 20051115.16 (2.2.9) there is a conn_timeout and conn_timeout_set.
     * Changing the worker->timeout is a bad idea (we have to restore the value from the shared memory).
     */
#if AP_MODULE_MAGIC_AT_LEAST(20051115,4)
    timeout = worker->ping_timeout;
#else
    helperping = worker->opaque;
    timeout = helperping->ping_timeout;
#endif
    if (timeout <= 0)
        timeout =  apr_time_from_sec(10); /* 10 seconds */

#if AP_MODULE_MAGIC_AT_LEAST(20051115,16)
    if (!worker->conn_timeout_set) {
        savetimeout_set = worker->conn_timeout_set;
        savetimeout = worker->conn_timeout;
        worker->conn_timeout = timeout;
        worker->conn_timeout_set = 1;
    }
#else
    /* XXX: side effects the worker may be used in another socket */
    savetimeout_set = worker->timeout_set;
    savetimeout = worker->timeout;
    worker->timeout_set = 1;
    worker->timeout = timeout;
#endif

    /* Step Two: Make the Connection */
    status = ap_proxy_connect_backend(worker->scheme, backend, worker, r->server);
#if AP_MODULE_MAGIC_AT_LEAST(20051115,16)
    if (!savetimeout_set) {
        worker->conn_timeout = savetimeout;
        worker->conn_timeout_set = savetimeout_set;
    }
#else
    /* Restore the information from the node information */
    if (workertimeout) {
        worker->timeout = workertimeout;
        worker->timeout_set = 1;
    } else {
        worker->timeout_set = 0;
        worker->timeout = 0;
    }
#endif
    if (status != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                     "proxy_cluster_try_pingpong: can't connect to backend");
        ap_proxy_release_connection(worker->scheme, backend, r->server);
        return status;
    }

    /* XXX: For the moment we support only AJP */
    if (strcasecmp(worker->scheme, "AJP") == 0) {
        status = ajp_handle_cping_cpong(backend->sock, r, timeout);
        if (status != APR_SUCCESS) {
            ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                         "proxy_cluster_try_pingpong: cping_cpong failed");
            backend->close++;
        }
        
    }
    ap_proxy_release_connection(worker->scheme, backend, r->server);
    return status;
}

/*
 * update the lbfactor of each node if needed,
 */
static void update_workers_lbstatus(proxy_server_conf *conf, apr_pool_t *pool, server_rec *server)
{
    int *id, size, i;
    apr_time_t now;

    now = apr_time_now();

    /* read the ident of the nodes */
    id = apr_pcalloc(pool, sizeof(int) * node_storage->get_max_size_node());
    size = node_storage->get_ids_used_node(id);

    /* update lbstatus if needed */
    for (i=0; i<size; i++) {
        nodeinfo_t *ou;
        if (node_storage->read_node(id[i], &ou) != APR_SUCCESS)
            continue;
        if (ou->mess.remove)
            continue;
        if (ou->mess.updatetimelb < (now - lbstatus_recalc_time)) {
            /* The lbstatus needs to be updated */
            int elected, oldelected;
            proxy_worker_stat *stat;
            char *ptr = (char *) ou;
            ptr = ptr + ou->offset;
            stat = (proxy_worker_stat *) ptr;
            elected = stat->elected;
            oldelected = ou->mess.oldelected;
            ou->mess.updatetimelb = now;
            ou->mess.oldelected = elected;
            if (stat->lbfactor > 0)
                stat->lbstatus = ((elected - oldelected) * 1000) / stat->lbfactor;
            if (elected == oldelected) {
                /* lbstatus_recalc_time without changes: test for broken nodes */
                /* first get the worker, create a dummy request and do a ping  */
                char sport[7];
                char *url;
                apr_status_t rv;
                apr_pool_t *rrp;
                request_rec *rnew;
                proxy_worker *worker = get_worker_from_id(conf, id[i]);
                if (worker == NULL)
                    continue; /* skip it */
                apr_snprintf(sport, sizeof(sport), ":%d", worker->port);
                url = apr_pstrcat(pool, worker->scheme, "://", worker->hostname,  sport, "/", NULL);

                apr_pool_create(&rrp, pool);
                apr_pool_tag(rrp, "subrequest");
                rnew = apr_pcalloc(rrp, sizeof(request_rec));
                rnew->pool = rrp;
                /* we need only those ones */
                rnew->server = server;
                rnew->connection = apr_pcalloc(rrp, sizeof(conn_rec));
                rnew->per_dir_config = server->lookup_defaults;
                rv = proxy_cluster_try_pingpong(rnew, worker, url, conf, ou->mess.ping, ou->mess.timeout);
                if (rv != APR_SUCCESS) {
                    /* We can't reach the node */
                    worker->s->status |= PROXY_WORKER_IN_ERROR;
                    ou->mess.num_failure_idle++;
                    if (ou->mess.num_failure_idle > 60) {
                        /* Failing for 5 minutes: time to mark it removed */
                        ou->mess.remove = 1;
                        ou->updatetime = now;
                    } 
                } else
                    ou->mess.num_failure_idle = 0;
            } else
                ou->mess.num_failure_idle = 0;
        } 
    } 
}

/*
 * remove the sessionids that have timeout
 */
static void remove_timeout_sessionid(proxy_server_conf *conf, apr_pool_t *pool, server_rec *server)
{
    int *id, size, i;
    apr_time_t now;

    now = apr_time_sec(apr_time_now());

    /* read the ident of the sessionid */
    id = apr_pcalloc(pool, sizeof(int) * sessionid_storage->get_max_size_sessionid());
    size = sessionid_storage->get_ids_used_sessionid(id);

    /* update lbstatus if needed */
    for (i=0; i<size; i++) {
        sessionidinfo_t *ou;
        if (sessionid_storage->read_sessionid(id[i], &ou) != APR_SUCCESS)
            continue;
        if (ou->updatetime < (now - TIMESESSIONID)) {
            /* Remove it */
            sessionid_storage->remove_sessionid(ou);
        } 
    } 
}

/*
 * remove the domain that have timeout
 */
static void remove_timeout_domain(apr_pool_t *pool, server_rec *server)
{
    int *id, size, i;
    apr_time_t now;

    now = apr_time_sec(apr_time_now());

    /* read the ident of the domain */
    id = apr_pcalloc(pool, sizeof(int) * domain_storage->get_max_size_domain());
    size = domain_storage->get_ids_used_domain(id);

    for (i=0; i<size; i++) {
        domaininfo_t *ou;
        if (domain_storage->read_domain(id[i], &ou) != APR_SUCCESS)
            continue;
        if (ou->updatetime < (now - TIMEDOMAIN)) {
            /* Remove it */
            domain_storage->remove_domain(ou);
        } 
    } 
}

/* Retrieve the parameter with the given name
 * Something like 'JSESSIONID=12345...N'
 * XXX: Should use the mod_proxy_balancer ones.
 */
static char *get_path_param(apr_pool_t *pool, char *url,
                            const char *name)
{
    char *path = NULL;

    for (path = strstr(url, name); path; path = strstr(path + 1, name)) {
        path += strlen(name);
        if (*path == '=') {
            /*
             * Session path was found, get it's value
             */
            ++path;
            if (strlen(path)) {
                char *q;
                path = apr_strtok(apr_pstrdup(pool, path), "?&", &q);
                return path;
            }
        }
    }
    return NULL;
}

/*
 * Read the cookie corresponding to name
 * @param r request.
 * @param name name of the cookie
 * @param in tells if cookie should read from the request or the response
 * @return the value of the cookie
 */
static char *get_cookie_param(request_rec *r, const char *name, int in)
{
    const char *cookies;
    const char *start_cookie;

    if (in)
        cookies = apr_table_get(r->headers_in, "Cookie");
    else
        cookies = apr_table_get(r->headers_out, "Set-Cookie");
    if (cookies) {
        for (start_cookie = ap_strstr_c(cookies, name); start_cookie;
             start_cookie = ap_strstr_c(start_cookie + 1, name)) {
            if (start_cookie == cookies ||
                start_cookie[-1] == ';' ||
                start_cookie[-1] == ',' ||
                isspace(start_cookie[-1])) {

                start_cookie += strlen(name);
                while(*start_cookie && isspace(*start_cookie))
                    ++start_cookie;
                if (*start_cookie == '=' && start_cookie[1]) {
                    /*
                     * Session cookie was found, get it's value
                     */
                    char *end_cookie, *cookie;
                    ++start_cookie;
                    cookie = apr_pstrdup(r->pool, start_cookie);
                    if ((end_cookie = strchr(cookie, ';')) != NULL)
                        *end_cookie = '\0';
                    if((end_cookie = strchr(cookie, ',')) != NULL)
                        *end_cookie = '\0';
                    return cookie;
                }
            }
        }
    }
    return NULL;
}

/**
 * Check that the request has a sessionid with a route
 * @param r the request_rec.
 * @param stickyval the cookie or/and parameter name.
 * @param uri part of the URL to for the session parameter.
 * @param sticky_used the string that was used to find the route
 */
static char *cluster_get_sessionid(request_rec *r, const char *stickyval, char *uri, char **sticky_used)
{
    char *sticky, *sticky_path;
    char *path;
    char *route;

    /* for 2.2.x the sticky parameter may contain 2 values */
    sticky = sticky_path = apr_pstrdup(r->pool, stickyval);
    if ((path = strchr(sticky, '|'))) {
        *path++ = '\0';
         sticky_path = path;
    }
    *sticky_used = sticky_path;
    route = get_path_param(r->pool, uri , sticky_path);
    if (!route) {
        route = get_cookie_param(r, sticky, 1);
        *sticky_used = sticky;
    }
    return route;
}

/**
 * Check that the request has a sessionid (even invalid)
 * @param r the request_rec.
 * @param balancer_name name of the balancer. (to find the balancer)
 * @param conf the proxy configuration. (Could be null 
 * @param balance the balancer (balancer to use).
 * @return 1 is it finds a sessionid 0 otherwise.
 */
static int hassession_byname(request_rec *r, char *balancer_name, proxy_server_conf *conf, proxy_balancer *balance)
{
    proxy_balancer *balancer = balance;
    char *sessionid;
    char *uri;
    char *sticky_used;
    int i;

    if (conf == NULL) {
         void *sconf = r->server->module_config;
         conf = (proxy_server_conf *) ap_get_module_config(sconf, &proxy_module);
    }

    if (balancer == NULL) {
        balancer = (proxy_balancer *)conf->balancers->elts;
        for (i = 0; i < conf->balancers->nelts; i++, balancer++) {
            if (strlen(balancer->name) > 11 && strcasecmp(&balancer->name[11], balancer_name) == 0)
                break;
        }
        if (i == conf->balancers->nelts)
            balancer = NULL;
    }

    /* XXX: We don't find the balancer, that is BAD */
    if (balancer == NULL)
        return 0;

    if (balancer->sticky == NULL)
        return 0;

    if (r->filename)
        uri = r->filename + 6;
    else {
        /* We are coming from proxy_cluster_trans */
        uri = r->unparsed_uri;
    }

    sessionid = cluster_get_sessionid(r, balancer->sticky, uri, &sticky_used);
    if (sessionid) {
#if HAVE_CLUSTER_EX_DEBUG
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                     "mod_proxy_cluster: found sessionid %s", sessionid);
#endif
        return 1;
    }
    return 0;
}

/**
 * Find/Check the balancer corresponding to the request and the node.
 * @param r the request_rec.
 * @param node the node to use.
 * @param conf the proxy configuration.
 * @param balance the balancer (balancer to use in that case we check it).
 * @return the name of the balancer or NULL if not found/not corresponding
 */


static char *get_balancer_by_node(request_rec *r, nodeinfo_t *node, proxy_server_conf *conf, proxy_balancer *balance)
{ 
    int i;
    int sizevhost;
    int *vhosts;

    /*
     * check the hosts and contexts
     * A node may have several virtual hosts and
     * each virtual hosts may have several context
     */
    sizevhost = host_storage->get_max_size_host();
    vhosts =  apr_palloc(r->pool, sizeof(int)*sizevhost);
    sizevhost = host_storage->get_ids_used_host(vhosts);
#if HAVE_CLUSTER_EX_DEBUG
    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                 "get_balancer_by_node testing node %s for %s", node->mess.JVMRoute, r->uri);
#endif
    for (i=0; i<sizevhost; i++) {
        hostinfo_t *vhost;
        host_storage->read_host(vhosts[i], &vhost);
        if (vhost->node == node->mess.id) {
            int j;
            int sizecontext = context_storage->get_max_size_context();
            int *contexts =  apr_palloc(r->pool, sizeof(int)*sizecontext);
            int *root =  apr_palloc(r->pool, sizeof(int)*sizevhost);
            int froot = 0;
            int hasnonroot = 0;

            /* Check the virtual host */
            if (use_alias) {
#if HAVE_CLUSTER_EX_DEBUG
                ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                             "get_balancer_by_node testing ServerName: %s host: %s", ap_get_server_name(r), vhost->host);
#endif
                if (strcmp(ap_get_server_name(r), vhost->host) != 0)
                    continue;
            }

            /* Check the contexts */
            sizecontext = context_storage->get_ids_used_context(contexts);
#if HAVE_CLUSTER_EX_DEBUG
            ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                         "get_balancer_by_node testing host %s", vhost->host);
#endif
            for (j=0; j<sizecontext; j++) {
                contextinfo_t *context;
                int len;
                context_storage->read_context(contexts[j], &context);
                if (context->vhost != vhost->vhost || (context->node != node->mess.id))
                    continue;
#if HAVE_CLUSTER_EX_DEBUG
                ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                             "get_balancer_by_node testing context %s", context->context);
#endif

                /* check for /context[/] in the URL */
                len = strlen(context->context);
                if (len==1 && context->context[0] == '/' && froot<sizevhost) {
                    root[froot] = contexts[j];
                    froot++;
                    continue;
                }
                if (strncmp(r->uri, context->context, len) == 0) {
                    if (r->uri[len] == '\0' || r->uri[len] == '/') {
                        hasnonroot = 1;
                        /* Check status */
                        switch (context->status)
                        {
                          case ENABLED: 
                                ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                                         "get_balancer_by_node found context %s", context->context);
                            apr_table_setn(r->subprocess_env, "BALANCER_CONTEXT_ID", apr_psprintf(r->pool, "%d", context->id));
                            return node->mess.balancer;
                            break;
                          case DISABLED:
                            /* Only the request with sessionid ok for it */
                            if (hassession_byname(r, node->mess.balancer, conf, NULL)) {
                                ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                                             "get_balancer_by_node found (DISABLED) context %s", context->context);
                                apr_table_setn(r->subprocess_env, "BALANCER_CONTEXT_ID", apr_psprintf(r->pool, "%d", context->id));
                                return node->mess.balancer;
                            }
                            break;
                        }
                    }
                } 
            }
            /* Check ROOT contexts at last */
            if (hasnonroot)
                continue;
            for (j=0; j<froot; j++) {
                contextinfo_t *context;
                context_storage->read_context(root[j], &context);
                /* Check status */
                switch (context->status)
                {
                  case ENABLED: 
                    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                             "get_balancer_by_node found context %s", context->context);
                    apr_table_setn(r->subprocess_env, "BALANCER_CONTEXT_ID", apr_psprintf(r->pool, "%d", context->id));
                    return node->mess.balancer;
                    break;
                  case DISABLED:
                    /* Only the request with sessionid ok for it */
                    if (hassession_byname(r, node->mess.balancer, conf, NULL)) {
                        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                                     "get_balancer_by_node found (DISABLED) context %s", context->context);
                        apr_table_setn(r->subprocess_env, "BALANCER_CONTEXT_ID", apr_psprintf(r->pool, "%d", context->id));
                        return node->mess.balancer;
                    }
                    break;
                }
            }
        }
    }
    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                 "get_balancer_by_node balancer NOT found");
    return NULL;

}
/**
 * Search the balancer that corresponds to the pair context/host
 * @param r the request_rec.
 * @balancer proxy_ ARF....
 * @return the balancer or NULL if not found.
 */ 
static char *get_context_host_balancer(request_rec *r)
{
    void *sconf = r->server->module_config;
    proxy_server_conf *conf = (proxy_server_conf *)
        ap_get_module_config(sconf, &proxy_module);

    int sizenode = node_storage->get_max_size_node();
    int n;
    int *nodes =  apr_palloc(r->pool, sizeof(int)*sizenode);
    sizenode = node_storage->get_ids_used_node(nodes);
    for (n=0; n<sizenode; n++) {
        nodeinfo_t *node;
        char *ret;
        if (node_storage->read_node(nodes[n], &node) != APR_SUCCESS)
            continue;
        ret = get_balancer_by_node(r, node, conf, NULL);
        if (ret) {
            /* Check that it is in our proxy_server_conf */
            char *name = apr_pstrcat(r->pool, "balancer://", ret, NULL);
            proxy_balancer *balancer = ap_proxy_get_balancer(r->pool, conf, name);
            if (balancer)
                return ret;
            else
                 ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                             "get_context_host_balancer: balancer %s not found", name);
        }
    }
    return NULL;
}
/*
 * Check that the worker will handle the host/context.
 * The id of the worker is used to find the (slot) node in the shared
 * memory.
 * (See get_context_host_balancer too).
 */ 
static int iscontext_host_ok(request_rec *r, proxy_balancer *balancer, nodeinfo_t *node)
{
    char *balancername = get_balancer_by_node(r, node, NULL, balancer);
    if (balancername != NULL) {
        return 1; /* Found */
    }
    return 0;
}

/*
 * Check that the worker corresponds to a node that belongs to the same domain according to the JVMRoute.
 */ 
static int isnode_domain_ok(request_rec *r, nodeinfo_t *node,
                             const char *domain)
{
#if HAVE_CLUSTER_EX_DEBUG
    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                     "isnode_domain_ok: domain %s:%s", domain, node->mess.Domain);
#endif
    if (domain == NULL)
        return 1; /* OK no domain in the corresponding to the SESSIONID */
    if (strcmp(node->mess.Domain, domain) == 0)
        return 1; /* OK */
    return 0;
}

/*
 * The ModClusterService from the cluster fills the lbfactor values.
 * Our logic is a bit different the mod_balancer one. We check the
 * context and host to prevent to route to application beeing redeploy or
 * stopped in one node but not in others.
 * We also try the domain.
 */
static proxy_worker *internal_find_best_byrequests(proxy_balancer *balancer, proxy_server_conf *conf,
                                         request_rec *r, const char *domain, int failoverdomain)
{
    int i;
    proxy_worker *worker;
    proxy_worker *mycandidate = NULL;
    int checking_standby = 0;
    int checked_standby = 0;
    int checked_domain = 1;

#if HAVE_CLUSTER_EX_DEBUG
    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                 "proxy: Entering byrequests for CLUSTER (%s)",
                 balancer->name);
#endif

    /* create workers for new nodes */
    update_workers_node(conf, r->pool, r->server, 1);

    /* First try to see if we have available candidate */
    if (domain && strlen(domain)>0)
        checked_domain = 0;
    while (!checked_standby) {
        worker = (proxy_worker *)balancer->workers->elts;
        for (i = 0; i < balancer->workers->nelts; i++, worker++) {
            nodeinfo_t *node;
            if (worker->id == 0)
                continue; /* marked removed */

            /* standby logic
             * lbfactor: -1 broken node.
             *            0 standby.
             *           >0 factor to use.
             */
            if (worker->s->lbfactor < 0 || (worker->s->lbfactor == 0 && !checking_standby))
                continue;

            /* If the worker is in error state the STATUS logic will retry it */
            if (!PROXY_WORKER_IS_USABLE(worker)) {
                continue;
            }

            /* Take into calculation only the workers that are
             * not in error state or not disabled.
             * and that can map the context.
             */
            if (node_storage->read_node(worker->id, &node) != APR_SUCCESS)
                continue; /* Can't read node */

            if (PROXY_WORKER_IS_USABLE(worker) && iscontext_host_ok(r, balancer, node)) {
                if (!checked_domain) {
                    /* First try only nodes in the domain */
                    if (!isnode_domain_ok(r, node, domain)) {
                        continue;
                    }
                }
                if (worker->s->lbfactor == 0 && checking_standby) {
                    mycandidate = worker;
                    break; /* Done */
                } else {
                    if (!mycandidate)
                        mycandidate = worker;
                    else {
                        nodeinfo_t *node1;
                        int lbstatus, lbstatus1;

                        if (node_storage->read_node(mycandidate->id, &node1) != APR_SUCCESS)
                            continue;
                        lbstatus1 = ((mycandidate->s->elected - node1->mess.oldelected) * 1000)/mycandidate->s->lbfactor;
                        lbstatus  = ((worker->s->elected - node->mess.oldelected) * 1000)/worker->s->lbfactor;
                        lbstatus1 = lbstatus1 + mycandidate->s->lbstatus;
                        lbstatus = lbstatus + worker->s->lbstatus;
                        if (lbstatus1> lbstatus) 
                            mycandidate = worker;
                    }
                }
            }
        }
        if (mycandidate)
            break;
        if (checked_domain) {
            if (failoverdomain)
                break; /* We only failover in the domain */
            checked_standby = checking_standby++;
        }
        checked_domain++;
    }

    if (mycandidate) {
        /* Failover in domain */
        if (!checked_domain)
            apr_table_setn(r->notes, "session-domain-ok", "1");
        mycandidate->s->elected++;
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                             "proxy: byrequests balancer DONE (%s)", mycandidate->name);
    } else {
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                             "proxy: byrequests balancer FAILED");
    }
    return mycandidate;
}
/*
 * Wrapper to mod_balancer "standard" interface.
 */
static proxy_worker *find_best_byrequests(proxy_balancer *balancer,
                                         request_rec *r)
{
    void *sconf = r->server->module_config;
    proxy_server_conf *conf = (proxy_server_conf *)
        ap_get_module_config(sconf, &proxy_module);
    return internal_find_best_byrequests(balancer, conf, r, NULL, 0);
}

/*
 * Check that we could connect to the node and create corresponding balancers and workers.
 * id   : worker id
 * load : load factor from the cluster manager.
 * load > 0  : a load factor.
 * load = 0  : standby worker.
 * load = -1 : errored worker.
 * load = -2 : just do a cping/cpong. 
 */
static int proxy_node_isup(request_rec *r, int id, int load)
{
    apr_status_t rv;
    proxy_worker *worker = NULL;
    server_rec *s = main_server;
    proxy_server_conf *conf = NULL;
    nodeinfo_t *node;

    if (node_storage->read_node(id, &node) != APR_SUCCESS)
        return 500;

    /* create the balancers and workers (that could be the first time) */
    apr_thread_mutex_lock(lock);
    add_balancers_workers(node, r->pool);
    apr_thread_mutex_unlock(lock);

    /* search for the worker in the VirtualHosts */ 
    while (s) {
        void *sconf = s->module_config;
        conf = (proxy_server_conf *) ap_get_module_config(sconf, &proxy_module);

        worker = get_worker_from_id(conf, id);
        if (worker != NULL)
            break;
        s = s->next;
    }
    if (worker == NULL) {
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                     "proxy_cluster_isup: Can't find worker for %d", id);
        return 500;
    }

    /* Try a  ping/pong to check the node */
    if (load > 0 || load == -2) {
        /* Only try usuable nodes */
        char sport[7];
        char *url;
        apr_snprintf(sport, sizeof(sport), ":%d", worker->port);
        url = apr_pstrcat(r->pool, worker->scheme, "://", worker->hostname,  sport, "/", NULL);
        rv = proxy_cluster_try_pingpong(r, worker, url, conf, node->mess.ping, node->mess.timeout);
        if (rv != APR_SUCCESS) {
            worker->s->status |= PROXY_WORKER_IN_ERROR;
            ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                         "proxy_cluster_isup: pingpong failed");
            return 500;
        }
    }
    if (load == -2) {
        return 0;
    }
    else if (load == -1) {
        worker->s->status |= PROXY_WORKER_IN_ERROR;
    }
    else if (load == 0) {
        /*
         * XXX: PROXY_WORKER_HOT_STANDBY Doesn't look supported
         * mark worker in error for the moment
         */
        worker->s->status |= PROXY_WORKER_IN_ERROR;
#if AP_MODULE_MAGIC_AT_LEAST(20051115,4)
        worker->s->status |= PROXY_WORKER_HOT_STANDBY;
#endif
    }
    else {
        worker->s->status &= ~PROXY_WORKER_IN_ERROR;
        worker->s->status &= ~PROXY_WORKER_STOPPED;
        worker->s->status &= ~PROXY_WORKER_DISABLED;
#if AP_MODULE_MAGIC_AT_LEAST(20051115,4)
        worker->s->status &= ~PROXY_WORKER_HOT_STANDBY;
#endif
        worker->s->lbfactor = load;
    }
    return 0;
}
static int proxy_host_isup(request_rec *r, char *scheme, char *host, char *port)
{
    apr_socket_t *sock;
    apr_sockaddr_t *to;
    apr_status_t rv;
    int nport = atoi(port);

    rv = apr_socket_create(&sock, APR_INET, SOCK_STREAM, 0, r->pool);
    if (rv != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                     "proxy_host_isup: pingpong (apr_socket_create) failed");
        return 500; 
    }
    rv = apr_sockaddr_info_get(&to, host, APR_INET, nport, 0, r->pool);
    if (rv != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                     "proxy_host_isup: pingpong (apr_sockaddr_info_get(%s, %d)) failed", host, nport);
        return 500; 
    }

    rv = apr_socket_connect(sock, to);
    if (rv != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                     "proxy_host_isup: pingpong (apr_socket_connect) failed");
        return 500; 
    }

    /* XXX: For the moment we support only AJP */
    if (strcasecmp(scheme, "AJP") == 0) {
        rv = ajp_handle_cping_cpong(sock, r, apr_time_from_sec(10));
        if (rv!= APR_SUCCESS) {
            ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                         "proxy_host_isup: cping_cpong failed");
            return 500;
        }
    } else {
            ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                         "proxy_host_isup: %s no yet supported", scheme);
    }

    apr_socket_close(sock);
    return 0;
}
/*
 * For the provider
 */
static const struct balancer_method balancerhandler =
{
    proxy_node_isup,
    proxy_host_isup
};

/*
 * Remove node that have beeen marked removed for more than 10 seconds.
 */
static void remove_removed_node(apr_pool_t *pool, server_rec *server)
{
    int *id, size, i;
    apr_time_t now = apr_time_now();

    /* read the ident of the nodes */
    id = apr_pcalloc(pool, sizeof(int) * node_storage->get_max_size_node());
    size = node_storage->get_ids_used_node(id);
    for (i=0; i<size; i++) {
        nodeinfo_t *ou;
        if (node_storage->read_node(id[i], &ou) != APR_SUCCESS)
            continue;
        if (ou->mess.remove && (now - ou->updatetime) >= apr_time_from_sec(WAITFORREMOVE) &&
            (now - ou->mess.lastcleantry) >= apr_time_from_sec(WAITFORREMOVE)) {
            /* if it has a domain store it in the domain */
#if HAVE_CLUSTER_EX_DEBUG
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
                     "remove_removed_node: %s %s %s", ou->mess.JVMRoute, ou->mess.balancer, ou->mess.Domain);
#endif
            if (ou->mess.Domain[0] != '\0') {
                domaininfo_t dom;
                strcpy(dom.JVMRoute, ou->mess.JVMRoute);
                strcpy(dom.balancer, ou->mess.balancer);
                strcpy(dom.domain, ou->mess.Domain);
                if (domain_storage->insert_update_domain(&dom)!=APR_SUCCESS) {
                    remove_timeout_domain(pool, server);
                    domain_storage->insert_update_domain(&dom);
                }
            }
            /* remove the node from the shared memory */
            node_storage->remove_host_context(ou->mess.id, pool);
            node_storage->remove_node(ou);
        }
    }
}
static void remove_workers_nodes(proxy_server_conf *conf, apr_pool_t *pool, server_rec *server)
{
    int *id, size, i;
    apr_thread_mutex_lock(lock);

    /* read the ident of the nodes */
    id = apr_pcalloc(pool, sizeof(int) * node_storage->get_max_size_node());
    size = node_storage->get_ids_used_node(id);
    for (i=0; i<size; i++) {
        nodeinfo_t *ou;
        if (node_storage->read_node(id[i], &ou) != APR_SUCCESS)
            continue;
        if (ou->mess.remove)
            remove_workers_node(ou, conf, pool, server);
    }
    apr_thread_mutex_unlock(lock);
}
static void * APR_THREAD_FUNC proxy_cluster_watchdog_func(apr_thread_t *thd, void *data)
{
    apr_pool_t *pool;
    for (;;) {
        server_rec *s = main_server;
        void *sconf = s->module_config;
        proxy_server_conf *conf = (proxy_server_conf *)
            ap_get_module_config(sconf, &proxy_module);
        apr_time_t last;

        apr_sleep(apr_time_make(1, 0));
        apr_pool_create(&pool, conf->pool);
        last = node_storage->worker_nodes_need_update(main_server, pool);
        while (s) {
            sconf = s->module_config;
            conf = (proxy_server_conf *)
                ap_get_module_config(sconf, &proxy_module);

            /* Create new workers if the shared memory changes */
            if (last)
                update_workers_node(conf, pool, s, 0);
            /* removed nodes: check for workers */
            remove_workers_nodes(conf, pool, s);
            /* cleanup removed node in shared memory */
            remove_removed_node(pool, s);
            /* Calculate the lbstatus for each node */
            update_workers_lbstatus(conf, pool, s);
            /* Free sessionid slots */
            if (sessionid_storage)
                remove_timeout_sessionid(conf, pool, s);
            s = s->next;
        }
        apr_pool_destroy(pool);
        if (last)
            node_storage->worker_nodes_are_updated(main_server);
    }
    apr_thread_exit(thd, 0);
    return NULL;
}

/*
 * Create a thread per process to make maintenance task.
 * and the mutex of the node creation.
 */
static void  proxy_cluster_child_init(apr_pool_t *p, server_rec *s)
{
    apr_status_t rv;
    apr_thread_t *wdt;

    main_server = s;

    rv = apr_thread_mutex_create(&lock, APR_THREAD_MUTEX_DEFAULT, p);
    if (rv != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR|APLOG_NOERRNO, 0, s,
                    "proxy_cluster_child_init: apr_thread_mutex_create failed");
    }

    rv = apr_thread_create(&wdt, NULL, proxy_cluster_watchdog_func, s, p);
    if (rv != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR|APLOG_NOERRNO, 0, s,
                    "proxy_cluster_child_init: apr_thread_create failed");
    }

}

static int proxy_cluster_post_config(apr_pool_t *p, apr_pool_t *plog,
                                     apr_pool_t *ptemp, server_rec *s)
{
    const char *userdata_key = "mod_cluster_init";
    void *data;

    apr_pool_userdata_get(&data, userdata_key, s->process->pool);
    if (data && sessionid_storage) {
        int nb_sessionid = sessionid_storage->get_max_size_sessionid();
        if (! nb_sessionid)
            sessionid_storage = NULL; /* don't use it */
    }

    /* Check that the mod_proxy_balancer.c is not loaded */
    if (ap_find_linked_module("mod_proxy_balancer.c") != NULL) {
       ap_log_error(APLOG_MARK, APLOG_ERR, 0, s,
                     "Module mod_proxy_balancer is loaded"
                     " it must be removed  in order for mod_proxy_cluster to function properly");
        return HTTP_INTERNAL_SERVER_ERROR;
    }

    node_storage = ap_lookup_provider("manager" , "shared", "0");
    if (node_storage == NULL) {
        ap_log_error(APLOG_MARK, APLOG_ERR|APLOG_NOERRNO, 0, s,
                    "proxy_cluster_post_config: Can't find mod_manager for nodes");
        return !OK;
    }
    host_storage = ap_lookup_provider("manager" , "shared", "1");
    if (host_storage == NULL) {
        ap_log_error(APLOG_MARK, APLOG_ERR|APLOG_NOERRNO, 0, s,
                    "proxy_cluster_post_config: Can't find mod_manager for hosts");
        return !OK;
    }
    context_storage = ap_lookup_provider("manager" , "shared", "2");
    if (context_storage == NULL) {
        ap_log_error(APLOG_MARK, APLOG_ERR|APLOG_NOERRNO, 0, s,
                    "proxy_cluster_post_config: Can't find mod_manager for contexts");
        return !OK;
    }
    balancer_storage = ap_lookup_provider("manager" , "shared", "3");
    if (balancer_storage == NULL) {
        ap_log_error(APLOG_MARK, APLOG_ERR|APLOG_NOERRNO, 0, s,
                    "proxy_cluster_post_config: Can't find mod_manager for balancers");
        return !OK;
    }
    sessionid_storage = ap_lookup_provider("manager" , "shared", "4");
    if (sessionid_storage == NULL) {
        ap_log_error(APLOG_MARK, APLOG_ERR|APLOG_NOERRNO, 0, s,
                    "proxy_cluster_post_config: Can't find mod_manager for sessionids");
        return !OK;
    }
    domain_storage = ap_lookup_provider("manager" , "shared", "5");
    if (domain_storage == NULL) {
        ap_log_error(APLOG_MARK, APLOG_ERR|APLOG_NOERRNO, 0, s,
                    "proxy_cluster_post_config: Can't find mod_manager for domains");
        return !OK;
    }

    /* Add version information */
    ap_add_version_component(p, MOD_CLUSTER_EXPOSED_VERSION);
    return OK;
}

/* Given the route find the corresponding domain (if there is a domain) */
static apr_status_t find_nodedomain(request_rec *r, char **domain, char *route, const char *balancer)
{
    nodeinfo_t *ou;
    domaininfo_t *dom;
#if HAVE_CLUSTER_EX_DEBUG
    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                 "find_nodedomain: finding node for %s: %s", route, balancer);
#endif
    if (node_storage->find_node(&ou, route) == APR_SUCCESS) {
        if (!strcmp(balancer, ou->mess.balancer)) {
            if (ou->mess.Domain[0] != '\0') {
                *domain = ou->mess.Domain;
            }
            return APR_SUCCESS;
        }
    }

#if HAVE_CLUSTER_EX_DEBUG
    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                 "find_nodedomain: finding domain for %s: %s", route, balancer);
#endif
    /* We can't find the node, because it was removed... */
    if (domain_storage->find_domain(&dom, route, balancer ) == APR_SUCCESS) {
        *domain = dom->domain;
        return APR_SUCCESS;
    }
    return APR_NOTFOUND;
}

/**
 * Find the balancer corresponding to the node information
 */
static const char *get_route_balancer(request_rec *r, proxy_server_conf *conf)
{
    proxy_balancer *balancer;
    char *route = NULL;
    char *sessionid = NULL;
    char *sticky_used;
    int i;

    balancer = (proxy_balancer *)conf->balancers->elts;
    for (i = 0; i < conf->balancers->nelts; i++, balancer++) {
        
        if (balancer->sticky == NULL)
            continue;
        if (strlen(balancer->name)<=11)
            continue;

        sessionid = cluster_get_sessionid(r, balancer->sticky, r->uri, &sticky_used);
        if (sessionid) {
            ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                         "cluster: Found value %s for "
                         "stickysession %s", sessionid, balancer->sticky);
            if ((route = strchr(sessionid, '.')) != NULL )
                route++;
            if (route && *route) {
                char *domain = NULL;
#if HAVE_CLUSTER_EX_DEBUG
                ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                                          "cluster: Found route %s", route);
#endif
                if (find_nodedomain(r, &domain, route, &balancer->name[11]) == APR_SUCCESS) {
#if HAVE_CLUSTER_EX_DEBUG
                    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                                "cluster: Found balancer %s for %s", &balancer->name[11], route);
#endif
                    /* here we have the route and domain for find_session_route ... */
                    apr_table_setn(r->notes, "session-sticky", sticky_used);
                    if (sessionid_storage)
                        apr_table_setn(r->notes, "session-id", sessionid);
                    apr_table_setn(r->notes, "session-route", route);

                    apr_table_setn(r->subprocess_env, "BALANCER_SESSION_ROUTE", route);
                    apr_table_setn(r->subprocess_env, "BALANCER_SESSION_STICKY", sticky_used);
                    if (domain) {
#if HAVE_CLUSTER_EX_DEBUG
                        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                                    "cluster: Found domain %s for %s", domain, route);
#endif
                        apr_table_setn(r->notes, "CLUSTER_DOMAIN", domain);
                    }
                    return &balancer->name[11];
                }
            }
        }
    }
    return NULL;
}

/* Copied from httpd mod_proxy */
static const char *proxy_interpolate(request_rec *r, const char *str)
{
    /* Interpolate an env str in a configuration string
     * Syntax ${var} --> value_of(var)
     * Method: replace one var, and recurse on remainder of string
     * Nothing clever here, and crap like nested vars may do silly things
     * but we'll at least avoid sending the unwary into a loop
     */
    const char *start;
    const char *end;
    const char *var;
    const char *val;
    const char *firstpart;

    start = ap_strstr_c(str, "${");
    if (start == NULL) {
        return str;
    }
    end = ap_strchr_c(start+2, '}');
    if (end == NULL) {
        return str;
    }
    /* OK, this is syntax we want to interpolate.  Is there such a var ? */
    var = apr_pstrndup(r->pool, start+2, end-(start+2));
    val = apr_table_get(r->subprocess_env, var);
    firstpart = apr_pstrndup(r->pool, str, (start-str));

    if (val == NULL) {
        return apr_pstrcat(r->pool, firstpart,
                           proxy_interpolate(r, end+1), NULL);
    }
    else {
        return apr_pstrcat(r->pool, firstpart, val,
                           proxy_interpolate(r, end+1), NULL);
    }
}
/* Copied from httpd mod_proxy */
static int alias_match(const char *uri, const char *alias_fakename)
{
    const char *end_fakename = alias_fakename + strlen(alias_fakename);
    const char *aliasp = alias_fakename, *urip = uri;
    const char *end_uri = uri + strlen(uri);

    while (aliasp < end_fakename && urip < end_uri) {
        if (*aliasp == '/') {
            /* any number of '/' in the alias matches any number in
             * the supplied URI, but there must be at least one...
             */
            if (*urip != '/')
                return 0;

            while (*aliasp == '/')
                ++aliasp;
            while (*urip == '/')
                ++urip;
        }
        else {
            /* Other characters are compared literally */
            if (*urip++ != *aliasp++)
                return 0;
        }
    }

    /* fixup badly encoded stuff (e.g. % as last character) */
    if (aliasp > end_fakename) {
        aliasp = end_fakename;
    }
    if (urip > end_uri) {
        urip = end_uri;
    }

   /* We reach the end of the uri before the end of "alias_fakename"
    * for example uri is "/" and alias_fakename "/examples"
    */
   if (urip == end_uri && aliasp!=end_fakename) {
       return 0;
   }

    /* Check last alias path component matched all the way */
    if (aliasp[-1] != '/' && *urip != '\0' && *urip != '/')
        return 0;

    /* Return number of characters from URI which matched (may be
     * greater than length of alias, since we may have matched
     * doubled slashes)
     */

    return urip - uri;
}

/*
 * See if we could map the request.
 * first check is we have a balancer corresponding to the route.
 * then search the balancer correspond to the context and host.
 */
static int proxy_cluster_trans(request_rec *r)
{
    const char *balancer;
    void *sconf = r->server->module_config;
    proxy_server_conf *conf = (proxy_server_conf *)
        ap_get_module_config(sconf, &proxy_module);

#if HAVE_CLUSTER_EX_DEBUG
    ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_DEBUG, 0, r->server,
                "proxy_cluster_trans for %d %s %s uri: %s args: %s unparsed_uri: %s",
                 r->proxyreq, r->filename, r->handler, r->uri, r->args, r->unparsed_uri);
#endif

    balancer = get_route_balancer(r, conf);
    if (!balancer) {
        /* May be the balancer has not been created (XXX: use shared memory to find the balancer ...) */
        update_workers_node(conf, r->pool, r->server, 1);
        balancer = get_route_balancer(r, conf);
    }
    if (!balancer)
        balancer = get_context_host_balancer(r);
    if (!balancer) {
        /* May be the balancer has not been created (we use shared memory to find the balancer name) */
        update_workers_node(conf, r->pool, r->server, 1);
        balancer = get_context_host_balancer(r);
    }
    

    if (balancer) {
        int i;
        struct proxy_alias *ent = (struct proxy_alias *) conf->aliases->elts;
        /* Check that we don't have a ProxyPassMatch ^(/.*\.gif)$ ! or something similar */
        for (i = 0; i < conf->aliases->nelts; i++) {
            if (ent[i].real[0] == '!' && ent[i].real[1] == '\0') {
                ap_regmatch_t regm[AP_MAX_REG_MATCH];
                if (ent[i].regex) {
                    if (!ap_regexec(ent[i].regex, r->uri, AP_MAX_REG_MATCH, regm, 0)) {
                        return DECLINED;
                    }
                }
                else {
                    const char *fake;
                    proxy_dir_conf *dconf = ap_get_module_config(r->per_dir_config,
                                                                 &proxy_module);
                    if ((dconf->interpolate_env == 1)
                        && (ent[i].flags & PROXYPASS_INTERPOLATE)) {
                        fake = proxy_interpolate(r, ent[i].fake);
                    }
                    else {
                        fake = ent[i].fake;
                    }
                    if (alias_match(r->uri, fake)) {
                        return DECLINED;
                    }
                }
            }
        }

        /* It is safer to use r->uri and get_balancer_by_node() use r->uri too */
        r->filename =  apr_pstrcat(r->pool, "proxy:balancer://", balancer, r->uri, NULL);
        r->handler = "proxy-server";
        r->proxyreq = PROXYREQ_REVERSE;
#if HAVE_CLUSTER_EX_DEBUG
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_DEBUG, 0, r->server,
                    "proxy_cluster_trans using %s uri: %s",
                     balancer, r->filename);
#endif
        return OK; /* Mod_proxy will process it */
    }
 
#if HAVE_CLUSTER_EX_DEBUG
    ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_DEBUG, 0, r->server,
                "proxy_cluster_trans DECLINED %s uri: %s unparsed_uri: %s",
                 balancer, r->filename, r->unparsed_uri);
#endif
    return DECLINED;
}

/*
 * canonise the url
 * XXX: needs more see the unparsed_uri in proxy_cluster_trans()
 */
static int proxy_cluster_canon(request_rec *r, char *url)
{
    char *host, *path;
    char *search = NULL;
    const char *err;
    apr_port_t port = 0;

    if (strncasecmp(url, "balancer:", 9) == 0) {
        url += 9;
    } else {
        return DECLINED;
    }

#if HAVE_CLUSTER_EX_DEBUG
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_DEBUG, 0, r->server,
                    "proxy_cluster_canon url: %s", url);
#endif

    /* do syntatic check.
     * We break the URL into host, port, path, search
     */
    err = ap_proxy_canon_netloc(r->pool, &url, NULL, NULL, &host, &port);
    if (err) {
        ap_log_rerror(APLOG_MARK, APLOG_ERR, 0, r,
                      "error parsing URL %s: %s",
                      url, err);
        return HTTP_BAD_REQUEST;
    }
    /*
     * now parse path/search args, according to rfc1738:
     * process the path. With proxy-noncanon set (by
     * mod_proxy) we use the raw, unparsed uri
    */
    if (apr_table_get(r->notes, "proxy-nocanon")) {
        path = url;   /* this is the raw path */
    }
    else {
        path = ap_proxy_canonenc(r->pool, url, strlen(url), enc_path, 0,
                                 r->proxyreq);
        search = r->args;
    }
    if (path == NULL)
        return HTTP_BAD_REQUEST;

    r->filename = apr_pstrcat(r->pool, "proxy:balancer://", host,
            "/", path, (search) ? "?" : "", (search) ? search : "", NULL);
    return OK;
}

/*
 * Find the worker that has the 'route' defined
 * (Should we also find the domain corresponding to it).
 */
static proxy_worker *find_route_worker(request_rec *r,
                                       proxy_balancer *balancer,
                                       const char *route)
{
    int i;
    int checking_standby;
    int checked_standby;
    
    proxy_worker *worker;

    checking_standby = checked_standby = 0;
    while (!checked_standby) {
        worker = (proxy_worker *)balancer->workers->elts;
        for (i = 0; i < balancer->workers->nelts; i++, worker++) {
            if (worker->id == 0)
                continue; /* marked removed */

            if ( (checking_standby ? !PROXY_WORKER_IS_STANDBY(worker) : PROXY_WORKER_IS_STANDBY(worker)) )
                continue;
            if (*(worker->s->route) && strcmp(worker->s->route, route) == 0) {
                /* that is the worker corresponding to the route */
                if (worker && PROXY_WORKER_IS_USABLE(worker)) {
                    /* The context may not be available */
                    nodeinfo_t *node;
                    if (node_storage->read_node(worker->id, &node) != APR_SUCCESS)
                        return NULL; /* can't read node */
                    if (iscontext_host_ok(r, balancer, node))
                       return worker;
                    else
                       return NULL; /* application has been removed from the node */
                } else {
                    /*
                     * If the worker is in error state run
                     * retry on that worker. It will be marked as
                     * operational if the retry timeout is elapsed.
                     * The worker might still be unusable, but we try
                     * anyway.
                     */
                    ap_proxy_retry_worker("BALANCER", worker, r->server);
                    if (PROXY_WORKER_IS_USABLE(worker)) {
                            return worker;
                    } else {
                        /*
                         * We have a worker that is unusable.
                         * It can be in error or disabled, but in case
                         * it has a redirection set use that redirection worker.
                         * This enables to safely remove the member from the
                         * balancer. Of course you will need some kind of
                         * session replication between those two remote.
                         */
                        if (*worker->s->redirect) {
                            proxy_worker *rworker = NULL;
                            rworker = find_route_worker(r, balancer, worker->s->redirect);
                            /* Check if the redirect worker is usable */
                            if (rworker && !PROXY_WORKER_IS_USABLE(rworker)) {
                                /*
                                 * If the worker is in error state run
                                 * retry on that worker. It will be marked as
                                 * operational if the retry timeout is elapsed.
                                 * The worker might still be unusable, but we try
                                 * anyway.
                                 */
                                ap_proxy_retry_worker("BALANCER", rworker, r->server);
                            }
                            if (rworker && PROXY_WORKER_IS_USABLE(rworker))
                                return rworker;
                        }
                    }
                }
            }
        }
        checked_standby = checking_standby++;
    }
    return NULL;
}

/*
 * Find the worker corresponding to the JVMRoute.
 */
static proxy_worker *find_session_route(proxy_balancer *balancer,
                                        request_rec *r,
                                        const char **route,
                                        const char **sticky_used,
                                        char **url,
                                        const char **domain)
{
    proxy_worker *worker = NULL;

    if (!balancer->sticky)
        return NULL;
    if (! (balancer->sticky_force & STSESSION))
        return NULL;

    /* We already should have the route in the notes for the trans() */
    *route = apr_table_get(r->notes, "session-route");
    if (*route && (**route)) {
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                     "cluster: Using route %s", *route);
    } else
        return NULL;

    *sticky_used = apr_table_get(r->notes, "session-sticky");

    if (domain)
        *domain = apr_table_get(r->notes, "CLUSTER_DOMAIN");

    /* We have a route in path or in cookie
     * Find the worker that has this route defined.
     */
    worker = find_route_worker(r, balancer, *route);
    if (worker && strcmp(*route, worker->s->route)) {
        /*
         * Notice that the route of the worker chosen is different from
         * the route supplied by the client. (mod_proxy compatibility).
         */
        apr_table_setn(r->subprocess_env, "BALANCER_ROUTE_CHANGED", "1");
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                     "proxy: CLUSTER: Route changed from %s to %s",
                     *route, worker->s->route);
    }
    return worker;
}

static proxy_worker *find_best_worker(proxy_balancer *balancer, proxy_server_conf *conf,
                                      request_rec *r, const char *domain, int failoverdomain)
{
    proxy_worker *candidate = NULL;
    apr_status_t rv;

    if ((rv = PROXY_THREAD_LOCK(balancer)) != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, rv, r->server,
        "proxy: CLUSTER: (%s). Lock failed for find_best_worker()", balancer->name);
        return NULL;
    }

    /* XXX: candidate = (*balancer->lbmethod->finder)(balancer, r); */
    candidate = internal_find_best_byrequests(balancer, conf, r, domain, failoverdomain);

    if ((rv = PROXY_THREAD_UNLOCK(balancer)) != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, rv, r->server,
        "proxy: CLUSTER: (%s). Unlock failed for find_best_worker()", balancer->name);
    }

    if (candidate == NULL) {
        /* All the workers are in error state or disabled.
         * If the balancer has a timeout sleep for a while
         * and try again to find the worker. The chances are
         * that some other thread will release a connection.
         * By default the timeout is not set, and the server
         * returns SERVER_BUSY.
         */
#if APR_HAS_THREADS
        if (balancer->timeout) {
            /* XXX: This can perhaps be build using some
             * smarter mechanism, like tread_cond.
             * But since the statuses can came from
             * different childs, use the provided algo.
             */
            apr_interval_time_t timeout = balancer->timeout;
            apr_interval_time_t step, tval = 0;
            /* Set the timeout to 0 so that we don't
             * end in infinite loop
             */
            balancer->timeout = 0;
            step = timeout / 100;
            while (tval < timeout) {
                apr_sleep(step);
                /* Try again */
                if ((candidate = find_best_worker(balancer, conf, r, domain, failoverdomain)))
                    break;
                tval += step;
            }
            /* restore the timeout */
            balancer->timeout = timeout;
        }
#endif
    }
    return candidate;
}

static int rewrite_url(request_rec *r, proxy_worker *worker,
                        char **url)
{
    const char *scheme = strstr(*url, "://");
    const char *path = NULL;

    if (scheme)
        path = ap_strchr_c(scheme + 3, '/');

    /* we break the URL into host, port, uri */
    if (!worker) {
        return ap_proxyerror(r, HTTP_BAD_REQUEST, apr_pstrcat(r->pool,
                             "missing worker. URI cannot be parsed: ", *url,
                             NULL));
    }

    *url = apr_pstrcat(r->pool, worker->name, path, NULL);

    return OK;
}

/*
 * Remove the session information
 */
void remove_session_route(request_rec *r, const char *name)
{
    char *path = NULL;
    char *url = r->filename;
    char *start = NULL;
    char *cookies;
    const char *readcookies;
    char *start_cookie;

    /* First try to manipulate the url. */
    for (path = strstr(url, name); path; path = strstr(path + 1, name)) {
        start = path;
        if (*(start-1) == '&')
            start--;
        path += strlen(name);
        if (*path == '=') {
            ++path;
            if (strlen(path)) {
                char *filename = r->filename;
                while (*path !='&' || *path !='\0')
                    path++;
                /* We have it */
                *start = '\0';
                r->filename = apr_pstrcat(r->pool, filename, path, NULL);
                return; 
            }
        }
    }
    /* Second try to manipulate the cookie header... */

    if ((readcookies = apr_table_get(r->headers_in, "Cookie"))) {
        cookies = apr_pstrdup(r->pool, readcookies);
        for (start_cookie = ap_strstr(cookies, name); start_cookie;
             start_cookie = ap_strstr(start_cookie + 1, name)) {
            if (start_cookie == cookies ||
                start_cookie[-1] == ';' ||
                start_cookie[-1] == ',' ||
                isspace(start_cookie[-1])) {

                start = start_cookie;
                if (start_cookie != cookies &&
                    (start_cookie[-1] == ';' || start_cookie[-1] == ',' || isspace(start_cookie[-1]))) {
                    start--;
                }
                start_cookie += strlen(name);
                while(*start_cookie && isspace(*start_cookie))
                    ++start_cookie;
                if (*start_cookie == '=' && start_cookie[1]) {
                    /*
                     * Session cookie was found, get it's value
                     */
                    char *end_cookie;
                    char *cookie;
                    ++start_cookie;
                    if ((end_cookie = ap_strchr(start_cookie, ';')) == NULL)
                        end_cookie = ap_strchr(start_cookie, ',');

                    cookie = cookies;
                    *start = '\0';
                    cookies = apr_pstrcat(r->pool, cookie , end_cookie, NULL);
                    apr_table_setn(r->headers_in, "Cookie", cookies);
                }
            }
        }
    }
}

/*
 * Update the context active request counter
 */
static void upd_context_count(const char *id, int val)
{
    int ident = atoi(id);
    contextinfo_t *context;
    if (context_storage->read_context(ident, &context) == APR_SUCCESS) {
        context->nbrequests = context->nbrequests + val;
    }
}

/*
 * Find a worker for mod_proxy logic
 */
static int proxy_cluster_pre_request(proxy_worker **worker,
                                      proxy_balancer **balancer,
                                      request_rec *r,
                                      proxy_server_conf *conf, char **url)
{
    int access_status;
    proxy_worker *runtime;
    const char *route = NULL;
    const char *sticky = NULL;
    const char *domain = NULL;
    int failoverdomain = 0;
    apr_status_t rv;
    proxy_cluster_helper *helper;
    const char *context_id;

    *worker = NULL;
#if HAVE_CLUSTER_EX_DEBUG
     if (*balancer) {
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                     "proxy_cluster_pre_request: url %s balancer %s", *url, (*balancer)->name);
     } else {
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                     "proxy_cluster_pre_request: url %s", *url);
     }
#endif
    /* Step 1: check if the url is for us
     * The url we can handle starts with 'balancer://'
     * If balancer is already provided skip the search
     * for balancer, because this is failover attempt.
     */
    if (*balancer) {
        /* Adjust the helper->count corresponding to the previous try */
        const char *worker_name =  apr_table_get(r->subprocess_env, "BALANCER_WORKER_NAME");
        if (worker_name && *worker_name) {
#if HAVE_CLUSTER_EX_DEBUG
            ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                         "proxy_cluster_pre_request: worker %s", worker_name);
#endif
            int i;
            runtime = (proxy_worker *)(*balancer)->workers->elts;
            for (i = 0; i < (*balancer)->workers->nelts; i++, runtime++) {
                if (runtime->name && strcmp(worker_name, runtime->name) == 0) {
                    helper = (proxy_cluster_helper *) (runtime)->opaque;
                    apr_thread_mutex_lock(lock);
                    if (helper->count_active>0)
                        helper->count_active--;
                    /* Ajust the context counter here too */
                    context_id = apr_table_get(r->subprocess_env, "BALANCER_CONTEXT_ID");
                    if (context_id && *context_id) {
                       upd_context_count(context_id, -1);
                    }
                    apr_thread_mutex_unlock(lock);
                }
            }
        }
    }

    apr_thread_mutex_lock(lock);
    if (!*balancer &&
        !(*balancer = ap_proxy_get_balancer(r->pool, conf, *url))) {
        apr_thread_mutex_unlock(lock);
        /* May the node has not be created yet */
        update_workers_node(conf, r->pool, r->server, 1);
        if (!(*balancer = ap_proxy_get_balancer(r->pool, conf, *url))) {
            ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                         "proxy: CLUSTER no balancer for %s", *url);
            return DECLINED;
        }
        apr_thread_mutex_lock(lock);
    }

    /* Step 2: find the session route */

    runtime = find_session_route(*balancer, r, &route, &sticky, url, &domain);
    apr_thread_mutex_unlock(lock);

    /* Lock the LoadBalancer
     * XXX: perhaps we need the process lock here
     */
    if ((rv = PROXY_THREAD_LOCK(*balancer)) != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, rv, r->server,
                     "proxy: CLUSTER: (%s). Lock failed for pre_request",
                     (*balancer)->name);
        return DECLINED;
    }
    if (runtime) {
        runtime->s->elected++;
        *worker = runtime;
    }
    else if (route && ((*balancer)->sticky_force & STSESSFOR)) {
        if (domain == NULL) {
            /*
             * We have a route provided that doesn't match the
             * balancer name. See if the provider route is the
             * member of the same balancer in which case return 503
             */
            ap_log_error(APLOG_MARK, APLOG_ERR, 0, r->server,
                         "proxy: CLUSTER: (%s). All workers are in error state for route (%s)",
                         (*balancer)->name, route);
            if ((rv = PROXY_THREAD_UNLOCK(*balancer)) != APR_SUCCESS) {
                ap_log_error(APLOG_MARK, APLOG_ERR, rv, r->server,
                             "proxy: CLUSTER: (%s). Unlock failed for pre_request",
                             (*balancer)->name);
            }
            return HTTP_SERVICE_UNAVAILABLE;
        } else {
            /* We try to to failover using another node in the domain */
            failoverdomain = 1;
        }
    }

    if ((rv = PROXY_THREAD_UNLOCK(*balancer)) != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, rv, r->server,
                     "proxy: CLUSTER: (%s). Unlock failed for pre_request",
                     (*balancer)->name);
    }
    if (!*worker) {
        /* 
         * We have to failover (in domain only may be) or we don't use sticky sessions
         */
        runtime = find_best_worker(*balancer, conf, r, domain, failoverdomain);
        if (!runtime) {
            ap_log_error(APLOG_MARK, APLOG_ERR, 0, r->server,
                         "proxy: CLUSTER: (%s). All workers are in error state",
                         (*balancer)->name);

            return HTTP_SERVICE_UNAVAILABLE;
        }
        if ((*balancer)->sticky && runtime) {
            /*
             * This balancer has sticky sessions and the client either has not
             * supplied any routing information or all workers for this route
             * including possible redirect and hotstandby workers are in error
             * state, but we have found another working worker for this
             * balancer where we can send the request. Thus notice that we have
             * changed the route to the backend.
             */
            apr_table_setn(r->subprocess_env, "BALANCER_ROUTE_CHANGED", "1");
        }
        if (route && ((*balancer)->sticky_force & STSESSREM)) {
            /*
             * Failover to another domain. Remove sessionid information.
             */
            const char *domain_ok =  apr_table_get(r->notes, "session-domain-ok");
            if (!domain_ok) {
                remove_session_route(r, sticky); 
            }
        }
        *worker = runtime;
    }

    (*worker)->s->busy++;

    /* Mark the worker used for the cleanup logic */
    apr_thread_mutex_lock(lock);
    helper = (proxy_cluster_helper *) (*worker)->opaque;
    helper->count_active++;

    /* Also mark the context here note that find_best_worker set BALANCER_CONTEXT_ID */
    context_id = apr_table_get(r->subprocess_env, "BALANCER_CONTEXT_ID");
    if (context_id && *context_id) {
       upd_context_count(context_id, 1);
    }
    apr_thread_mutex_unlock(lock);

    /*
     * get_route_balancer already fills all of the notes and some subprocess_env
     * but not all.
     * Note that BALANCER_WORKER_NAME would have changed in case of failover.
     */
    /* Add balancer/worker info to env. */
    apr_table_setn(r->subprocess_env, "BALANCER_NAME", (*balancer)->name);
    apr_table_setn(r->subprocess_env, "BALANCER_WORKER_NAME", (*worker)->name);
    apr_table_setn(r->subprocess_env, "BALANCER_WORKER_ROUTE", (*worker)->s->route);

    /* Rewrite the url from 'balancer://url'
     * to the 'worker_scheme://worker_hostname[:worker_port]/url'
     * This replaces the balancers fictional name with the
     * real hostname of the elected worker.
     */
    access_status = rewrite_url(r, *worker, url);

#if HAVE_CLUSTER_EX_DEBUG
    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                 "proxy_cluster_pre_request: balancer (%s) worker (%s) rewritten to %s",
                 (*balancer)->name, (*worker)->name, *url);
#endif

    return access_status;
}

static int proxy_cluster_post_request(proxy_worker *worker,
                                       proxy_balancer *balancer,
                                       request_rec *r,
                                       proxy_server_conf *conf)
{

    proxy_cluster_helper *helper;
    const char *sessionid;
    const char *route;
    char *cookie = NULL;
    const char *sticky;
    char *oroute;
    const char *context_id = apr_table_get(r->subprocess_env, "BALANCER_CONTEXT_ID");

    /* mark the work as not use */
    apr_thread_mutex_lock(lock);
    helper = (proxy_cluster_helper *) worker->opaque;
    helper->count_active--;
    /* Ajust the context counter here too */
    context_id = apr_table_get(r->subprocess_env, "BALANCER_CONTEXT_ID");
    if (context_id && *context_id) {
       upd_context_count(context_id, -1);
    }
    apr_thread_mutex_unlock(lock);

#if HAVE_CLUSTER_EX_DEBUG
    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                 "proxy_cluster_post_request for (%s)", balancer->name);
    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                 "proxy_cluster_post_request for (%s) %s", balancer->name, balancer->sticky);
#endif

    if (worker && worker->s->busy)
        worker->s->busy--;

    if (sessionid_storage) {

        /* Add information about sessions corresponding to a node */
        sticky = apr_table_get(r->notes, "session-sticky");
        if (sticky == NULL && balancer->sticky) {
            char *path, *stick;
            stick = apr_pstrdup(r->pool, balancer->sticky);
            if ((path = strchr(stick, '|'))) {
                *path++ = '\0';
            }
            sticky = (const char *) stick;
        }
        if (sticky == NULL) {
    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                 "proxy_cluster_post_request for (%s) %s", balancer->name, balancer->sticky);
            return OK;
        }

        cookie = get_cookie_param(r, sticky, 0);
        sessionid =  apr_table_get(r->notes, "session-id");
        route =  apr_table_get(r->notes, "session-route");
        if (cookie) {
            if (sessionid && strcmp(cookie, sessionid)) {
                /* The cookie has changed, remove the old one and store the next one */
                sessionidinfo_t ou;
                strncpy(ou.sessionid, sessionid, SESSIONIDSZ);
                ou.id = 0;
                sessionid_storage->remove_sessionid(&ou);
            }
            if ((oroute = strchr(cookie, '.')) != NULL )
                oroute++;
            route = oroute;
            sessionid = cookie;
        }

        if (sessionid && route) {
            sessionidinfo_t ou;
            strncpy(ou.sessionid, sessionid, SESSIONIDSZ);
            strncpy(ou.JVMRoute, route, JVMROUTESZ);
            sessionid_storage->insert_update_sessionid(&ou);
        }
    }

    return OK;
}

/* the lbmethods (note it the only one in mod_cluster for the moment) */
static const proxy_balancer_method byrequests = 
{
    "byrequests",
    &find_best_byrequests,
    NULL
};

/*
 * Register the hooks on our module.
 */
static void proxy_cluster_hooks(apr_pool_t *p)
{
    static const char * const aszPre[]={ "mod_manager.c", NULL };
    static const char * const aszSucc[]={ "mod_proxy.c", NULL };

    ap_hook_post_config(proxy_cluster_post_config, NULL, NULL, APR_HOOK_MIDDLE);

    /* create the "maintenance" thread */
    ap_hook_child_init(proxy_cluster_child_init, NULL, NULL, APR_HOOK_MIDDLE);

    /* check the url and give the mapping to mod_proxy */
    ap_hook_translate_name(proxy_cluster_trans, aszPre, aszSucc, APR_HOOK_FIRST);

    proxy_hook_canon_handler(proxy_cluster_canon, NULL, NULL, APR_HOOK_FIRST);
 
    proxy_hook_pre_request(proxy_cluster_pre_request, NULL, NULL, APR_HOOK_FIRST);
    proxy_hook_post_request(proxy_cluster_post_request, NULL, NULL, APR_HOOK_FIRST);

    /* Register a provider for the "ping/pong" logic */
    ap_register_provider(p, "proxy_cluster", "balancer", "0", &balancerhandler);
    /* Register a provider for the loadbalancer (for things like ProxyPass /titi balancer://mycluster/myapp) */
    ap_register_provider(p, PROXY_LBMETHOD, "byrequests", "0", &balancerhandler);
}

/* XXX: not needed
static void *create_proxy_cluster_dir_config(apr_pool_t *p, char *dir)
{
    return NULL;
}
 */

static void *create_proxy_cluster_server_config(apr_pool_t *p, server_rec *s)
{
    return NULL;
}

static const char*cmd_proxy_cluster_creatbal(cmd_parms *cmd, void *dummy, const char *arg)
{
    int val = atoi(arg);
    if (val<0 || val>2) {
        return "CreateBalancers  must be one of: 0, 1 or 2";
    } else {
        creat_bal = val;
    }
    return NULL;
}

static const char*cmd_proxy_cluster_use_alias(cmd_parms *cmd, void *dummy, const char *arg)
{
    int val = atoi(arg);
    if (val<0 || val>1) {
        return "UseAlias  must be one of: 0 or 1";
    } else {
        use_alias = val;
    }
    return NULL;
}

static const char*cmd_proxy_cluster_lbstatus_recalc_time(cmd_parms *cmd, void *dummy, const char *arg)
{
    int val = atoi(arg);
    if (val<0) {
        return "LBstatusRecalTime must be greater than 0";
    } else {
        lbstatus_recalc_time = apr_time_from_sec(val);
    }
    return NULL;
}

static const command_rec  proxy_cluster_cmds[] =
{
    AP_INIT_TAKE1(
        "CreateBalancers",
        cmd_proxy_cluster_creatbal,
        NULL,
        OR_ALL,
        "CreateBalancers - Defined VirtualHosts where the balancers are created 0: All, 1: None, 2: Main (Default: 2 Main)"
    ),
    AP_INIT_TAKE1(
        "UseAlias",
        cmd_proxy_cluster_use_alias,
        NULL,
        OR_ALL,
        "UseAlias - Check that the Alias corresponds to the ServerName 0: Don't (ignore Aliases), 1: Check it (Default: 0 Ignore)"
    ),
    AP_INIT_TAKE1(
        "LBstatusRecalTime",
        cmd_proxy_cluster_lbstatus_recalc_time,
        NULL,
        OR_ALL,
        "LBstatusRecalTime - Time interval in seconds for loadbalancing logic to recalculate the status of a node: (Default: 5 seconds)"
    ),
    {NULL}
};

module AP_MODULE_DECLARE_DATA proxy_cluster_module = {
    STANDARD20_MODULE_STUFF,
    NULL,    /* per-directory config creator */
    NULL,                               /* dir config merger */
    create_proxy_cluster_server_config, /* server config creator */
    NULL,                               /* server config merger */
    proxy_cluster_cmds,                 /* command table */
    proxy_cluster_hooks                 /* register hooks */
};
