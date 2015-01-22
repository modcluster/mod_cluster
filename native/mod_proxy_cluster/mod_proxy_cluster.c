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
#include "apr_thread_cond.h"

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
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
    proxy_worker_shared *shared;
    int index; /* like the worker->id */
#endif 
};
typedef struct  proxy_cluster_helper proxy_cluster_helper;

static struct node_storage_method *node_storage = NULL; 
static struct host_storage_method *host_storage = NULL; 
static struct context_storage_method *context_storage = NULL; 
static struct balancer_storage_method *balancer_storage = NULL; 
static struct sessionid_storage_method *sessionid_storage = NULL; 
static struct domain_storage_method *domain_storage = NULL; 

static apr_thread_t *watchdog_thread = NULL;
static apr_thread_mutex_t *lock = NULL;
static apr_thread_cond_t *exit_cond = NULL;
static int watchdog_must_terminate = 0;

static server_rec *main_server = NULL;
#define CREAT_ALL  0 /* create balancers/workers in all VirtualHost */
#define CREAT_NONE 1 /* don't create balancers (but add workers) */
#define CREAT_ROOT 2 /* Only create balancers/workers in the main server */
static int creat_bal = CREAT_ROOT;

static int use_alias = 0; /* 1 : Compare Alias with server_name */

static apr_time_t lbstatus_recalc_time = apr_time_from_sec(5); /* recalcul the lbstatus based on number of request in the time interval */

static apr_time_t wait_for_remove =  apr_time_from_sec(10); /* wait until that before removing a removed node */

static int enable_options = -1; /* Use OPTIONS * for CPING/CPONG */

#define TIMESESSIONID 300                    /* after 5 minutes the sessionid have probably timeout */
#define TIMEDOMAIN    300                    /* after 5 minutes the sessionid have probably timeout */

/* Context table copy for local use */
struct proxy_context_table
{
	int sizecontext;
	int* contexts;
	contextinfo_t* context_info;
};
typedef struct proxy_context_table proxy_context_table;


/* VHost table copy for local use */
struct proxy_vhost_table
{
	int sizevhost;
	int* vhosts;
	hostinfo_t* vhost_info;
};
typedef struct proxy_vhost_table proxy_vhost_table;

/* Balancer table copy for local use */
struct proxy_balancer_table
{
	int sizebalancer;
	int* balancers;
	balancerinfo_t* balancer_info;
};
typedef struct proxy_balancer_table proxy_balancer_table;

/* Node table copy for local use */
struct proxy_node_table
{
	int sizenode;
	int* nodes;
	nodeinfo_t*  node_info;
};
typedef struct proxy_node_table proxy_node_table;

/* table of node and context selected by find_node_context_host() */
struct node_context
{
        int node;
        int context;
};
typedef struct node_context node_context;

#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
static int (*ap_proxy_retry_worker_fn)(const char *proxy_function,
        proxy_worker *worker, server_rec *s) = NULL;
#else

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
#endif /* AP_MODULE_MAGIC_AT_LEAST(20101223,1) */

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

/**
 * Add a node to the worker conf
 * XXX: Contains code of ap_proxy_initialize_worker (proxy_util.c)
 * XXX: If something goes wrong the worker can't be used and we leak memory... in a pool
 * NOTE: pool is the request pool or any temporary pool. Use conf->pool for any data that live longer.
 * @param node the pointer to the node structure
 * @param conf a proxy_server_conf.
 * @param balancer the balancer to update.
 * @param pool a temporary pool.
 * @server the server rec for logging purposes.
 *
 */
static apr_status_t create_worker(proxy_server_conf *conf, proxy_balancer *balancer,
                          server_rec *server,
                          nodeinfo_t *node, apr_pool_t *pool)
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
{
    char *url;
    char *ptr;
    apr_status_t rv = APR_SUCCESS;
    proxy_worker *worker;
    proxy_worker_shared *shared;
    proxy_cluster_helper *helper;

    /* build the name (scheme and port) when needed */
    url = apr_pstrcat(pool, node->mess.Type, "://", node->mess.Host, ":", node->mess.Port, NULL);

    worker = ap_proxy_get_worker(conf->pool, balancer, conf, url);
    if (worker == NULL) {

        /* creates it */ 
        const char *err = ap_proxy_define_worker(conf->pool, &worker, balancer, conf, url, 0);
        if (err) {
            ap_log_error(APLOG_MARK, APLOG_NOTICE|APLOG_NOERRNO, 0, server,
                         "Created: worker for %s failed: %s", url, err);
            return APR_EGENERAL;
        }

        worker->context = (proxy_cluster_helper *) apr_pcalloc(conf->pool,  sizeof(proxy_cluster_helper));
        if (!worker->context)
            return APR_EGENERAL;
        helper = (proxy_cluster_helper *) worker->context;
        helper->count_active = 0;
        helper->shared = worker->s;
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
                     "Created: worker for %s", url);
    } else {
        if (!worker->context) {
            /* That is BalancerMember */
            ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
                         "Created: reusing BalancerMember worker for %s", url);
            worker->context = (proxy_cluster_helper *) apr_pcalloc(conf->pool,  sizeof(proxy_cluster_helper));
            if (!worker->context)
                return APR_EGENERAL;
            helper = (proxy_cluster_helper *) worker->context;
            helper->index = -1; /* not remove and not a possible node */
        }
        helper = (proxy_cluster_helper *) worker->context;
        if (helper->index == 0) {
            /* We are going to reuse a removed one */
            ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
                         "Created: reusing removed worker for %s", url);
            if (worker->cp->pool == NULL) {
                init_conn_pool(conf->pool, worker);
            }
        } else {
            /* Check if the shared memory goes to the right place */
            char *pptr = (char *) node;
            ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
                         "Created: reusing worker for %s", url);
            pptr = pptr + node->offset;
            if (helper->index == node->mess.id && worker->s == (proxy_worker_shared *) pptr) {
                /* the share memory may have been removed and recreated */
                if (!worker->s->status) {
                    worker->s->status = PROXY_WORKER_INITIALIZED;
                    strncpy(worker->s->route, node->mess.JVMRoute, PROXY_WORKER_MAX_ROUTE_SIZE-1);
                    worker->s->route[PROXY_WORKER_MAX_ROUTE_SIZE-1] = '\0';
                    /* XXX: We need that information from TC */
                    worker->s->redirect[0] = '\0';
                    worker->s->lbstatus = 0;
                    worker->s->lbfactor = -1; /* prevent using the node using status message */
                }
                if (worker->cp->pool == NULL) {
                     init_conn_pool(conf->pool, worker);
                }
                return APR_SUCCESS; /* Done Already existing */
            } else {
                ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
                             "Created: can't reuse worker as it for %s cleaning...", url);
                ptr = (char *) node;
                ptr = ptr + node->offset;
                shared = worker->s;
                worker->s = (proxy_worker_shared *) ptr;
                worker->s->was_malloced = 0; /* Prevent mod_proxy to free it */
                helper->index = node->mess.id;

                if ((rv = ap_proxy_initialize_worker(worker, server, conf->pool)) != APR_SUCCESS) {
                    ap_log_error(APLOG_MARK, APLOG_ERR, rv, server,
                                 "ap_proxy_initialize_worker failed %d for %s", rv, url);
                    return rv;
                }
                if (worker->cp->pool == NULL) {
                    init_conn_pool(conf->pool, worker);
                }
                return APR_SUCCESS;
            }
        }
    }

    /*
     * Get the shared memory for this worker
     * we are here for 3 reasons:
     * 1 - the worker was created.
     * 2 - it is the BalancerMember and we try to change the shared status.
     * 3 - we are reusing a removed worker.
     */
    node_storage->lock_nodes();
    ptr = (char *) node;
    ptr = ptr + node->offset;
    shared = worker->s;
    worker->s = (proxy_worker_shared *) ptr;
    helper->index = node->mess.id;

    /* Changing the shared memory requires looking it... */
    if (strncmp(worker->s->name, shared->name, sizeof(worker->s->name))) {
        /* We will modify it only is the name has changed to minimize access */
        worker->s->was_malloced = 0; /* Prevent mod_proxy to free it */
        worker->s->index = node->mess.id;
        strncpy(worker->s->name, shared->name, sizeof(worker->s->name));
        strncpy(worker->s->hostname, shared->hostname, sizeof(worker->s->hostname));
        strncpy(worker->s->scheme, shared->scheme, sizeof(worker->s->scheme));
        worker->s->port = shared->port;
        worker->s->hmax = shared->hmax;
        if (worker->s->hmax < node->mess.smax)
            worker->s->hmax = node->mess.smax + 1;
        strncpy(worker->s->route, node->mess.JVMRoute, PROXY_WORKER_MAX_ROUTE_SIZE-1);
        worker->s->route[PROXY_WORKER_MAX_ROUTE_SIZE-1] = '\0';
        worker->s->redirect[0] = '\0';
        worker->s->smax = node->mess.smax;
        worker->s->ttl = node->mess.ttl;
        if (node->mess.timeout) {
            worker->s->timeout_set = 1;
            worker->s->timeout = node->mess.timeout;
        }
        worker->s->flush_packets = node->mess.flushpackets;
        worker->s->flush_wait = node->mess.flushwait;
        worker->s->ping_timeout = node->mess.ping;
        worker->s->ping_timeout_set = 1;
        worker->s->acquire_set = 1;
        worker->s->conn_timeout_set = 1;
        worker->s->conn_timeout = node->mess.ping;
        worker->s->keepalive = 1;
        worker->s->keepalive_set = 1;
        worker->s->is_address_reusable = 1;
        worker->s->acquire = apr_time_make(0, 2 * 1000); /* 2 ms */
        worker->s->retry = apr_time_from_sec(PROXY_WORKER_DEFAULT_RETRY);
    }

    if ((rv = ap_proxy_initialize_worker(worker, server, conf->pool)) != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, rv, server,
                     "ap_proxy_initialize_worker failed %d for %s", rv, url);
        node_storage->unlock_nodes();
        return rv;
    }

    /*
     * The Shared datastatus may already contains a valid information
     */
    if (!worker->s->status) {
        worker->s->status = PROXY_WORKER_INITIALIZED;
        /* XXX: We need that information from TC */
        worker->s->redirect[0] = '\0';
        worker->s->lbstatus = 0;
        worker->s->lbfactor = -1; /* prevent using the node using status message */
    }

    node_storage->unlock_nodes();
    return rv;
}
#else
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
    int sizew = conf->workers->elt_size;
    proxy_worker *worker;
    /* build the name (scheme and port) when needed */
    url = apr_pstrcat(pool, node->mess.Type, "://", node->mess.Host, ":", node->mess.Port, NULL);

    worker = ap_proxy_get_worker(pool, conf, url);
    if (worker == NULL) {

        /* creates it */ 
        proxy_cluster_helper *helper;
        const char *err = ap_proxy_add_worker(&worker, conf->pool, conf, url);
        if (err) {
            ap_log_error(APLOG_MARK, APLOG_NOTICE|APLOG_NOERRNO, 0, server,
                         "Created: worker for %s failed: %s", url, err);
            return APR_EGENERAL;
        }
        worker->opaque = (proxy_cluster_helper *) apr_pcalloc(conf->pool,  sizeof(proxy_cluster_helper));
        if (!worker->opaque)
            return APR_EGENERAL;
        helper = (proxy_cluster_helper *) worker->opaque;
        helper->count_active = 0;
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
                     "Created: worker for %s", url);
    } else  if (worker->id == 0) {
        /* We are going to reuse a removed one */
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
                     "Created: reusing worker for %s", url);
        if (worker->cp->pool == NULL) {
            init_conn_pool(conf->pool, worker);
        }
        reuse = 1;
    } else {
        /* Check if the shared memory goes to the right place */
        char *pptr = (char *) node;
        pptr = pptr + node->offset;
        if (worker->id == node->mess.id && worker->s == (proxy_worker_stat *) pptr) {
            /* the share memory may have been removed and recreated */
            if (!worker->s->status) {
                worker->s->status = PROXY_WORKER_INITIALIZED;
                strncpy(worker->s->route, node->mess.JVMRoute, PROXY_WORKER_MAX_ROUTE_SIZ);
                worker->s->route[PROXY_WORKER_MAX_ROUTE_SIZ] = '\0';
                /* XXX: We need that information from TC */
                worker->s->redirect[0] = '\0';
                worker->s->lbstatus = 0;
                worker->s->lbfactor = -1; /* prevent using the node using status message */
            }
            return APR_SUCCESS; /* Done Already existing */
        }
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
                     "Created: can't reuse worker as it for %s cleaning...", url);
        if (!worker->opaque)
            worker->opaque = (proxy_cluster_helper *) apr_pcalloc(conf->pool,  sizeof(proxy_cluster_helper));
        if (!worker->opaque)
            return APR_EGENERAL;
        if (worker->cp->pool) {
            /* destroy and create a new one */
            apr_pool_destroy(worker->cp->pool);
            worker->cp->pool = NULL;
            init_conn_pool(conf->pool, worker);
        }
        reuse = 1;
    }

    /* Get the shared memory for this worker */
    ptr = (char *) node;
    ptr = ptr + node->offset;
    worker->s = (proxy_worker_stat *) ptr;

    worker->id = node->mess.id;
    worker->route = apr_pstrdup(conf->pool, node->mess.JVMRoute);
    worker->redirect = apr_pstrdup(conf->pool, "");
    worker->smax = node->mess.smax;
    worker->ttl = node->mess.ttl;
    if (node->mess.timeout) {
        worker->timeout_set = 1;
        worker->timeout = node->mess.timeout;
    }
    worker->flush_packets = node->mess.flushpackets;
    worker->flush_wait = node->mess.flushwait;
#if AP_MODULE_MAGIC_AT_LEAST(20051115,4)
    worker->ping_timeout = node->mess.ping;
    worker->ping_timeout_set = 1;
    worker->acquire_set = 1;
#else
    helperping = (proxy_cluster_helper *) worker->opaque;
    helperping->ping_timeout = node->mess.ping;
    helperping->ping_timeout_set = 1;
#endif
#if AP_MODULE_MAGIC_AT_LEAST(20051115,16)
    /* For MODCLUSTER-217 */
    worker->conn_timeout_set = 1;
    worker->conn_timeout = node->mess.ping;
#endif
    worker->keepalive = 1;
    worker->keepalive_set = 1;
    worker->is_address_reusable = 1;
    worker->acquire = apr_time_make(0, 2 * 1000); /* 2 ms */
    worker->retry = apr_time_from_sec(PROXY_WORKER_DEFAULT_RETRY);

    /* from ap_proxy_initialize_worker() */
#if APR_HAS_THREADS
    ap_mpm_query(AP_MPMQ_MAX_THREADS, &mpm_threads);
    if (mpm_threads > 1) {
        /* Set hard max to no more then mpm_threads */
        if (worker->hmax == 0 || worker->hmax > mpm_threads) {
            worker->hmax = mpm_threads;
        }
        if (worker->smax == -1 || worker->smax > worker->hmax) {
            worker->smax = worker->hmax;
        }
        /* Set min to be lower then smax */
        if (worker->min > worker->smax) {
            worker->min = worker->smax;
        }
    }
    else {
        /* This will supress the apr_reslist creation */
        worker->min = worker->smax = worker->hmax = 0;
    }

    if (worker->hmax) {
        rv = apr_reslist_create(&(worker->cp->res),
                                worker->min, worker->smax,
                                worker->hmax, worker->ttl,
                                connection_constructor, connection_destructor,
                                worker, worker->cp->pool);

        apr_pool_cleanup_register(worker->cp->pool, (void *)worker,
                                  conn_pool_cleanup,
                                  apr_pool_cleanup_null);

        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
            "proxy: initialized worker %d in child %" APR_PID_T_FMT " for (%s) min=%d max=%d smax=%d",
             worker->id, getpid(), worker->hostname, worker->min,
             worker->hmax, worker->smax);

#if (APR_MAJOR_VERSION > 0)
        /* Set the acquire timeout */
        if (rv == APR_SUCCESS && worker->acquire_set) {
            apr_reslist_timeout_set(worker->cp->res, worker->acquire);
        }
#endif
    }
    else
#endif
    {

        rv = connection_constructor((void **)&(worker->cp->conn), worker, worker->cp->pool);
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
             "proxy: initialized single connection worker %d in child %" APR_PID_T_FMT " for (%s)",
             worker->id, getpid(), worker->hostname);
    }
    /* end from ap_proxy_initialize_worker() */

    /*
     * The Shared datastatus may already contains a valid information
     */
    if (!worker->s->status) {
        worker->s->status = PROXY_WORKER_INITIALIZED;
        strncpy(worker->s->route, node->mess.JVMRoute, PROXY_WORKER_MAX_ROUTE_SIZ);
        worker->s->route[PROXY_WORKER_MAX_ROUTE_SIZ] = '\0';
        /* XXX: We need that information from TC */
        worker->s->redirect[0] = '\0';
        worker->s->lbstatus = 0;
        worker->s->lbfactor = -1; /* prevent using the node using status message */
    }

    if (!reuse) {
        /*
         * Create the corresponding balancer worker information
         * copying for proxy_util.c ap_proxy_add_worker_to_balancer
         */
        proxy_worker *runtime;
        runtime = apr_array_push(balancer->workers);
        memcpy(runtime, worker, sizew);
    } else {
        /* Update the corresponding balancer worker information */
        proxy_worker *runtime;
        int i;
        int sizew = balancer->workers->elt_size;
        char *ptr = balancer->workers->elts;

        runtime = (proxy_worker *)balancer->workers->elts;
        for (i = 0; i < balancer->workers->nelts; i++, ptr=ptr+sizew) {
            runtime = (proxy_worker *)ptr;
            if (runtime->name) {
                if (strcmp(url, runtime->name) == 0) {
                    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
                                 "Created: reuse worker %s of %s", runtime->name, balancer->name);
                    memcpy(runtime, worker, sizew);
                    break;
                }
            }
        }
        if (i ==  balancer->workers->nelts) {
            ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
                         "Created: reuse worker %s not yet in balancer %s", url, balancer->name);
            runtime = apr_array_push(balancer->workers);
            memcpy(runtime, worker, sizew);
        }
    }
    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
                 "Created: worker for %s %d (status): %d", url, worker->id, worker->s->status);
    return rv;
}
#endif

static balancerinfo_t *read_balancer_name(const char *name, apr_pool_t *pool)
{
    int sizebal, i;
    int *bal;
    sizebal =  balancer_storage->get_max_size_balancer();
    if (sizebal == 0)
        return NULL; /* Done broken. */
    bal = apr_pcalloc(pool, sizeof(int) * sizebal);
    sizebal = balancer_storage->get_ids_used_balancer(bal);
    for (i=0; i<sizebal; i++) {
        balancerinfo_t *balan;
        balancer_storage->read_balancer(bal[i], &balan);
        /* Something like balancer://cluster1 and cluster1 */
        if (strcmp(balan->balancer, name) == 0) {
            return balan;
        }
    }
    return NULL;
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

#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
    balancer = ap_proxy_get_balancer(pool, conf, name, 0);
#else
    balancer = ap_proxy_get_balancer(pool, conf, name);
#endif
    if (!balancer) {
       int sizeb = conf->balancers->elt_size;
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
        proxy_balancer_shared *bshared;
#endif
       ap_log_error(APLOG_MARK, APLOG_DEBUG|APLOG_NOERRNO, 0, server,
                    "add_balancer_node: Create balancer %s", name);

       balancer = apr_array_push(conf->balancers);
       memset(balancer, 0, sizeb);

#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
        balancer->gmutex = NULL;
        bshared = apr_palloc(conf->pool, sizeof(proxy_balancer_shared));
        memset(bshared, 0, sizeof(proxy_balancer_shared));
        if (PROXY_STRNCPY(bshared->sname, name) != APR_SUCCESS) {
            ap_log_error(APLOG_MARK, APLOG_NOTICE|APLOG_NOERRNO, 0, server,
                          "add_balancer_node: balancer safe-name (%s) too long", name);
            return NULL;
        }
        bshared->hash.def = ap_proxy_hashfunc(name, PROXY_HASHFUNC_DEFAULT);
        bshared->hash.fnv = ap_proxy_hashfunc(name, PROXY_HASHFUNC_FNV);
        balancer->s = bshared;
        balancer->hash = bshared->hash;
        balancer->sconf = conf;
        if (apr_thread_mutex_create(&(balancer->tmutex),
                    APR_THREAD_MUTEX_DEFAULT, conf->pool) != APR_SUCCESS) {
            /* XXX: Do we need to log something here? */
            ap_log_error(APLOG_MARK, APLOG_NOTICE|APLOG_NOERRNO, 0, server,
                          "add_balancer_node: Can't create lock for balancer");
        }
        balancer->workers = apr_array_make(conf->pool, 5, sizeof(proxy_worker *));
        strncpy(balancer->s->name, name, PROXY_BALANCER_MAX_NAME_SIZE);
#else
        balancer->name = apr_pstrdup(conf->pool, name);
        /* XXX Is this a right place to create mutex */
#if APR_HAS_THREADS
        if (apr_thread_mutex_create(&(balancer->mutex),
                    APR_THREAD_MUTEX_DEFAULT, conf->pool) != APR_SUCCESS) {
            /* XXX: Do we need to log something here? */
            ap_log_error(APLOG_MARK, APLOG_NOTICE|APLOG_NOERRNO, 0, server,
                          "add_balancer_node: Can't create lock for balancer");
        }
#endif
        int sizew = conf->workers->elt_size;
        balancer->workers = apr_array_make(conf->pool, 5, sizew);
#endif /* AP_MODULE_MAGIC_AT_LEAST(20101223,1) */
        balancer->lbmethod = ap_lookup_provider(PROXY_LBMETHOD, "byrequests", "0");
    } else {
        ap_log_error(APLOG_MARK, APLOG_DEBUG|APLOG_NOERRNO, 0, server,
                      "add_balancer_node: Using balancer %s", name);
    }

    if (balancer && balancer->workers->nelts == 0) {
        /* Logic to copy the shared memory information to the balancer */
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
        balancerinfo_t *balan = read_balancer_name(&balancer->s->name[11], pool);
#else
        balancerinfo_t *balan = read_balancer_name(&balancer->name[11], pool);
#endif
        if (balan == NULL)
            return balancer; /* Done broken */
        /* XXX: StickySession, StickySessionRemove not in */
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
        strncpy(balancer->s->sticky, balan->StickySessionCookie, PROXY_BALANCER_MAX_STICKY_SIZE-1);
        balancer->s->sticky[PROXY_BALANCER_MAX_STICKY_SIZE-1] = '\0';
        strncpy(balancer->s->sticky_path, balan->StickySessionPath, PROXY_BALANCER_MAX_STICKY_SIZE-1);
        balancer->s->sticky_path[PROXY_BALANCER_MAX_STICKY_SIZE-1] = '\0';
        if (balan->StickySessionForce)
            balancer->s->sticky_force = 1; /* STSESSREM is keep in the mod_cluster "private" balancer */
        balancer->s->timeout = balan->Timeout;
        balancer->s->max_attempts = balan->Maxattempts;
        balancer->s->max_attempts_set = 1;
#else
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
#endif
    }
    return balancer;
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
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
        proxy_balancer *balancer = ap_proxy_get_balancer(pool, conf, name, 0);
#else
        proxy_balancer *balancer = ap_proxy_get_balancer(pool, conf, name);
#endif

        if (!balancer && (creat_bal == CREAT_NONE ||
            (creat_bal == CREAT_ROOT && s!=main_server))) {
            s = s->next;
            continue;
        }
        if (!balancer)
            balancer = add_balancer_node(node, conf, pool, s);
        else {
            /* We "reuse" the balancer */
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
            balancerinfo_t *balan = read_balancer_name(&balancer->s->name[11], pool);
            if (balan != NULL) {
                int changed = 0;
                if (!balancer->s->sticky_force && balan->StickySessionForce) {
                    balancer->s->sticky_force = 1;
                    changed = -1;
                }
                if (balancer->s->sticky_force && !balan->StickySessionForce) {
                    balancer->s->sticky_force = 0;
                    changed = -1;
                }
                if (strcmp(balan->StickySessionCookie, balancer->s->sticky) != 0) {
                    strncpy(balancer->s->sticky , balan->StickySessionCookie, PROXY_BALANCER_MAX_STICKY_SIZE-1);
                    balancer->s->sticky[PROXY_BALANCER_MAX_STICKY_SIZE-1] = '\0';
                    changed = -1;
                }
                if (strcmp(balan->StickySessionPath, balancer->s->sticky_path) != 0) {
                    strncpy(balancer->s->sticky_path , balan->StickySessionPath, PROXY_BALANCER_MAX_STICKY_SIZE-1);
                    balancer->s->sticky_path[PROXY_BALANCER_MAX_STICKY_SIZE-1] = '\0';
                    changed = -1;
                }
                balancer->s->timeout =  balan->Timeout;
                balancer->s->max_attempts = balan->Maxattempts;
                balancer->s->max_attempts_set = 1;
                if (changed) {
                    /* log a warning */
                    ap_log_error(APLOG_MARK, APLOG_NOTICE|APLOG_NOERRNO, 0, s,
                                 "Balancer %s changed" , &balancer->s->name[11]);
                }
            }
#else
            balancerinfo_t *balan = read_balancer_name(&balancer->name[11], pool);
            if (balan != NULL) {
                char *sticky = apr_psprintf(pool, "%s|%s", balan->StickySessionCookie,
                                                           balan->StickySessionPath);
                int sticky_force=0;
                int changed = 0;
                if (balan->StickySession)
                    sticky_force += STSESSION;
                if (balan->StickySessionForce)
                    sticky_force += STSESSFOR;
                if (balan->StickySessionRemove)
                    sticky_force += STSESSREM;
              
                if (balancer->sticky_force != sticky_force) {
                    balancer->sticky_force = sticky_force;
                    changed = -1;
                }
                if (balancer->sticky == NULL || strcmp(sticky, balancer->sticky) != 0) {
                    balancer->sticky = apr_pstrdup(conf->pool, sticky);
                    changed = -1;
                }
                balancer->timeout =  balan->Timeout;
                balancer->max_attempts = balan->Maxattempts;
                balancer->max_attempts_set = 1;
                if (changed) {
                    /* log a warning */
                    ap_log_error(APLOG_MARK, APLOG_NOTICE|APLOG_NOERRNO, 0, s,
                                 "Balancer %s changed" , &balancer->name[11]);
                }
            }
#endif
        }
        if (balancer)
            create_worker(conf, balancer, s, node, pool);
        s = s->next;
    }
}

/* the worker corresponding to the id, note that we need to compare the shared memory pointer too */
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
static proxy_worker *get_worker_from_id_stat(proxy_server_conf *conf, int id, proxy_worker_shared *stat)
{
    int i;
    char *ptr = conf->balancers->elts;
    int sizeb = conf->balancers->elt_size;
    int sizew = sizeof(proxy_worker *);

    for (i = 0; i < conf->balancers->nelts; i++, ptr=ptr+sizeb) {
        int j;
        char *ptrw;
        proxy_balancer *balancer = (proxy_balancer *) ptr;
        ptrw = balancer->workers->elts;
        for (j = 0; j < balancer->workers->nelts; j++,  ptrw=ptrw+sizew) {
            proxy_worker **worker = (proxy_worker **) ptrw;
            proxy_cluster_helper *helper = (proxy_cluster_helper *) (*worker)->context;
            if ((*worker)->s == stat && helper->index == id) {
                return *worker;
            }
        }
    }
    return NULL;
}
#else
static proxy_worker *get_worker_from_id_stat(proxy_server_conf *conf, int id, proxy_worker_stat *stat)
{
    int i;
    int sizew = conf->workers->elt_size;
    char *ptrw = conf->workers->elts;

    for (i = 0; i < conf->workers->nelts; i++, ptrw=ptrw+sizew) {
        proxy_worker *worker = (proxy_worker *) ptrw;
        if (worker->id == id && worker->s == stat) {
            return worker;
        }
        worker++;
    }
    return NULL;
}
#endif

/*
 * Remove a node from the worker conf
 */
static int remove_workers_node(nodeinfo_t *node, proxy_server_conf *conf, apr_pool_t *pool, server_rec *server)
{
    int i;
    char *pptr = (char *) node;
    proxy_cluster_helper *helper;
    proxy_worker *worker;
    pptr = pptr + node->offset;

#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
    worker = get_worker_from_id_stat(conf, node->mess.id, (proxy_worker_shared *) pptr);
#else
    worker = get_worker_from_id_stat(conf, node->mess.id, (proxy_worker_stat *) pptr);
#endif
    if (!worker) {
        /* XXX: Another process may use it, can't do: node_storage->remove_node(node); */
        return 0; /* Done */
                    /* Here we loop through our workers not need to check that the worker->s is OK */
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
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
    helper = (proxy_cluster_helper *) worker->context;
#else
    helper = (proxy_cluster_helper *) worker->opaque;
#endif
    if (helper) {
        i = helper->count_active;
    }
    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
             "remove_workers_node (helper) %d %s", i, node->mess.JVMRoute);
#endif

    if (i == 0) {
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
        /* The worker already comes from the apr_array of the balancer */
        proxy_worker_shared *stat = worker->s;
        proxy_cluster_helper *helper = (proxy_cluster_helper *) worker->context;
#else
        /* No connection in use: clean the worker */
        char *name = apr_pstrcat(pool, "balancer://", node->mess.balancer, NULL);
        char *ptr = conf->balancers->elts;
        int sizeb = conf->balancers->elt_size;

        /* mark the worker removed in the apr_array of the balancer */
        for (i = 0; i < conf->balancers->nelts; i++, ptr=ptr+sizeb) {
            proxy_balancer *balancer =  (proxy_balancer *) ptr;
            if (strcmp(balancer->name, name) == 0) {
                int j;
                int sizew = balancer->workers->elt_size;
                char *ptrw = balancer->workers->elts;
                
                for (j = 0; j < balancer->workers->nelts; j++, ptrw=ptrw+sizew) {
                    /* Here we loop through our workers not need to check that the worker->s is OK */
                    proxy_worker *searched = (proxy_worker *) ptrw;
                    if (searched->id == worker->id) {
                        searched->id = 0; /* mark it removed */
                    }
                }
            }
        }
#endif

        /* Clear the connection pool (close the sockets) */
        if (worker->cp->pool) {
            apr_pool_destroy(worker->cp->pool);
            worker->cp->pool = NULL;
        }

        /* XXX: Shouldnn't we remove the mutex too (worker->mutex) */

#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
        /* Here that is tricky the worker needs shared memory but we don't and CONFIG will reset it */
        helper->index = 0; /* mark it removed */
        worker->s = helper->shared;
        memcpy(worker->s, stat, sizeof(proxy_worker_shared));
#else
        worker->id = 0; /* mark it removed */
#endif

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
    unsigned int last;

    /* Check if we have to do something */
    apr_thread_mutex_lock(lock);
    if (check) { 
        last = node_storage->worker_nodes_need_update(main_server, pool);
        /* nodes_need_update will return 1 if last_updated is zero: first time we are called */
        if (last == 0) {
            apr_thread_mutex_unlock(lock);
            return;
        }
    }

    /* read the ident of the nodes */
    size = node_storage->get_max_size_node();
    if (size == 0) {
        apr_thread_mutex_unlock(lock);
        return;
    }
    id = apr_pcalloc(pool, sizeof(int) * size);
    size = node_storage->get_ids_used_node(id);

    /* XXX: How to skip the balancer that aren't controled by mod_manager */

    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
             "update_workers_node starting");

    /* Only process the nodes that have been updated since our last update */
    for (i=0; i<size; i++) {
        nodeinfo_t *ou;
        if (node_storage->read_node(id[i], &ou) != APR_SUCCESS)
            continue;
        add_balancers_workers(ou, pool);
    } 

    apr_thread_mutex_unlock(lock);
    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, server,
             "update_workers_node done");
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
        ap_log_error(APLOG_MARK, APLOG_ERR, status, r->server,
                      "ajp_cping_cpong(): send failed");
        return status;
    }
    status = apr_socket_timeout_get(sock, &org);
    if (status != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, status, r->server,
                      "ajp_cping_cpong(): apr_socket_timeout_get failed");
        return status;
    }
    status = apr_socket_timeout_set(sock, timeout);
    written = 5;
    status = apr_socket_recv(sock, buf, &written);
    if (status != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, status, r->server,
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

static
apr_status_t ap_proxygetline(apr_bucket_brigade *bb, char *s, int n, request_rec *r,
                             int fold, int *writen)
{
    char *tmp_s = s;
    apr_status_t rv;
    apr_size_t len;

    rv = ap_rgetline(&tmp_s, n, &len, r, fold, bb);
    apr_brigade_cleanup(bb);

    if (rv == APR_SUCCESS) {
        *writen = (int) len;
    } else if (rv == APR_ENOSPC) {
        *writen = n;
    } else {
        *writen = -1;
    }

    return rv;
}

/* In 2.4.x the routine is public any more */
#if AP_MODULE_MAGIC_AT_LEAST(20111203,0)
static request_rec *ap_proxy_make_fake_req(conn_rec *c, request_rec *r)
{
    apr_pool_t *pool;
    request_rec *rp;

    apr_pool_create(&pool, c->pool);

    rp = apr_pcalloc(pool, sizeof(*r));

    rp->pool            = pool;
    rp->status          = HTTP_OK;

    rp->headers_in      = apr_table_make(pool, 50);
    rp->subprocess_env  = apr_table_make(pool, 50);
    rp->headers_out     = apr_table_make(pool, 12);
    rp->err_headers_out = apr_table_make(pool, 5);
    rp->notes           = apr_table_make(pool, 5);

    rp->server = r->server;
    rp->log = r->log;
    rp->proxyreq = r->proxyreq;
    rp->request_time = r->request_time;
    rp->connection      = c;
    rp->output_filters  = c->output_filters;
    rp->input_filters   = c->input_filters;
    rp->proto_output_filters  = c->output_filters;
    rp->proto_input_filters   = c->input_filters;
    rp->useragent_ip = c->client_ip;
    rp->useragent_addr = c->client_addr;

    rp->request_config  = ap_create_request_config(pool);
    proxy_run_create_req(r, rp);

    return rp;
}
#endif
/*
 * Do a ping/pong to the node
 */
static apr_status_t http_handle_cping_cpong(proxy_conn_rec *p_conn,
                                           request_rec *r,
                                           apr_interval_time_t timeout)
{
    char *srequest;
    char buffer[HUGE_STRING_LEN];
    int len;
    apr_status_t status, rv;
    apr_interval_time_t org; 
    apr_bucket_brigade *header_brigade, *tmp_bb;
    apr_bucket *e;
    request_rec *rp;

    srequest = apr_pstrcat(r->pool, "OPTIONS * HTTP/1.0\r\nUser-Agent: ",
                           ap_get_server_banner(),
                           " (internal mod_cluster connection)\r\n\r\n", NULL);
    header_brigade = apr_brigade_create(r->pool, p_conn->connection->bucket_alloc);
    e = apr_bucket_pool_create(srequest, strlen(srequest), r->pool, p_conn->connection->bucket_alloc);
    APR_BRIGADE_INSERT_TAIL(header_brigade, e);
    e = apr_bucket_flush_create(p_conn->connection->bucket_alloc);
    APR_BRIGADE_INSERT_TAIL(header_brigade, e);

    status = ap_pass_brigade(p_conn->connection->output_filters, header_brigade);
    if (status != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, status, r->server,
                      "http_cping_cpong(): send failed");
        return status;
    }
    apr_brigade_cleanup(header_brigade);

    status = apr_socket_timeout_get(p_conn->sock, &org);
    if (status != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, status, r->server,
                      "http_cping_cpong(): apr_socket_timeout_get failed");
        return status;
    }
    status = apr_socket_timeout_set(p_conn->sock, timeout);

    /* we need to read the answer */
    status = APR_EGENERAL;
    rp = ap_proxy_make_fake_req(p_conn->connection, r);
    rp->proxyreq = PROXYREQ_RESPONSE;
    tmp_bb = apr_brigade_create(r->pool, p_conn->connection->bucket_alloc);
    while (1) {
        ap_proxygetline(tmp_bb, buffer, sizeof(buffer), rp, 0, &len);
        if (len <= 0)
           break;
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                     "http_cping_cpong: received %s", buffer);
        status = APR_SUCCESS;
    }
    if (status != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, status, r->server,
               "http_cping_cpong: ap_getline failed");
    }
    rv = apr_socket_timeout_set(p_conn->sock, org);
    if (rv != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, 0, r->server,
               "http_cping_cpong: apr_socket_timeout_set failed");
        p_conn->close = 1;
        return rv;
    }

    p_conn->close = 1;
    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                         "http_cping_cpong: Done");
    return status;
}

static apr_status_t proxy_cluster_try_pingpong(request_rec *r, proxy_worker *worker,
                                               char *url, proxy_server_conf *conf,
                                               apr_interval_time_t ping, apr_interval_time_t workertimeout)
{
    apr_status_t status;
    apr_interval_time_t timeout;
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
#else
    apr_interval_time_t savetimeout;
    char savetimeout_set;
#endif
#if AP_MODULE_MAGIC_AT_LEAST(20051115,4)
#else
    proxy_cluster_helper *helperping;
#endif
    proxy_conn_rec *backend = NULL;
    char server_portstr[32];
    char *locurl = url;
    apr_uri_t *uri;
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
    char *scheme = worker->s->scheme;
#else
    const char *scheme = worker->scheme;
#endif
    int is_ssl = 0;

    if ((strcasecmp(scheme, "HTTPS") == 0 ||
        strcasecmp(scheme, "WSS") == 0 ||
        strcasecmp(scheme, "WS") == 0 ||
        strcasecmp(scheme, "HTTP") == 0) &&
        !enable_options) {
        /* we cant' do CPING/CPONG so we just return OK */
        return APR_SUCCESS;
    }
    if (strcasecmp(scheme, "HTTPS") == 0 ||
        strcasecmp(scheme, "WSS") == 0 ) {

        if (!ap_proxy_ssl_enable(NULL)) {
            ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                         "proxy_cluster_try_pingpong: cping_cpong failed (mod_ssl not configured?)");
            return APR_EGENERAL;
        }
        is_ssl = 1;
    }

    /* create space for state information */
    status = ap_proxy_acquire_connection(scheme, &backend, worker, r->server);
    if (status != OK) {
        if (backend) {
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
            backend->close = 1;
#else
            backend->close_on_recycle = 1;
#endif
            ap_proxy_release_connection(scheme, backend, r->server);
        }
        return status;
    }

    backend->is_ssl = is_ssl;
    if (is_ssl) {
        ap_proxy_ssl_connection_cleanup(backend, r);
    }

    /* Step One: Determine Who To Connect To */
    uri = apr_palloc(r->pool, sizeof(*uri)); /* We don't use it anyway */
    server_portstr[0] = '\0';
    status = ap_proxy_determine_connection(r->pool, r, conf, worker, backend,
                                           uri, &locurl,
                                           NULL, 0,
                                           server_portstr,
                                           sizeof(server_portstr));
    if (status != OK) {
        ap_proxy_release_connection(scheme, backend, r->server);
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
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
    /* Do nothing: 2.4.x has the ping_timeout and conn_timeout */
    timeout = worker->s->ping_timeout;
    if (timeout <= 0)
        timeout =  apr_time_from_sec(10); /* 10 seconds */
#else
#if AP_MODULE_MAGIC_AT_LEAST(20051115,4)
    timeout = worker->ping_timeout;
#else
    helperping = (proxy_cluster_helper *) worker->opaque;
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
    } else {
        savetimeout_set = 0;
        savetimeout = 0;
    }
#else
    /* XXX: side effects the worker may be used in another socket */
    savetimeout_set = worker->timeout_set;
    savetimeout = worker->timeout;
    worker->timeout_set = 1;
    worker->timeout = timeout;
#endif
#endif /* AP_MODULE_MAGIC_AT_LEAST(20101223,1) */

    /* Step Two: Make the Connection */
    status = ap_proxy_connect_backend(scheme, backend, worker, r->server);
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
    /* Do nothing: 2.4.x has the ping_timeout and conn_timeout */
#else
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
#endif /* AP_MODULE_MAGIC_AT_LEAST(20101223,1) */
    if (status != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                     "proxy_cluster_try_pingpong: can't connect to backend");
        ap_proxy_release_connection(scheme, backend, r->server);
        return status;
    } else {
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                     "proxy_cluster_try_pingpong: connected to backend");
    }

    if (strcasecmp(scheme, "AJP") == 0) {
        status = ajp_handle_cping_cpong(backend->sock, r, timeout);
        if (status != APR_SUCCESS) {
            ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                         "proxy_cluster_try_pingpong: cping_cpong failed");
            backend->close = 1;
        }
        
    } else {
        /* non-AJP connections */
        if (!backend->connection) {
            if ((status = ap_proxy_connection_create(scheme, backend,
                                                     (conn_rec *) NULL, r->server)) == OK) {
                if (is_ssl) {
                    apr_table_set(backend->connection->notes, "proxy-request-hostname",
                                  uri->hostname);
                }
            } else {
                ap_proxy_release_connection(scheme, backend, r->server);
                return status;
            }
        }
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                "proxy_cluster_try_pingpong: trying %s"
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
                , backend->connection->client_ip);
#else
                , backend->connection->remote_ip);
#endif
        status = http_handle_cping_cpong(backend, r, timeout);
        if (status != APR_SUCCESS) {
            ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                         "proxy_cluster_try_pingpong: cping_cpong failed");
            backend->close = 1;
        }
    }
    ap_proxy_release_connection(scheme, backend, r->server);
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
    size = node_storage->get_max_size_node();
    if (size == 0)
        return;
    id = apr_pcalloc(pool, sizeof(int) * size);
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
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
            proxy_worker_shared *stat;
#else
            proxy_worker_stat *stat;
#endif
            char *ptr = (char *) ou;
            ptr = ptr + ou->offset;
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
            stat = (proxy_worker_shared *) ptr;
#else
            stat = (proxy_worker_stat *) ptr;
#endif
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
                proxy_worker *worker = get_worker_from_id_stat(conf, id[i], stat);
                if (worker == NULL)
                    continue; /* skip it */
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
                apr_snprintf(sport, sizeof(sport), ":%d", worker->s->port);
                if (strchr(worker->s->hostname, ':') != NULL)
                    url = apr_pstrcat(pool, worker->s->scheme, "://[", worker->s->hostname, "]", sport, "/", NULL);
                else
                    url = apr_pstrcat(pool, worker->s->scheme, "://", worker->s->hostname,  sport, "/", NULL);
#else
                apr_snprintf(sport, sizeof(sport), ":%d", worker->port);
                if (strchr(worker->hostname, ':') != NULL)
                    url = apr_pstrcat(pool, worker->scheme, "://[", worker->hostname, "]", sport, "/", NULL);
                else
                    url = apr_pstrcat(pool, worker->scheme, "://", worker->hostname,  sport, "/", NULL);
#endif

                apr_pool_create(&rrp, pool);
                apr_pool_tag(rrp, "subrequest");
                rnew = apr_pcalloc(rrp, sizeof(request_rec));
                rnew->pool = rrp;
                /* we need only those ones */
                rnew->server = server;
                rnew->connection = apr_pcalloc(rrp, sizeof(conn_rec));
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
                rnew->connection->log_id = "-";
                rnew->log_id = "-";
                rnew->useragent_addr = apr_pcalloc(rrp, sizeof(apr_sockaddr_t));
#endif
                rnew->per_dir_config = server->lookup_defaults;
                rnew->notes = apr_table_make(rnew->pool, 1);
                rnew->method = "PING";
                rnew->uri = "/";
                rnew->headers_in = apr_table_make(rnew->pool, 1);
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
    size = sessionid_storage->get_max_size_sessionid();
    if (size == 0)
        return;
    id = apr_pcalloc(pool, sizeof(int) * size);
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
    size = domain_storage->get_max_size_domain();
    if (size == 0)
        return;
    id = apr_pcalloc(pool, sizeof(int) * size);
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
                    /* remove " from version1 cookies */
                    if (*cookie == '\"' && *(cookie+strlen(cookie)-1) == '\"') {
                        ++cookie;
                        *(cookie+strlen(cookie)-1) = '\0';
                        cookie = apr_pstrdup(r->pool, cookie);
                    }
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
 * @param nodeid the node id.
 * @param route (if received)
 * @return 1 is it finds a sessionid 0 otherwise.
 */
static int hassession_byname(request_rec *r, int nodeid, const char *route)
{
    proxy_balancer *balancer = NULL;
    char *sessionid;
    char *uri;
    char *sticky_used;
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
    char *sticky;
#else
    const char *sticky;
#endif
    int i;
    proxy_server_conf *conf;
    nodeinfo_t *node;
    int sizeb;
    char *ptr;

   /* well we already have it */
    if (route != NULL && (*route != '\0'))
        return 1;

    if (node_storage->read_node(nodeid, &node) != APR_SUCCESS)
        return 0; /* failed */

    conf = (proxy_server_conf *) ap_get_module_config(r->server->module_config, &proxy_module);
    sizeb = conf->balancers->elt_size;
    ptr = conf->balancers->elts;
    for (i = 0; i < conf->balancers->nelts; i++, ptr=ptr+sizeb) {
        balancer = (proxy_balancer *) ptr;
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
        if (strlen(balancer->s->name) > 11 && strcasecmp(&balancer->s->name[11], node->mess.balancer) == 0)
#else
        if (strlen(balancer->name) > 11 && strcasecmp(&balancer->name[11], node->mess.balancer) == 0)
#endif
            break;
    }
    if (i == conf->balancers->nelts)
        balancer = NULL;

    /* XXX: We don't find the balancer, that is BAD */
    if (balancer == NULL)
        return 0;

#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
    sticky = apr_psprintf(r->pool, "%s|%s", balancer->s->sticky, balancer->s->sticky_path);
#else
    sticky = balancer->sticky;
#endif
    if (sticky == NULL)
        return 0;

    if (r->filename)
        uri = r->filename + 6;
    else {
        /* We are coming from proxy_cluster_trans */
        uri = r->unparsed_uri;
    }

    sessionid = cluster_get_sessionid(r, sticky, uri, &sticky_used);
    if (sessionid) {
#if HAVE_CLUSTER_EX_DEBUG
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                     "mod_proxy_cluster: found sessionid %s", sessionid);
#endif
        return 1;
    }
    return 0;
}


/* Read the virtual host table from shared memory */
static proxy_vhost_table *read_vhost_table(request_rec *r)
{
    int i;
    int size;
    proxy_vhost_table *vhost_table = apr_palloc(r->pool, sizeof(proxy_vhost_table));
    size = host_storage->get_max_size_host();
    if (size == 0) {
        vhost_table->sizevhost = 0;
        vhost_table->vhosts = NULL;
        vhost_table->vhost_info = NULL;
        return vhost_table;
    }

    vhost_table->vhosts =  apr_palloc(r->pool, sizeof(int) * host_storage->get_max_size_host());
    vhost_table->sizevhost = host_storage->get_ids_used_host(vhost_table->vhosts);
    vhost_table->vhost_info = apr_palloc(r->pool, sizeof(hostinfo_t) * vhost_table->sizevhost);
    for (i = 0; i < vhost_table->sizevhost; i++) {
        hostinfo_t* h;
        int host_index = vhost_table->vhosts[i];
        host_storage->read_host(host_index, &h);
        vhost_table->vhost_info[i] = *h;
    }
    return vhost_table;
}

/* Read the context table from shared memory */
static proxy_context_table *read_context_table(request_rec *r)
{
    int i;
    int size;
    proxy_context_table *context_table = apr_palloc(r->pool, sizeof(proxy_context_table));
    size = context_storage->get_max_size_context();
    if (size == 0) { 
        context_table->sizecontext = 0;
        context_table->contexts = NULL;
        context_table->context_info = NULL;
        return context_table;
    }
    context_table->contexts =  apr_palloc(r->pool, sizeof(int) * size);
    context_table->sizecontext = context_storage->get_ids_used_context(context_table->contexts);
    context_table->context_info = apr_palloc(r->pool, sizeof(contextinfo_t) * context_table->sizecontext);
    for (i = 0; i < context_table->sizecontext; i++) {
        contextinfo_t* h;
        int context_index = context_table->contexts[i];
        context_storage->read_context(context_index, &h);
        context_table->context_info[i] = *h;
    }
    return context_table;
}

/* Read the balancer table from shared memory */
static proxy_balancer_table *read_balancer_table(request_rec *r)
{
    int i;
    int size;
    proxy_balancer_table *balancer_table = apr_palloc(r->pool, sizeof(proxy_balancer_table));
    size = balancer_storage->get_max_size_balancer();
    if (size == 0) { 
        balancer_table->sizebalancer = 0;
        balancer_table->balancers = NULL;
        balancer_table->balancer_info = NULL;
        return balancer_table;
    }
    balancer_table->balancers =  apr_palloc(r->pool, sizeof(int) * size);
    balancer_table->sizebalancer = balancer_storage->get_ids_used_balancer(balancer_table->balancers);
    balancer_table->balancer_info = apr_palloc(r->pool, sizeof(balancerinfo_t) * balancer_table->sizebalancer);
    for (i = 0; i < balancer_table->sizebalancer; i++) {
        balancerinfo_t* h;
        int balancer_index = balancer_table->balancers[i];
        balancer_storage->read_balancer(balancer_index, &h);
        balancer_table->balancer_info[i] = *h;
    }
    return balancer_table;
}

/* Read the node table from shared memory */
static proxy_node_table *read_node_table(request_rec *r)
{
    int i;
    int size;
    proxy_node_table *node_table =  apr_palloc(r->pool, sizeof(proxy_node_table));
    size = node_storage->get_max_size_node();
    if (size == 0) { 
        node_table->sizenode = 0;
        node_table->nodes = NULL;
        node_table->node_info = NULL;
        return node_table;
    }
    node_table->nodes =  apr_palloc(r->pool, sizeof(int) * size);
    node_table->sizenode = node_storage->get_ids_used_node(node_table->nodes);
    node_table->node_info = apr_palloc(r->pool, sizeof(nodeinfo_t) * node_table->sizenode);
    for (i = 0; i < node_table->sizenode; i++) {
        nodeinfo_t* h;
        int node_index = node_table->nodes[i];
        node_storage->read_node(node_index, &h);
        node_table->node_info[i] = *h;
    }
    return node_table;
}

/* Read a node from the table using its it */
static  nodeinfo_t* table_get_node(proxy_node_table *node_table, int id)
{
    int i;
    for (i = 0; i < node_table->sizenode; i++) {
        if (node_table->nodes[i] == id)
            return &node_table->node_info[i];
    }
    return NULL;
}

/**
 * Find the best nodes for a request (check host and context (and balancer))
 * @param r the request_rec
 * @param balancer the balancer (balancer to use in that case we check it).
 * @param route from the sessionid if we have one.
 * @param use_alias compare alias with server_name
 * @return a pointer to a list of nodes.
 */
static node_context *find_node_context_host(request_rec *r, proxy_balancer *balancer, const char *route, int use_alias, proxy_vhost_table* vhost_table, proxy_context_table* context_table, proxy_node_table *node_table)
{
    int sizecontext = context_table->sizecontext;
    int *contexts;
    int *length;
    int *status;
    int i, j, max;
    node_context *best;
    int nbest;
    const char *uri = NULL;
    const char *luri = NULL;

    /* use r->uri (trans) or r->filename (after canon or rewrite) */
    if (r->filename) {
        const char *scheme = strstr(r->filename, "://");
        if (scheme)
            luri = ap_strchr_c(scheme + 3, '/');
    }
    if (!luri)
       luri = r->uri;
    uri = ap_strchr_c(luri, '?');
    if (uri)
       uri = apr_pstrndup(r->pool, luri, uri - luri);
    else {
       uri = ap_strchr_c(luri, ';');
       if (uri)
          uri = apr_pstrndup(r->pool, luri, uri - luri);
       else
          uri = luri;
    }

    /* read the contexts */
    if (sizecontext == 0)
        return NULL;
    contexts =  apr_palloc(r->pool, sizeof(int)*sizecontext);
    for (i=0; i < sizecontext; i++)
        contexts[i] = i;
    length =  apr_pcalloc(r->pool, sizeof(int)*sizecontext);
    status =  apr_palloc(r->pool, sizeof(int)*sizecontext);
    /* Check the virtual host */
    if (use_alias) {
        /* read the hosts */
        int sizevhost;
        int *contextsok = apr_pcalloc(r->pool, sizeof(int)*sizecontext);
        const char *hostname = ap_get_server_name(r);
#if HAVE_CLUSTER_EX_DEBUG
    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                     "find_node_context_host: Host: %s", hostname);
#endif
        sizevhost = vhost_table->sizevhost;
        for (i=0; i<sizevhost; i++) {
            hostinfo_t *vhost = vhost_table->vhost_info + i;
            if (strcmp(hostname, vhost->host) == 0) {
                /* add the contexts that match */
                for (j=0; j<sizecontext; j++) {
                    contextinfo_t *context = &context_table->context_info[j];
                    if (context->vhost == vhost->vhost && context->node == vhost->node)
                        contextsok[j] = 1;
                }
            }
        }
        for (j=0; j<sizecontext; j++) {
            if (!contextsok[j])
                contexts[j] = -1;
        }
    }
#if HAVE_CLUSTER_EX_DEBUG
    for (j=0; j<sizecontext; j++) {
        contextinfo_t *context;
        if (contexts[j] == -1) continue;
        context = &context_table->context_info[j]; 
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                         "find_node_context_host: %s node: %d vhost: %d context: %s",
                          uri, context->node, context->vhost, context->context);
    }
#endif

    /* Check the contexts */
    max = 0;
    for (j=0; j<sizecontext; j++) {
        contextinfo_t *context;
        int len;
        if (contexts[j] == -1) continue;
        context = &context_table->context_info[j];

        /* keep only the contexts corresponding to our balancer */
        if (balancer != NULL) {

            nodeinfo_t *node =  table_get_node(node_table, context->node);
            if (node == NULL)
                continue;
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
            if (strlen(balancer->s->name) > 11 && strcasecmp(&balancer->s->name[11], node->mess.balancer) != 0)
#else
            if (strlen(balancer->name) > 11 && strcasecmp(&balancer->name[11], node->mess.balancer) != 0)
#endif
                continue;
        }
        len = strlen(context->context);
        if (strncmp(uri, context->context, len) == 0) {
            if (uri[len] == '\0' || uri[len] == '/' || len==1) {
                status[j] = context->status;
                length[j] = len;
                if (len > max) {
                    max = len;
                } 
            }
        }
    }
    if (max == 0)
        return NULL;


    /* find the best matching contexts */
    nbest = 1;
    for (j=0; j<sizecontext; j++)
        if (length[j] == max)
            nbest++;
    best =  apr_palloc(r->pool, sizeof(node_context)*nbest);
    nbest  = 0;
    for (j=0; j<sizecontext; j++)
        if (length[j] == max) {
            contextinfo_t *context;
            int ok = 0;
            context = &context_table->context_info[j];
            /* Check status */
            switch (status[j]) {
                case ENABLED:
                    ok = -1;
                    break;
                case DISABLED:
                    /* Only the request with sessionid ok for it */
                    if (hassession_byname(r, context->node, route)) {
                        ok = -1;
                    }
                    break;
            }
            if (ok) {
                best[nbest].node = context->node;
                best[nbest].context = context->id;
                nbest++;
            }
        }
    if (nbest == 0)
        return NULL;
    best[nbest].node = -1;
    return best; 
}

/**
 * Search the balancer that corresponds to the pair context/host
 * @param r the request_rec.
 * @vhost_table table of host virtual hosts.
 * @context_table table of contexts.
 * @return the balancer name or NULL if not found.
 */ 
static char *get_context_host_balancer(request_rec *r,
		proxy_vhost_table *vhost_table, proxy_context_table *context_table, proxy_node_table *node_table)
{
    void *sconf = r->server->module_config;
    proxy_server_conf *conf = (proxy_server_conf *)
        ap_get_module_config(sconf, &proxy_module);

    node_context *nodes =  find_node_context_host(r, NULL, NULL, use_alias, vhost_table, context_table, node_table);
    if (nodes == NULL)
        return NULL;
    while ((*nodes).node != -1) {
        nodeinfo_t *node;
        if (node_storage->read_node((*nodes).node, &node) != APR_SUCCESS) {
            nodes++;
            continue;
        }
        if (node->mess.balancer) {
            /* Check that it is in our proxy_server_conf */
            char *name = apr_pstrcat(r->pool, "balancer://", node->mess.balancer, NULL);
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
            proxy_balancer *balancer = ap_proxy_get_balancer(r->pool, conf, name, 0);
#else
            proxy_balancer *balancer = ap_proxy_get_balancer(r->pool, conf, name);
#endif
            if (balancer)
                return node->mess.balancer;
            else
                 ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                             "get_context_host_balancer: balancer %s not found", name);
        }
        nodes++;
    }
    return NULL;
}
/*
 * Return the node cotenxt Check that the worker will handle the host/context.
 * de
 * The id of the worker is used to find the (slot) node in the shared
 * memory.
 * (See get_context_host_balancer too).
 */ 
static node_context *context_host_ok(request_rec *r, proxy_balancer *balancer, int node,
                             proxy_vhost_table *vhost_table, proxy_context_table *context_table, proxy_node_table *node_table)
{
    const char *route;
    node_context *best;
    route = apr_table_get(r->notes, "session-route");
    best = find_node_context_host(r, balancer, route, use_alias, vhost_table, context_table, node_table);
    if (best == NULL)
        return NULL;
    while ((*best).node != -1) {
        if ((*best).node == node) break;
        best++;
    }
    if ((*best).node == -1)
        return NULL; /* not found */
    return best;
}

/*
 * Check that the balancer is in our balancer table
 */
static int isbalancer_ours(proxy_balancer *balancer, proxy_balancer_table *balancer_table)
{
    int i;
    for (i = 0; i < balancer_table->sizebalancer; i++) {
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
      if (strcasecmp(balancer_table->balancer_info[i].balancer, &balancer->s->name[11]))
#else
      if (strcasecmp(balancer_table->balancer_info[i].balancer, &balancer->name[11]))
#endif
            continue;
        else
            return 1; /* found */
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
                                         request_rec *r, const char *domain, int failoverdomain,
                                         proxy_vhost_table *vhost_table,
                                         proxy_context_table *context_table, proxy_node_table *node_table)
{
    int i;
    proxy_worker *mycandidate = NULL;
    node_context *mynodecontext = NULL;
    proxy_worker *worker;
    int checking_standby = 0;
    int checked_standby = 0;
    int checked_domain = 1;

#if HAVE_CLUSTER_EX_DEBUG
    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                 "proxy: Entering byrequests for CLUSTER (%s) failoverdomain:%d",
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
                 balancer->s->name,
#else
                 balancer->name,
#endif
                 failoverdomain);
#endif

    /* create workers for new nodes */
    update_workers_node(conf, r->pool, r->server, 1);

    /* First try to see if we have available candidate */
    if (domain && strlen(domain)>0)
        checked_domain = 0;
    while (!checked_standby) {
        char *ptr = balancer->workers->elts;
        int sizew = balancer->workers->elt_size;
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
        for (i = 0; i < balancer->workers->nelts; i++, ptr=ptr+sizew) {
            node_context *nodecontext;
            nodeinfo_t *node;
            proxy_cluster_helper *helper;
            proxy_worker **run = (proxy_worker **) ptr;

            worker = *run;
            helper = (proxy_cluster_helper *) worker->context;
            if (!worker->s)
                continue;
            if (helper->index == 0)
                continue; /* marked removed */
            if (helper->index != worker->s->index) {
                /* something is very bad */
                ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                             "proxy: byrequests balancer skipping BAD worker");
                continue; /* probably used by different worker */
            }
#else
        for (i = 0; i < balancer->workers->nelts; i++, ptr=ptr+sizew) {
            node_context *nodecontext;
            nodeinfo_t *node;
            worker = (proxy_worker *) ptr;
            if (worker->id == 0)
                continue; /* marked removed */
#endif

            /* standby logic
             * lbfactor: -1 broken node.
             *            0 standby.
             *           >0 factor to use.
             */
            if (worker->s == NULL || worker->s->lbfactor < 0 || (worker->s->lbfactor == 0 && !checking_standby))
                continue;

            /* If the worker is in error state the STATUS logic will retry it */
            if (!PROXY_WORKER_IS_USABLE(worker)) {
                continue;
            }

            /* Take into calculation only the workers that are
             * not in error state or not disabled.
             * and that can map the context.
             */
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
            if (node_storage->read_node(worker->s->index, &node) != APR_SUCCESS)
#else
            if (node_storage->read_node(worker->id, &node) != APR_SUCCESS)
#endif
                continue; /* Can't read node */

#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
            if (PROXY_WORKER_IS_USABLE(worker) && (nodecontext = context_host_ok(r, balancer, worker->s->index, vhost_table, context_table, node_table)) != NULL) {
#else
            if (PROXY_WORKER_IS_USABLE(worker) && (nodecontext = context_host_ok(r, balancer, worker->id, vhost_table, context_table, node_table)) != NULL) {
#endif
                if (!checked_domain) {
                    /* First try only nodes in the domain */
                    if (!isnode_domain_ok(r, node, domain)) {
                        continue;
                    }
                }
                if (worker->s->lbfactor == 0 && checking_standby) {
                    mycandidate = worker;
                    mynodecontext = nodecontext;
                    break; /* Done */
                } else {
                    if (!mycandidate) {
                        mycandidate = worker;
                        mynodecontext = nodecontext;
                    } else {
                        nodeinfo_t *node1;
                        int lbstatus, lbstatus1;

#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
                        if (node_storage->read_node(mycandidate->s->index, &node1) != APR_SUCCESS)
#else
                        if (node_storage->read_node(mycandidate->id, &node1) != APR_SUCCESS)
#endif
                            continue;
                        lbstatus1 = ((mycandidate->s->elected - node1->mess.oldelected) * 1000)/mycandidate->s->lbfactor;
                        lbstatus  = ((worker->s->elected - node->mess.oldelected) * 1000)/worker->s->lbfactor;
                        lbstatus1 = lbstatus1 + mycandidate->s->lbstatus;
                        lbstatus = lbstatus + worker->s->lbstatus;
                        if (lbstatus1> lbstatus) {
                            mycandidate = worker;
                            mynodecontext = nodecontext;
                        }
                    }
                }
            }
        }
        if (mycandidate)
            break;
        if (failoverdomain)
             break; /* We only failover in the domain */
        if (checked_domain) {
            checked_standby = checking_standby++;
        }
        checked_domain++;
    }

    if (mycandidate) {
        /* Failover in domain */
        if (!checked_domain)
            apr_table_setn(r->notes, "session-domain-ok", "1");
        mycandidate->s->elected++;
        apr_table_setn(r->subprocess_env, "BALANCER_CONTEXT_ID", apr_psprintf(r->pool, "%d", (*mynodecontext).context));
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                             "proxy: byrequests balancer DONE (%s)",
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
                             mycandidate->s->name
#else
                             mycandidate->name
#endif
                             );
    } else {
        apr_table_setn(r->subprocess_env, "BALANCER_CONTEXT_ID", "");
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                             "proxy: byrequests balancer FAILED");
    }
    return mycandidate;
}
/*
 * Wrapper to mod_balancer "standard" interface.
 */
static proxy_worker *find_best_byrequests(proxy_balancer *balancer, request_rec *r)
{
    void *sconf = r->server->module_config;
    proxy_server_conf *conf = (proxy_server_conf *)
        ap_get_module_config(sconf, &proxy_module);

    proxy_vhost_table *vhost_table = read_vhost_table(r);
    proxy_context_table *context_table = read_context_table(r);
    proxy_node_table *node_table = read_node_table(r);

    return internal_find_best_byrequests(balancer, conf, r, NULL, 0, vhost_table, context_table, node_table);
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
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
    proxy_worker_shared *stat;
#else
    proxy_worker_stat *stat;
#endif
    char *ptr;

    if (node_storage->read_node(id, &node) != APR_SUCCESS)
        return 500;

    /* Calculate the address of our shared memory that corresponds to the stat info of the worker */
    ptr = (char *) node;
    ptr = ptr + node->offset;
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
    stat = (proxy_worker_shared *) ptr;
#else
    stat = (proxy_worker_stat *) ptr;
#endif

    /* create the balancers and workers (that could be the first time) */
    apr_thread_mutex_lock(lock);
    add_balancers_workers(node, r->pool);
    apr_thread_mutex_unlock(lock);

    /* search for the worker in the VirtualHosts */ 
    while (s) {
        void *sconf = s->module_config;
        conf = (proxy_server_conf *) ap_get_module_config(sconf, &proxy_module);

        worker = get_worker_from_id_stat(conf, id, stat);
        if (worker != NULL)
            break;
        s = s->next;
    }
    if (worker == NULL) {
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                     "proxy_cluster_isup: Can't find worker for %d. Check balancer names.", id);
        return 500;
    }

    /* Try a  ping/pong to check the node */
    if (load >= 0 || load == -2) {
        /* Only try usuable nodes */
        char sport[7];
        char *url;
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
        apr_snprintf(sport, sizeof(sport), ":%d", worker->s->port);
        if (strchr(worker->s->hostname, ':') != NULL)
            url = apr_pstrcat(r->pool, worker->s->scheme, "://[", worker->s->hostname, "]", sport, "/", NULL);
        else
            url = apr_pstrcat(r->pool, worker->s->scheme, "://", worker->s->hostname,  sport, "/", NULL);
#else
        apr_snprintf(sport, sizeof(sport), ":%d", worker->port);
        if (strchr(worker->hostname, ':') != NULL)
            url = apr_pstrcat(r->pool, worker->scheme, "://[", worker->hostname, "]", sport, "/", NULL);
        else
            url = apr_pstrcat(r->pool, worker->scheme, "://", worker->hostname,  sport, "/", NULL);
#endif
        worker->s->error_time = 0; /* Force retry now */
        rv = proxy_cluster_try_pingpong(r, worker, url, conf, node->mess.ping, node->mess.timeout);
        if (rv != APR_SUCCESS) {
            worker->s->status |= PROXY_WORKER_IN_ERROR;
            ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                         "proxy_cluster_isup: pingpong %s failed", url);
            return 500;
        }
    }
    if (load == -2) {
        return 0;
    }
    else if (load == -1) {
        worker->s->status |= PROXY_WORKER_IN_ERROR;
        worker->s->lbfactor = -1;
    }
    else if (load == 0) {
#if AP_MODULE_MAGIC_AT_LEAST(20051115,4)
        worker->s->status |= PROXY_WORKER_HOT_STANDBY;
        worker->s->lbfactor = 0;
#else
        /*
         * XXX: PROXY_WORKER_HOT_STANDBY Doesn't look supported
         * mark worker in error for the moment
         */
        worker->s->status |= PROXY_WORKER_IN_ERROR;
        worker->s->lbfactor = -1;
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
    size = node_storage->get_max_size_node();
    if (size == 0) {
        return;
    }
    id = apr_pcalloc(pool, sizeof(int) * size);
    size = node_storage->get_ids_used_node(id);
    for (i=0; i<size; i++) {
        nodeinfo_t *ou;
        if (node_storage->read_node(id[i], &ou) != APR_SUCCESS)
            continue;
        if (ou->mess.remove && (now - ou->updatetime) >= wait_for_remove &&
            (now - ou->mess.lastcleantry) >= wait_for_remove) {
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
    size = node_storage->get_max_size_node();
    if (size == 0) {
        apr_thread_mutex_unlock(lock);
        return;
    }
    id = apr_pcalloc(pool, sizeof(int) * size);
    size = node_storage->get_ids_used_node(id);
    for (i=0; i<size; i++) {
        nodeinfo_t *ou;
        if (node_storage->read_node(id[i], &ou) != APR_SUCCESS)
            continue;
        if (ou->mess.remove) {
            remove_workers_node(ou, conf, pool, server);
        }
    }
    apr_thread_mutex_unlock(lock);
}
static void * APR_THREAD_FUNC proxy_cluster_watchdog_func(apr_thread_t *thd, void *data)
{
    apr_status_t rv;
    apr_pool_t *pool;
    apr_sleep(apr_time_make(1, 0)); /* wait before starting */
    for (;;) {
        server_rec *s = main_server;
        void *sconf = s->module_config;
        proxy_server_conf *conf = (proxy_server_conf *)
            ap_get_module_config(sconf, &proxy_module);
        unsigned int last;

        if (!conf)
           break;

        apr_thread_mutex_lock(lock);
        if (watchdog_must_terminate) {
            apr_thread_mutex_unlock(lock);
            break;
        }
        rv = apr_thread_cond_timedwait(exit_cond, lock, apr_time_make(1, 0));
        apr_thread_mutex_unlock(lock);
        if (rv == APR_SUCCESS)
            break; /* If condition variable was signaled, terminate. */
        if (rv != APR_TIMEUP) {
            ap_log_error(APLOG_MARK, APLOG_ERR, rv, main_server, "cluster: apr_thread_cond_timedwait() failed");
        }

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
            node_storage->worker_nodes_are_updated(main_server, last);
    }
    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, main_server, "cluster: Watchdog thread exiting cleanly.");
    apr_thread_exit(thd, 0);
    return NULL;
}

/*
 * Tell the watchdog thread to exit and wait for it to
 * complete.
 */
static int terminate_watchdog(void *data)
{
    apr_status_t rv, trv;

    if (watchdog_thread == NULL)
        return APR_SUCCESS;

    apr_thread_mutex_lock(lock);
    watchdog_must_terminate = 1;
    rv = apr_thread_cond_signal(exit_cond);
    apr_thread_mutex_unlock(lock);
    if (rv != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, rv, main_server,
                    "terminate_watchdog: apr_thread_cond_signal failed");
        return APR_SUCCESS; /* There isn't a lot we can do about this. */
    }

    rv = apr_thread_join(&trv, watchdog_thread);
    if (rv != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, rv, main_server,
                    "terminate_watchdog: apr_thread_join failed");
    }

    return APR_SUCCESS;
}

/*
 * Create a thread per process to make maintenance task.
 * and the mutex of the node creation.
 */
static void  proxy_cluster_child_init(apr_pool_t *p, server_rec *s)
{
    apr_status_t rv;
    void *sconf = s->module_config;
    proxy_server_conf *conf = (proxy_server_conf *) ap_get_module_config(sconf, &proxy_module);

    main_server = s;

    rv = apr_thread_mutex_create(&lock, APR_THREAD_MUTEX_DEFAULT, p);
    if (rv != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR|APLOG_NOERRNO, 0, s,
                    "proxy_cluster_child_init: apr_thread_mutex_create failed");
    }

    rv = apr_thread_cond_create(&exit_cond, p);
    if (rv != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR|APLOG_NOERRNO, 0, s,
                    "proxy_cluster_child_init: apr_thread_cond_create failed");
    }

    if (conf) {
        apr_pool_t *pool;
        apr_pool_create(&pool, conf->pool);
        while (s) {
            sconf = s->module_config;
            conf = (proxy_server_conf *)
                ap_get_module_config(sconf, &proxy_module);

            update_workers_node(conf, pool, s, 0);

            s = s->next;
        }
        apr_pool_destroy(pool);
    }

    rv = apr_thread_create(&watchdog_thread, NULL, proxy_cluster_watchdog_func, main_server, p);
    if (rv != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR|APLOG_NOERRNO, 0, main_server,
                    "proxy_cluster_child_init: apr_thread_create failed");
    }

    apr_pool_pre_cleanup_register(p, NULL, terminate_watchdog);
}

static int proxy_cluster_post_config(apr_pool_t *p, apr_pool_t *plog,
                                     apr_pool_t *ptemp, server_rec *s)
{
    void *sconf = s->module_config;
    proxy_server_conf *conf = (proxy_server_conf *)ap_get_module_config(sconf, &proxy_module);
    int sizew = conf->workers->elt_size;
    int sizeb = conf->balancers->elt_size;
    if (sizew != sizeof(proxy_worker) || sizeb != sizeof(proxy_balancer)) {
        ap_version_t version;
        ap_get_server_revision(&version);

        ap_log_error(APLOG_MARK, APLOG_WARNING, 0, s,
                     "httpd version %d.%d.%d doesn't match version %d.%d.%d used by mod_proxy_cluster.c",
                      version.major, version.minor, version.patch,
                      AP_SERVER_MAJORVERSION_NUMBER, AP_SERVER_MINORVERSION_NUMBER, AP_SERVER_PATCHLEVEL_NUMBER);
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
#else
        if (version.patch < 8) {
            ap_log_error(APLOG_MARK, APLOG_WARNING, 0, s,
                         "httpd version %d.%d.%d too old", version.major, version.minor, version.patch);
            return HTTP_INTERNAL_SERVER_ERROR;
        }
#endif
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
    /* if Maxsessionid = 0 switch of the sessionid storing logic */
    if (!sessionid_storage->get_max_size_sessionid()) {
        sessionid_storage = NULL; /* don't use it */
    }

    domain_storage = ap_lookup_provider("manager" , "shared", "5");
    if (domain_storage == NULL) {
        ap_log_error(APLOG_MARK, APLOG_ERR|APLOG_NOERRNO, 0, s,
                    "proxy_cluster_post_config: Can't find mod_manager for domains");
        return !OK;
    }
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
    if (!ap_proxy_retry_worker_fn) {
        ap_proxy_retry_worker_fn =
                APR_RETRIEVE_OPTIONAL_FN(ap_proxy_retry_worker);
        if (!ap_proxy_retry_worker_fn) {
            ap_log_error(APLOG_MARK, APLOG_EMERG, 0, s,
                         "mod_proxy must be loaded for mod_proxy_cluster");
            return !OK;
        }
    }
#endif

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
        if (!strcasecmp(balancer, ou->mess.balancer)) {
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
static const char *get_route_balancer(request_rec *r, proxy_server_conf *conf,
                                      proxy_vhost_table *vhost_table,
                                      proxy_context_table *context_table,
                                      proxy_balancer_table *balancer_table,
                                      proxy_node_table *node_table)
{
    char *route = NULL;
    char *sessionid = NULL;
    char *sticky_used;
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
    char *sticky;
#else
    const char *sticky;
#endif
    int i;
    char *ptr = conf->balancers->elts;
    int sizeb = conf->balancers->elt_size;

    for (i = 0; i < conf->balancers->nelts; i++, ptr=ptr+sizeb) {
        proxy_balancer *balancer = (proxy_balancer *) ptr;

#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
        if (balancer->s->sticky[0] == '\0' || balancer->s->sticky_path == '\0')
            continue;
        if (strlen(balancer->s->name)<=11)
            continue;
        sticky = apr_psprintf(r->pool, "%s|%s", balancer->s->sticky, balancer->s->sticky_path);
#else
        if (balancer->sticky == NULL)
            continue;
        if (strlen(balancer->name)<=11)
            continue;
        sticky = balancer->sticky;
#endif
        if (!isbalancer_ours(balancer, balancer_table))
            continue;

        sessionid = cluster_get_sessionid(r, sticky, r->uri, &sticky_used);
        if (sessionid) {
            ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                         "cluster: %s Found value %s for "
                         "stickysession %s",
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
                         balancer->s->name,
#else
                         balancer->name,
#endif
                         sessionid, sticky);
            if ((route = strchr(sessionid, '.')) != NULL )
                route++;
            if (route && *route) {
                /* Nice we have a route, but make sure we have to serve it */
                node_context *nodes = find_node_context_host(r, balancer, route, use_alias, vhost_table, context_table, node_table);
                if (nodes == NULL)
                    continue; /* we can't serve context/host for the request with this balancer*/
            }
            if (route && *route) {
                char *domain = NULL;
#if HAVE_CLUSTER_EX_DEBUG
                ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                                          "cluster: Found route %s", route);
#endif
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
                if (find_nodedomain(r, &domain, route, &balancer->s->name[11]) == APR_SUCCESS) {
#else
                if (find_nodedomain(r, &domain, route, &balancer->name[11]) == APR_SUCCESS) {
#endif
#if HAVE_CLUSTER_EX_DEBUG
                    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                                "cluster: Found balancer %s for %s",
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
                                 &balancer->s->name[11],
#else
                                 &balancer->name[11],
#endif
                                 route);
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
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
                    return &balancer->s->name[11];
#else
                    return &balancer->name[11];
#endif
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

    proxy_vhost_table *vhost_table = read_vhost_table(r);
    proxy_context_table *context_table = read_context_table(r);
    proxy_balancer_table *balancer_table = read_balancer_table(r);
    proxy_node_table *node_table = read_node_table(r);

    apr_table_setn(r->notes, "balancer-table",  (char *) balancer_table);

#if HAVE_CLUSTER_EX_DEBUG
    ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_DEBUG, 0, r->server,
                "proxy_cluster_trans for %d %s %s uri: %s args: %s unparsed_uri: %s",
                 r->proxyreq, r->filename, r->handler, r->uri, r->args, r->unparsed_uri);
#endif

    balancer = get_route_balancer(r, conf, vhost_table, context_table, balancer_table, node_table);
    if (!balancer) {
        /* May be the balancer has not been created (we use shared memory to find the balancer name) */
        update_workers_node(conf, r->pool, r->server, 1);
        balancer = get_route_balancer(r, conf, vhost_table, context_table, balancer_table, node_table);
    }
    if (!balancer) {
        balancer = get_context_host_balancer(r, vhost_table, context_table, node_table);
    }
    

    if (balancer) {
        int i;
        int sizea = conf->aliases->elt_size;
        char *ptr = conf->aliases->elts;
        /* Check that we don't have a ProxyPassMatch ^(/.*\.gif)$ ! or something similar */
        for (i = 0; i < conf->aliases->nelts; i++, ptr=ptr+sizea) {
            struct proxy_alias *ent = (struct proxy_alias *) ptr;
            if (ent->real[0] == '!' && ent->real[1] == '\0') {
                ap_regmatch_t regm[AP_MAX_REG_MATCH];
                if (ent->regex) {
                    if (!ap_regexec(ent->regex, r->uri, AP_MAX_REG_MATCH, regm, 0)) {
#if HAVE_CLUSTER_EX_DEBUG
                        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_DEBUG, 0, r->server,
                                    "proxy_cluster_trans DECLINED %s uri: %s unparsed_uri: %s (match: regexp)",
                                     balancer, r->filename, r->unparsed_uri);
#endif
                        return DECLINED;
                    }
                }
                else {
                    const char *fake;
                    proxy_dir_conf *dconf = ap_get_module_config(r->per_dir_config,
                                                                 &proxy_module);
                    if ((dconf->interpolate_env == 1)
                        && (ent->flags & PROXYPASS_INTERPOLATE)) {
                        fake = proxy_interpolate(r, ent->fake);
                    }
                    else {
                        fake = ent->fake;
                    }
                    if (alias_match(r->uri, fake)) {
#if HAVE_CLUSTER_EX_DEBUG
                        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_DEBUG, 0, r->server,
                                    "proxy_cluster_trans DECLINED %s uri: %s unparsed_uri: %s (match: %s)",
                                     balancer, r->filename, r->unparsed_uri, fake);
#endif
                        return DECLINED;
                    }
                }
            }
        }

        /* It is safer to use r->uri */
        if (strncmp(r->uri, "balancer://",11))
            r->filename =  apr_pstrcat(r->pool, "proxy:balancer://", balancer, r->uri, NULL);
        else
            r->filename =  apr_pstrcat(r->pool, "proxy:", r->uri, NULL);
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
                                       proxy_balancer *balancer, const char *route,
                                       proxy_vhost_table *vhost_table,
                                       proxy_context_table *context_table,
                                       proxy_node_table *node_table)
{
    int i;
    int checking_standby;
    int checked_standby;
    int sizew = balancer->workers->elt_size;
    
    proxy_worker *worker;
    node_context *nodecontext;

    checking_standby = checked_standby = 0;
    while (!checked_standby) {
        char *ptr = balancer->workers->elts;
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
        for (i = 0; i < balancer->workers->nelts; i++, ptr=ptr+sizew) {
            proxy_worker **run = (proxy_worker **) ptr;
            int index = (*run)->s->index;
            proxy_cluster_helper *helper = (*run)->context;
            worker = *run;
            if (index != helper->index) {
                 ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                              "proxy: find_route_worker skipping BAD worker");
                 continue; /* skip it */
            }
#else
        for (i = 0; i < balancer->workers->nelts; i++, ptr=ptr+sizew) {
            int index;
            worker = (proxy_worker *) ptr;
            index = worker->id;
#endif
            if (index == 0)
                continue; /* marked removed */

            if ( (checking_standby ? !PROXY_WORKER_IS_STANDBY(worker) : PROXY_WORKER_IS_STANDBY(worker)) )
                continue;
            if (*(worker->s->route) && strcmp(worker->s->route, route) == 0) {
                /* that is the worker corresponding to the route */
                if (worker && PROXY_WORKER_IS_USABLE(worker)) {
                    /* The context may not be available */
                    nodeinfo_t *node;
                    if (node_storage->read_node(index, &node) != APR_SUCCESS)
                        return NULL; /* can't read node */
                    if ((nodecontext = context_host_ok(r, balancer, index, vhost_table, context_table, node_table)) != NULL) {
                        apr_table_setn(r->subprocess_env, "BALANCER_CONTEXT_ID", apr_psprintf(r->pool, "%d", (*nodecontext).context));
                        return worker;
                    } else {
                        return NULL; /* application has been removed from the node */
                    }
                } else {
                    /*
                     * If the worker is in error state run
                     * retry on that worker. It will be marked as
                     * operational if the retry timeout is elapsed.
                     * The worker might still be unusable, but we try
                     * anyway.
                     */
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
                    ap_proxy_retry_worker_fn("BALANCER", worker, r->server);
#else
                    ap_proxy_retry_worker("BALANCER", worker, r->server);
#endif
                    if (PROXY_WORKER_IS_USABLE(worker)) {
                            /* The context may not be available */
                            nodeinfo_t *node;
                            if (node_storage->read_node(index, &node) != APR_SUCCESS)
                                return NULL; /* can't read node */
                            if ((nodecontext = context_host_ok(r, balancer, index, vhost_table, context_table, node_table)) != NULL) {
                                apr_table_setn(r->subprocess_env, "BALANCER_CONTEXT_ID", apr_psprintf(r->pool, "%d", (*nodecontext).context));
                                return worker;
                            } else {
                                return NULL; /* application has been removed from the node */
                            }
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
                            rworker = find_route_worker(r, balancer, worker->s->redirect,
                            		vhost_table, context_table, node_table);
                            /* Check if the redirect worker is usable */
                            if (rworker && !PROXY_WORKER_IS_USABLE(rworker)) {
                                /*
                                 * If the worker is in error state run
                                 * retry on that worker. It will be marked as
                                 * operational if the retry timeout is elapsed.
                                 * The worker might still be unusable, but we try
                                 * anyway.
                                 */
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
                                ap_proxy_retry_worker_fn("BALANCER", worker, r->server);
#else
                                ap_proxy_retry_worker("BALANCER", rworker, r->server);
#endif
                            }
                            if (rworker && PROXY_WORKER_IS_USABLE(rworker)) {
                                /* The context may not be available */
                                nodeinfo_t *node;
                                if (node_storage->read_node(index, &node) != APR_SUCCESS)
                                    return NULL; /* can't read node */
                                if ((nodecontext = context_host_ok(r, balancer, index, vhost_table, context_table, node_table)) != NULL) {
                                    apr_table_setn(r->subprocess_env, "BALANCER_CONTEXT_ID", apr_psprintf(r->pool, "%d", (*nodecontext).context));
                                    return rworker;
                                } else {
                                    return NULL; /* application has been removed from the node */
                                }
                            }
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
                                        const char **domain,
                                        proxy_vhost_table *vhost_table,
                                        proxy_context_table *context_table,
                                        proxy_node_table *node_table)
{
    proxy_worker *worker = NULL;

#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
#if HAVE_CLUSTER_EX_DEBUG
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                     "find_session_route: sticky %s sticky_path: %s sticky_force: %d", balancer->s->sticky, balancer->s->sticky_path, balancer->s->sticky_force);
#endif
    if (balancer->s->sticky[0] == '\0' || balancer->s->sticky_path == '\0')
        return NULL;
#else
#if HAVE_CLUSTER_EX_DEBUG
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                     "find_session_route: sticky %s sticky_force: %d", balancer->sticky, balancer->sticky_force);
#endif
    if (!balancer->sticky)
        return NULL;
    if (! (balancer->sticky_force & STSESSION))
        return NULL;
#endif

    /* We already should have the route in the notes for the trans() */
    *route = apr_table_get(r->notes, "session-route");
    if (*route && (**route)) {
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                     "cluster: Using route %s", *route);
    } else {
#if HAVE_CLUSTER_EX_DEBUG
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                     "cluster:No route found");
#endif
        return NULL;
    }

    *sticky_used = apr_table_get(r->notes, "session-sticky");

    if (domain)
        *domain = apr_table_get(r->notes, "CLUSTER_DOMAIN");

    /* We have a route in path or in cookie
     * Find the worker that has this route defined.
     */
    worker = find_route_worker(r, balancer, *route, vhost_table, context_table, node_table);
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
                                      request_rec *r, const char *domain, int failoverdomain,
                                      proxy_vhost_table *vhost_table,
                                      proxy_context_table *context_table,
                                      proxy_node_table *node_table,
                                      int recurse)
{
    proxy_worker *candidate = NULL;
    apr_status_t rv;

    if ((rv = PROXY_THREAD_LOCK(balancer)) != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, rv, r->server,
        "proxy: CLUSTER: (%s). Lock failed for find_best_worker()",
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
         balancer->s->name
#else
         balancer->name
#endif
         );
        return NULL;
    }

    /* XXX: candidate = (*balancer->lbmethod->finder)(balancer, r); */
    candidate = internal_find_best_byrequests(balancer, conf, r, domain, failoverdomain,
    		vhost_table, context_table, node_table);

    if ((rv = PROXY_THREAD_UNLOCK(balancer)) != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, rv, r->server,
        "proxy: CLUSTER: (%s). Unlock failed for find_best_worker()",
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
         balancer->s->name
#else
         balancer->name
#endif
         );
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
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
        if (balancer->s->timeout && recurse) {
#else
        if (balancer->timeout && recurse) {
#endif
            /* XXX: This can perhaps be build using some
             * smarter mechanism, like tread_cond.
             * But since the statuses can came from
             * different childs, use the provided algo.
             */
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
            apr_interval_time_t timeout = balancer->s->timeout;
#else
            apr_interval_time_t timeout = balancer->timeout;
#endif
            apr_interval_time_t step, tval = 0;

            step = timeout / 100;
            while (tval < timeout) {
                apr_sleep(step);
                /* Try again */
                if ((candidate = find_best_worker(balancer, conf, r,
                		domain, failoverdomain, vhost_table, context_table, node_table, 0)))
                    break;
                tval += step;
            }
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

#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
    *url = apr_pstrcat(r->pool, worker->s->name, path, NULL);
#else
    *url = apr_pstrcat(r->pool, worker->name, path, NULL);
#endif

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
                while (*path !='&' && *path !='\0')
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
 * Note: it need to lock the whole context table
 */
static void upd_context_count(const char *id, int val, server_rec *s)
{
    int ident = atoi(id);
    contextinfo_t *context;
    context_storage->lock_contexts();
    if (context_storage->read_context(ident, &context) == APR_SUCCESS) {
        context->nbrequests = context->nbrequests + val;
    }
    context_storage->unlock_contexts();
}

static apr_status_t decrement_busy_count(void *worker_)
{
    proxy_worker *worker = worker_;
    
    if (worker->s->busy) {
        worker->s->busy--;
    }

    return APR_SUCCESS;
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
    int remove_sessionid = 0;

    proxy_vhost_table *vhost_table = read_vhost_table(r);
    proxy_context_table *context_table = read_context_table(r);
    proxy_node_table *node_table = read_node_table(r);

    *worker = NULL;
#if HAVE_CLUSTER_EX_DEBUG
     if (*balancer) {
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                     "proxy_cluster_pre_request: url %s balancer %s", *url,
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
                     (*balancer)->s->name
#else
                     (*balancer)->name
#endif
                     );
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
            int i;
            int sizew = (*balancer)->workers->elt_size;
            char *ptr = (*balancer)->workers->elts;
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
            int def = ap_proxy_hashfunc(worker_name, PROXY_HASHFUNC_DEFAULT);
            int fnv = ap_proxy_hashfunc(worker_name, PROXY_HASHFUNC_FNV);
#endif
#if HAVE_CLUSTER_EX_DEBUG
            ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                         "proxy_cluster_pre_request: worker %s", worker_name);
#endif
            /* Ajust the context counter here */
            context_id = apr_table_get(r->subprocess_env, "BALANCER_CONTEXT_ID");
            if (context_id && *context_id) {
               upd_context_count(context_id, -1, r->server);
            }
            apr_thread_mutex_lock(lock);
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
            for (i = 0; i < (*balancer)->workers->nelts; i++, ptr=ptr+sizew) {
                proxy_worker **run = (proxy_worker **) ptr;
                if ((*run)->hash.def == def && (*run)->hash.fnv == fnv) {
                    helper = (proxy_cluster_helper *) (*run)->context;
                    if (helper->count_active>0)
                        helper->count_active--;
                    break;
                }
            }
#else
            for (i = 0; i < (*balancer)->workers->nelts; i++, ptr=ptr+sizew) {
                proxy_worker *run = (proxy_worker *) ptr;
                if (run->name && strcmp(worker_name, run->name) == 0) {
                    helper = (proxy_cluster_helper *) (run)->opaque;
                    if (helper->count_active>0)
                        helper->count_active--;
                    break;
                }
            }
#endif
            apr_thread_mutex_unlock(lock);
        }
    }

    /* TODO if we don't have a balancer but a route we should use it directly */
    apr_thread_mutex_lock(lock);
    if (!*balancer &&
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
        !(*balancer = ap_proxy_get_balancer(r->pool, conf, *url, 0))) {
#else
        !(*balancer = ap_proxy_get_balancer(r->pool, conf, *url))) {
#endif
        apr_thread_mutex_unlock(lock);
        /* May be the node has not been created yet */
        update_workers_node(conf, r->pool, r->server, 1);
        apr_thread_mutex_lock(lock);
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
        if (!(*balancer = ap_proxy_get_balancer(r->pool, conf, *url, 0))) {
#else
        if (!(*balancer = ap_proxy_get_balancer(r->pool, conf, *url))) {
#endif
            apr_thread_mutex_unlock(lock);
            ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                         "proxy: CLUSTER no balancer for %s", *url);
            return DECLINED;
        }
    }

    /* Step 2: find the session route */

    runtime = find_session_route(*balancer, r, &route, &sticky, url, &domain,
    		vhost_table, context_table, node_table);
    apr_thread_mutex_unlock(lock);

    /* Lock the LoadBalancer
     * XXX: perhaps we need the process lock here
     */
    if ((rv = PROXY_THREAD_LOCK(*balancer)) != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, rv, r->server,
                     "proxy: CLUSTER: (%s). Lock failed for pre_request",
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
                     (*balancer)->s->name
#else
                     (*balancer)->name
#endif
                     );
        return DECLINED;
    }
    if (runtime) {
        runtime->s->elected++;
        *worker = runtime;
    }
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
    else if (route && ((*balancer)->s->sticky_force)) {
#else
    else if (route && ((*balancer)->sticky_force & STSESSFOR)) {
#endif
        if (domain == NULL) {
            /*
             * We have a route provided that doesn't match the
             * balancer name. See if the provider route is the
             * member of the same balancer in which case return 503
             */
            ap_log_error(APLOG_MARK, APLOG_ERR, 0, r->server,
                         "proxy: CLUSTER: (%s). All workers are in error state for route (%s)",
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
                         (*balancer)->s->name,
#else
                         (*balancer)->name,
#endif
                         route);
            if ((rv = PROXY_THREAD_UNLOCK(*balancer)) != APR_SUCCESS) {
                ap_log_error(APLOG_MARK, APLOG_ERR, rv, r->server,
                             "proxy: CLUSTER: (%s). Unlock failed for pre_request",
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
                             (*balancer)->s->name
#else
                             (*balancer)->name
#endif
                             );
            }
            return HTTP_SERVICE_UNAVAILABLE;
        } else {
            /* We try to to failover using another node in the domain */
#if HAVE_CLUSTER_EX_DEBUG
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                     "mod_proxy_cluster: failover in domain");
#endif
            failoverdomain = 1;
        }
    }

    if ((rv = PROXY_THREAD_UNLOCK(*balancer)) != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, rv, r->server,
                     "proxy: CLUSTER: (%s). Unlock failed for pre_request",
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
                     (*balancer)->s->name
#else
                     (*balancer)->name
#endif
                     );
    }
    if (!*worker) {
        /* 
         * We have to failover (in domain only may be) or we don't use sticky sessions
         */
        runtime = find_best_worker(*balancer, conf, r, domain, failoverdomain,
        		vhost_table, context_table, node_table, 1);
        if (!runtime) {
            ap_log_error(APLOG_MARK, APLOG_ERR, 0, r->server,
                         "proxy: CLUSTER: (%s). All workers are in error state",
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
                         (*balancer)->s->name
#else
                         (*balancer)->name
#endif
                         );

            return HTTP_SERVICE_UNAVAILABLE;
        }
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
        if ((*balancer)->s->sticky && runtime) {
#else
        if ((*balancer)->sticky && runtime) {
#endif
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
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
        /* we should read Remove sessionid from the proxy_balancer_table */
        if (route) {
            const proxy_balancer_table *balancer_table =  (proxy_balancer_table *) apr_table_get(r->notes, "balancer-table");
            if (balancer_table) {
                int i;
                for (i = 0; i < balancer_table->sizebalancer; i++) {
                    if (strcmp(balancer_table->balancer_info[i].balancer, &(*balancer)->s->name[11])) {
                        if ( balancer_table->balancer_info[i].StickySessionRemove)
                            remove_sessionid = 1;
                        break;
                    }
                } 
            }
        }
#else
        /* Remove sessionid fits in sticky_force */
        if (route && ((*balancer)->sticky_force & STSESSREM)) {
            remove_sessionid = 1;
        }
#endif
        if (remove_sessionid) {
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
    apr_pool_cleanup_register(r->pool, *worker, decrement_busy_count,
                              apr_pool_cleanup_null);

    /* Also mark the context here note that find_best_worker set BALANCER_CONTEXT_ID */
    context_id = apr_table_get(r->subprocess_env, "BALANCER_CONTEXT_ID");
    if (context_id && *context_id) {
       upd_context_count(context_id, 1, r->server);
    }

    /* Mark the worker used for the cleanup logic */
    apr_thread_mutex_lock(lock);
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
    helper = (proxy_cluster_helper *) (*worker)->context;
#else
    helper = (proxy_cluster_helper *) (*worker)->opaque;
#endif
    helper->count_active++;
    apr_thread_mutex_unlock(lock);

    /*
     * get_route_balancer already fills all of the notes and some subprocess_env
     * but not all.
     * Note that BALANCER_WORKER_NAME would have changed in case of failover.
     */
    /* Add balancer/worker info to env. */
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
    apr_table_setn(r->subprocess_env, "BALANCER_NAME", (*balancer)->s->name);
    apr_table_setn(r->subprocess_env, "BALANCER_WORKER_NAME", (*worker)->s->name);
#else
    apr_table_setn(r->subprocess_env, "BALANCER_NAME", (*balancer)->name);
    apr_table_setn(r->subprocess_env, "BALANCER_WORKER_NAME", (*worker)->name);
#endif
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
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
                 (*balancer)->s->name, (*worker)->s->name,
#else
                 (*balancer)->name, (*worker)->name,
#endif
                 *url);
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
#if AP_MODULE_MAGIC_AT_LEAST(20051115,25)
    apr_status_t rv;
#endif

    /* Ajust the context counter here too */
    if (context_id && *context_id) {
       upd_context_count(context_id, -1, r->server);
    }

    /* mark the worker as not in use */
    apr_thread_mutex_lock(lock);
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
    helper = (proxy_cluster_helper *) worker->context;
#else
    helper = (proxy_cluster_helper *) worker->opaque;
#endif
    helper->count_active--;

    apr_thread_mutex_unlock(lock);

#if HAVE_CLUSTER_EX_DEBUG
    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                 "proxy_cluster_post_request for (%s) %s",
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
                  balancer->s->name, balancer->s->sticky
#else
                  balancer->name, balancer->sticky
#endif
                  );
#endif

    if (sessionid_storage) {

        /* Add information about sessions corresponding to a node */
        sticky = apr_table_get(r->notes, "session-sticky");
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
        if (sticky == NULL && balancer->s->sticky[0] != '\0') {
            sticky = apr_pstrdup(r->pool, balancer->s->sticky);
        }
#else
        if (sticky == NULL && balancer->sticky) {
            char *path, *stick;
            stick = apr_pstrdup(r->pool, balancer->sticky);
            if ((path = strchr(stick, '|'))) {
                *path++ = '\0';
            }
            sticky = (const char *) stick;
        }
#endif
        if (sticky != NULL) {
            cookie = get_cookie_param(r, sticky, 0);
            sessionid =  apr_table_get(r->notes, "session-id");
            route =  apr_table_get(r->notes, "session-route");
            if (cookie) {
                if (sessionid && strcmp(cookie, sessionid)) {
                    /* The cookie has changed, remove the old one and store the next one */
                    sessionidinfo_t ou;
#if HAVE_CLUSTER_EX_DEBUG
                    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                                 "proxy_cluster_post_request sessionid changed (%s to %s)", sessionid, cookie);
#endif
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
    }

    /*  20051115.25 (2.2.17) Add errstatuses member to proxy_balancer */
#if AP_MODULE_MAGIC_AT_LEAST(20051115,25)
    if ((rv = PROXY_THREAD_LOCK(balancer)) != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, rv, r->server,
            "proxy: BALANCER: (%s). Lock failed for post_request",
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
            balancer->s->name
#else
            balancer->name
#endif
            );
        return HTTP_INTERNAL_SERVER_ERROR;
    }

    if (!apr_is_empty_array(balancer->errstatuses)) {
        int i;
        for (i = 0; i < balancer->errstatuses->nelts; i++) {
            int val = ((int *)balancer->errstatuses->elts)[i];
            if (r->status == val) {
                ap_log_rerror(APLOG_MARK, APLOG_ERR, 0, r,
                              "%s: Forcing worker (%s) into error state "
                              "due to status code %d matching 'failonstatus' "
                              "balancer parameter",
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
                              balancer->s->name,
                              worker->s->name,
#else
                              balancer->name,
                              worker->name,
#endif
                              val);
                worker->s->status |= PROXY_WORKER_IN_ERROR;
                worker->s->error_time = apr_time_now();
                break;
            }
        }
    }

    if ((rv = PROXY_THREAD_UNLOCK(balancer)) != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_ERR, rv, r->server,
            "proxy: BALANCER: (%s). Unlock failed for post_request",
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
            balancer->s->name
#else
            balancer->name
#endif
            );
    }
#endif

    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                 "proxy_cluster_post_request %d for (%s)", r->status,
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
                 balancer->s->name
#else
                 balancer->name
#endif
                 );

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
    static const char * const aszPre[]={ "mod_manager.c", "mod_rewrite.c", NULL };
    static const char * const aszSucc[]={ "mod_proxy.c", NULL };

    ap_hook_post_config(proxy_cluster_post_config, NULL, NULL, APR_HOOK_MIDDLE);

    /* create the "maintenance" thread */
    ap_hook_child_init(proxy_cluster_child_init, NULL, NULL, APR_HOOK_LAST);

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
        return "CreateBalancers must be one of: 0, 1 or 2";
    } else {
        creat_bal = val;
    }
    return NULL;
}

static const char*cmd_proxy_cluster_use_alias(cmd_parms *cmd, void *dummy, const char *arg)
{

   /* Cannot use AP_INIT_FLAG, to keep compatibility with versions <= 1.3.0.Final which accepted
      only values 1 and 0. (see MODCLUSTER-403) */
   if (strcasecmp(arg, "Off") == 0 || strcasecmp(arg, "0") == 0) {
       use_alias = 0;
   } else if (strcasecmp(arg, "On") == 0 || strcasecmp(arg, "1") == 0) {
       use_alias = 1;
   } else {
       return "UseAlias must be either On or Off";
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

static const char*cmd_proxy_cluster_wait_for_remove(cmd_parms *cmd, void *dummy, const char *arg)
{
    int val = atoi(arg);
    if (val<10) {
        return "WaitForRemove must be greater than 10";
    } else {
        wait_for_remove = apr_time_from_sec(val);
    }
    return NULL;
}

static const char*cmd_proxy_cluster_enable_options(cmd_parms *cmd, void *dummy, const char *args)
{
    char *val = ap_getword_conf(cmd->pool, &args);

    if (strcasecmp(val, "Off") == 0 || strcasecmp(val, "0") == 0) {
        /* Disables OPTIONS, overrides the default */
        enable_options = 0;
    } else if (strcmp(val, "") == 0 || strcasecmp(val, "On") == 0 || strcasecmp(val, "1") == 0) {
        /* No param or explicitly set default */
        enable_options = -1;
    } else {
        return "EnableOptions must be either without value or On or Off";
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
        "UseAlias - Check that the Alias corresponds to the ServerName Off: Don't check (ignore Aliases), On: Check aliases (Default: Off)"
    ),
    AP_INIT_TAKE1(
        "LBstatusRecalTime",
        cmd_proxy_cluster_lbstatus_recalc_time,
        NULL,
        OR_ALL,
        "LBstatusRecalTime - Time interval in seconds for loadbalancing logic to recalculate the status of a node: (Default: 5 seconds)"
    ),
    AP_INIT_TAKE1(
        "WaitBeforeRemove",
        cmd_proxy_cluster_wait_for_remove,
        NULL,
        OR_ALL,
        "WaitBeforeRemove - Time in seconds before a node removed is forgotten by httpd: (Default: 10 seconds)"
    ),
    /* This is not the ideal type, but it either takes no parameters (for backwards compatibility) or 1 flag argument. */
    AP_INIT_RAW_ARGS(
        "EnableOptions",
         cmd_proxy_cluster_enable_options,
         NULL,
         OR_ALL,
         "EnableOptions - Use OPTIONS with HTTP/HTTPS for CPING/CPONG. On: Use OPTIONS, Off: Do not use OPTIONS (Default: On)"
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
