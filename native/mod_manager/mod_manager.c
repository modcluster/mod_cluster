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
#include "apr_lib.h"
#include "apr_uuid.h"

#define CORE_PRIVATE
#include "httpd.h"
#include "http_config.h"
#include "http_log.h"
#include "http_main.h"
#include "http_request.h"
#include "http_protocol.h"
#include "http_core.h"
#include "scoreboard.h"
#include "mod_proxy.h"
#include "ap_mpm.h"

#include "mod_proxy_cluster.h"

#include "slotmem.h"

#include "node.h"
#include "host.h"
#include "context.h"
#include "balancer.h"
#include "sessionid.h"

#define DEFMAXCONTEXT   100
#define DEFMAXNODE      20
#define DEFMAXHOST      20
#define DEFMAXSESSIONID 512
#define MAXMESSSIZE     1024

/* Error messages */
#define TYPESYNTAX 1
#define SMESPAR "SYNTAX: Can't parse message"
#define SBALBIG "SYNTAX: Balancer field too big"
#define SBAFBIG "SYNTAX: A field is too big"
#define SROUBIG "SYNTAX: JVMRoute field too big"
#define SROUBAD "SYNTAX: JVMRoute can't be empty"
#define SDOMBIG "SYNTAX: Domain field too big"
#define SHOSBIG "SYNTAX: Host field too big"
#define SPORBIG "SYNTAX: Port field too big"    
#define STYPBIG "SYNTAX: Type field too big"
#define SALIBAD "SYNTAX: Alias without Context"
#define SCONBAD "SYNTAX: Context without Alias"
#define SBADFLD "SYNTAX: Invalid field \"%s\" in message"
#define SCMDUNS "SYNTAX: Command is not supported"
#define SMULALB "SYNTAX: Only one Alias in APP command"
#define SMULCTB "SYNTAX: Only one Context in APP command"
#define SREADER "SYNTAX: %s can't read POST data"

#define TYPEMEM 2
#define MNODEUI "MEM: Can't update or insert node"
#define MNODERM "MEM: Old node still exist"
#define MBALAUI "MEM: Can't update or insert balancer"
#define MNODERD "MEM: Can't read node"
#define MHOSTRD "MEM: Can't read host alias"

/* Protocol version supported */
#define VERSION_PROTOCOL "0.0.0"

/* shared memory */
mem_t *contextstatsmem = NULL;
mem_t *nodestatsmem = NULL;
mem_t *hoststatsmem = NULL;
mem_t *balancerstatsmem = NULL;
mem_t *sessionidstatsmem = NULL;

slotmem_storage_method *storage = NULL;
balancer_method *balancerhandler = NULL;

module AP_MODULE_DECLARE_DATA manager_module;

static char balancer_nonce[APR_UUID_FORMATTED_LENGTH + 1];

typedef struct mod_manager_config
{
    /* base name for the shared memory */
    char *basefilename;
    /* max number of context supported */
    int maxcontext;
    /* max munber of node supported */
    int maxnode;
    /* max munber of host supported */
    int maxhost;
    /* max munber of session supported */
    int maxsessionid;

    /* last time the node update logic was called */
    apr_time_t last_updated;

    /* Should be the slotmem persisted (1) or not (0) */
    int persistent;

} mod_manager_config;

/*
 * routines for the node_storage_method
 */
static apr_status_t loc_read_node(int ids, nodeinfo_t **node)
{
    return (get_node(nodestatsmem, node, ids));
}
static int loc_get_ids_used_node(int *ids)
{
    return(get_ids_used_node(nodestatsmem, ids)); 
}
static int loc_get_max_size_node()
{
    return(get_max_size_node(nodestatsmem));
}
static apr_status_t loc_remove_node(nodeinfo_t *node)
{
    return (remove_node(nodestatsmem, node));
}
static apr_status_t loc_find_node(nodeinfo_t **node, const char *route)
{
    return (find_node(nodestatsmem, node, route));
}
static apr_status_t loc_unlock_node()
{
    return (unlock_node(nodestatsmem));
}
static apr_status_t loc_lock_node()
{
    return (lock_node(nodestatsmem));
}

/* Check is the nodes (in shared memory) were modified since last
 * call to worker_nodes_are_updated().
 * return codes:
 *   0 : No update of the nodes since last time.
 *   x : Last time we changed something in the process.
 */
static apr_time_t loc_worker_nodes_need_update(void *data, apr_pool_t *pool)
{
    int size, i;
    int *id;
    server_rec *s = (server_rec *) data;
    apr_time_t last = 0;
    mod_manager_config *mconf = ap_get_module_config(s->module_config, &manager_module);

    size = get_max_size_node(nodestatsmem);
    id = apr_palloc(pool, sizeof(int) * size);
    size = get_ids_used_node(nodestatsmem, id);
    for (i=0; i<size; i++) {
        nodeinfo_t *ou;
        if (get_node(nodestatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        if (ou->updatetime > last)
            last = ou->updatetime;
    }
    if (last >= mconf->last_updated) {
        if (mconf->last_updated == 0)
            return(1); /* First time */
        return(mconf->last_updated);
    }

    return (0);
}
/* Store the last update time in the proccess config */
static int loc_worker_nodes_are_updated(void *data)
{
    server_rec *s = (server_rec *) data;
    mod_manager_config *mconf = ap_get_module_config(s->module_config, &manager_module);
    mconf->last_updated = apr_time_now();
    return (0);
}
static const struct node_storage_method node_storage =
{
    loc_read_node,
    loc_get_ids_used_node,
    loc_get_max_size_node,
    loc_worker_nodes_need_update,
    loc_worker_nodes_are_updated,
    loc_remove_node,
    loc_find_node,
    loc_lock_node,
    loc_unlock_node
};

/*
 * routines for the context_storage_method
 */
static apr_status_t loc_read_context(int ids, contextinfo_t **context)
{
    return (get_context(contextstatsmem, context, ids));
}
static int loc_get_ids_used_context(int *ids)
{
    return(get_ids_used_context(contextstatsmem, ids)); 
}
static int loc_get_max_size_context()
{
    return(get_max_size_context(contextstatsmem));
}
static const struct context_storage_method context_storage =
{
    loc_read_context,
    loc_get_ids_used_context,
    loc_get_max_size_context
};

/*
 * routines for the host_storage_method
 */
static apr_status_t loc_read_host(int ids, hostinfo_t **host)
{
    return (get_host(hoststatsmem, host, ids));
}
static int loc_get_ids_used_host(int *ids)
{
    return(get_ids_used_host(hoststatsmem, ids)); 
}
static int loc_get_max_size_host()
{
    return(get_max_size_host(hoststatsmem));
}
static const struct host_storage_method host_storage =
{
    loc_read_host,
    loc_get_ids_used_host,
    loc_get_max_size_host
};

/*
 * routines for the balancer_storage_method
 */
static apr_status_t loc_read_balancer(int ids, balancerinfo_t **balancer)
{
    return (get_balancer(balancerstatsmem, balancer, ids));
}
static int loc_get_ids_used_balancer(int *ids)
{
    return(get_ids_used_balancer(balancerstatsmem, ids)); 
}
static int loc_get_max_size_balancer()
{
    return(get_max_size_balancer(balancerstatsmem));
}
static const struct balancer_storage_method balancer_storage =
{
    loc_read_balancer,
    loc_get_ids_used_balancer,
    loc_get_max_size_balancer
};
/*
 * routines for the sessionid_storage_method
 */
static apr_status_t loc_read_sessionid(int ids, sessionidinfo_t **sessionid)
{
    return (get_sessionid(sessionidstatsmem, sessionid, ids));
}
static int loc_get_ids_used_sessionid(int *ids)
{
    return(get_ids_used_sessionid(sessionidstatsmem, ids)); 
}
static int loc_get_max_size_sessionid()
{
    return(get_max_size_sessionid(sessionidstatsmem));
}
static apr_status_t loc_remove_sessionid(sessionidinfo_t *sessionid)
{
    return (remove_sessionid(sessionidstatsmem, sessionid));
}
static apr_status_t loc_insert_update_sessionid(sessionidinfo_t *sessionid)
{
    return (insert_update_sessionid(sessionidstatsmem, sessionid));
}
static const struct  sessionid_storage_method sessionid_storage =
{
    loc_read_sessionid,
    loc_get_ids_used_sessionid,
    loc_get_max_size_sessionid,
    loc_remove_sessionid,
    loc_insert_update_sessionid
};

/* helper for the handling of the Alias: host1,... Context: context1,... */
struct cluster_host {
    char *host;
    char *context;
    struct cluster_host *next;
};

/*
 * call after parser the configuration.
 * create the shared memory.
 */
static int manager_init(apr_pool_t *p, apr_pool_t *plog,
                          apr_pool_t *ptemp, server_rec *s)
{
    char *node;
    char *context;
    char *host;
    char *balancer;
    char *sessionid;
    void *data;
    const char *userdata_key = "mod_manager_init";
    apr_uuid_t uuid;
    mod_manager_config *mconf = ap_get_module_config(s->module_config, &manager_module);
    apr_pool_userdata_get(&data, userdata_key, s->process->pool);
    if (!data) {
        /* first call do nothing */
        apr_pool_userdata_set((const void *)1, userdata_key, apr_pool_cleanup_null, s->process->pool);
        return OK;
    }

    if (mconf->basefilename) {
        node = apr_pstrcat(ptemp, mconf->basefilename, ".node", NULL);
        context = apr_pstrcat(ptemp, mconf->basefilename, ".context", NULL);
        host = apr_pstrcat(ptemp, mconf->basefilename, ".host", NULL);
        balancer = apr_pstrcat(ptemp, mconf->basefilename, ".balancer", NULL);
        sessionid = apr_pstrcat(ptemp, mconf->basefilename, ".sessionid", NULL);
    } else {
        node = ap_server_root_relative(ptemp, "logs/manager.node");
        context = ap_server_root_relative(ptemp, "logs/manager.context");
        host = ap_server_root_relative(ptemp, "logs/manager.host");
        balancer = ap_server_root_relative(ptemp, "logs/manager.balancer");
        sessionid = ap_server_root_relative(ptemp, "logs/manager.sessionid");
    }

    /* Get a provider to handle the shared memory */

    storage = ap_lookup_provider(SLOTMEM_STORAGE, "shared", "0");
    if (storage == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "ap_lookup_provider %s failed", SLOTMEM_STORAGE);
        return  !OK;
    }
    nodestatsmem = create_mem_node(node, &mconf->maxnode, mconf->persistent, p, storage);
    if (nodestatsmem == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "create_mem_node %s failed", node);
        return  !OK;
    }

    contextstatsmem = create_mem_context(context, &mconf->maxcontext, mconf->persistent, p, storage);
    if (contextstatsmem == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "create_mem_context failed");
        return  !OK;
    }

    hoststatsmem = create_mem_host(host, &mconf->maxhost, mconf->persistent, p, storage);
    if (hoststatsmem == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "create_mem_host failed");
        return  !OK;
    }

    balancerstatsmem = create_mem_balancer(balancer, &mconf->maxhost, mconf->persistent, p, storage);
    if (balancerstatsmem == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "create_mem_balancer failed");
        return  !OK;
    }

    sessionidstatsmem = create_mem_sessionid(sessionid, &mconf->maxhost, mconf->persistent, p, storage);
    if (sessionidstatsmem == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "create_mem_sessionid failed");
        return  !OK;
    }

    /* Get a provider to ping/pong logics */

    balancerhandler = ap_lookup_provider("proxy_cluster", "balancer", "0");
    if (balancerhandler == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_WARNING, 0, s, "can't find a ping/pong logic");
    }

    /*
     * Retrieve a UUID and store the nonce.
     */
    apr_uuid_get(&uuid);
    apr_uuid_format(balancer_nonce, &uuid);

    return OK;
}
static char **process_buff(request_rec *r, char *buff)
{
    int i = 0;
    char *s = buff;
    char **ptr = NULL;
    for (; *s != '\0'; s++) {
        if (*s == '&' || *s == '=') {
            i++;
        }
    }
    ptr = apr_palloc(r->pool, sizeof(char *) * (i + 2));
    if (ptr == NULL)
        return NULL;

    s = buff;
    ptr[0] = s;
    ptr[i+1] = NULL;
    i = 1;
    for (; *s != '\0'; s++) {
        if (*s == '&' || *s == '=') {
            *s = '\0';
            ptr[i] = s + 1;
            i++;
        }
    }
    return ptr;
}
/*
 * Insert the hosts from Alias information
 */
static int  insert_update_hosts(mem_t *mem, char *str, int node, int vhost)
{
    char *ptr = str;
    char *previous = str;
    int ret = 0;
    hostinfo_t info;
    char empty[1] = {'\0'};

    info.node = node;
    info.vhost = vhost;
    if (ptr == NULL) {
        ptr = empty;
        previous = ptr;
    }
    while (*ptr) {
        if (*ptr == ',') {
            *ptr = '\0';
            strncpy(info.host, previous, sizeof(info.host));
            insert_update_host(mem, &info); 
            if (!ret)
                ret = info.id;
            previous = ptr + 1;
        }
        ptr ++;
    }
    strncpy(info.host, previous, sizeof(info.host));
    insert_update_host(mem, &info); 
    return ret;
}
/*
 * Insert the context from Context information
 * Note that if status is REMOVE remove_context will be called.
 */
static int  insert_update_contexts(mem_t *mem, char *str, int node, int vhost, int status)
{
    char *ptr = str;
    char *previous = str;
    int ret = 0;
    contextinfo_t info;
    char empty[2] = {'/','\0'};

    info.node = node;
    info.vhost = vhost;
    info.status = status;
    if (ptr == NULL) {
        ptr = empty;
        previous = ptr;
    }
    while (*ptr) {
        if (*ptr == ',') {
            *ptr = '\0';
            info.id = 0;
            strncpy(info.context, previous, sizeof(info.context));
            if (status != REMOVE)
                insert_update_context(mem, &info); 
            else
                remove_context(mem, &info);

            if (!ret)
                ret = info.id;
            previous = ptr + 1;
        }
        ptr ++;
    }
    info.id = 0;
    strncpy(info.context, previous, sizeof(info.context));
    if (status != REMOVE)
        insert_update_context(mem, &info); 
    else
        remove_context(mem, &info);
    return ret;
}
/*
 * Check that the node could be handle as is there were the same.
 */
static int  is_same_node(nodeinfo_t *nodeinfo, nodeinfo_t *node) {
    if (strcmp(nodeinfo->mess.balancer,node->mess.balancer))
        return 0;
    if (strcmp(nodeinfo->mess.Host, node->mess.Host))
        return 0;
    if (strcmp(nodeinfo->mess.Port,node->mess.Port))
        return 0;
    if (strcmp(nodeinfo->mess.Type, node->mess.Type))
        return 0;
    if (nodeinfo->mess.reversed != node->mess.reversed)
        return 0;

    /* Those means the reslist has to be changed */
    if (nodeinfo->mess.smax !=  node->mess.smax)
        return 0;
    if (nodeinfo->mess.ttl != node->mess.ttl)
        return 0;

    /* All other fields can be modified without causing problems */
    return -1;
}
/*
 * Remove host and context belonging to the node
 */
static void remove_host_context(request_rec *r, int node)
{
    /* for read the hosts */
    int i,j;
    int size = get_max_size_host(hoststatsmem);
    int *id = apr_palloc(r->pool, sizeof(int) * size);
    int sizecontext = get_max_size_context(contextstatsmem);
    int *idcontext = apr_palloc(r->pool, sizeof(int) * sizecontext);


    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                "remove_host_context processing node: %d", node);

    size = get_ids_used_host(hoststatsmem, id);
    for (i=0; i<size; i++) {
        hostinfo_t *ou;
        int sizecontext;
        int *idcontext;

        if (get_host(hoststatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        if (ou->node == node)
            remove_host(hoststatsmem, ou);
    }

    sizecontext = get_ids_used_context(contextstatsmem, idcontext);
    for (i=0; i<sizecontext; i++) {
        contextinfo_t *context;
        if (get_context(contextstatsmem, &context, idcontext[i]) != APR_SUCCESS)
            continue;
        if (context->node == node)
            remove_context(contextstatsmem, context);
    }
}

/*
 * Process a CONFIG message
 * Balancer: <Balancer name>
 * <balancer configuration>
 * StickySession	StickySessionCookie	StickySessionPath	StickySessionRemove
 * StickySessionForce	Timeout	Maxattempts
 * JvmRoute?: <JvmRoute>
 * Domain: <Domain>
 * <Host: <Node IP>
 * Port: <Connector Port>
 * Type: <Type of the connector>
 * Reserved: <Use connection pool initiated by Tomcat *.>
 * <node conf>
 * flushpackets	flushwait	ping	smax	ttl
 * Virtual hosts in JBossAS
 * Alias: <vhost list>
 * Context corresponding to the applications.
 * Context: <context list>
 */
static char * process_config(request_rec *r, char *buff, int *errtype)
{
    /* Process the node/balancer description */
    nodeinfo_t nodeinfo;
    nodeinfo_t *node;
    balancerinfo_t balancerinfo;
    int mpm_threads;
    
    struct cluster_host *vhost; 
    struct cluster_host *phost; 

    int i = 0;
    int id;
    int vid = 1; /* zero and "" is empty */

    char **ptr = process_buff(r, buff);
    if (ptr == NULL) {
        *errtype = TYPESYNTAX;
        return SMESPAR;
    }
    vhost = apr_palloc(r->pool, sizeof(struct cluster_host));

    /* Map nothing by default */
    vhost->host = NULL;
    vhost->context = NULL;
    vhost->next = NULL;
    phost = vhost;

    /* Fill default nodes values */
    memset(&nodeinfo.mess, '\0', sizeof(nodeinfo.mess));
    strcpy(nodeinfo.mess.balancer, "mycluster");
    strcpy(nodeinfo.mess.Host, "localhost");
    strcpy(nodeinfo.mess.Port, "8009");
    strcpy(nodeinfo.mess.Type, "ajp");
    nodeinfo.mess.reversed = 0;
    nodeinfo.mess.remove = 0; /* not marked as removed */
    nodeinfo.mess.flushpackets = flush_off; /* FLUSH_OFF; See enum flush_packets in proxy.h flush_off */
    nodeinfo.mess.flushwait = PROXY_FLUSH_WAIT;
    nodeinfo.mess.ping = apr_time_from_sec(10);
    ap_mpm_query(AP_MPMQ_MAX_THREADS, &mpm_threads);
    nodeinfo.mess.smax = mpm_threads + 1;
    nodeinfo.mess.ttl = apr_time_from_sec(60);
    nodeinfo.mess.timeout = 0;
    nodeinfo.mess.id = 0;
    nodeinfo.mess.lastcleantry = 0;

    /* Fill default balancer values */
    memset(&balancerinfo, '\0', sizeof(balancerinfo));
    strcpy(balancerinfo.balancer, "mycluster");
    balancerinfo.StickySession = 1;
    balancerinfo.StickySessionForce = 1;
    strcpy(balancerinfo.StickySessionCookie, "JSESSIONID");
    strcpy(balancerinfo.StickySessionPath, "jsessionid");
    balancerinfo.Maxattempts = 1;
    balancerinfo.Timeout = 0;

    while (ptr[i]) {
        /* XXX: balancer part */
        if (strcasecmp(ptr[i], "Balancer") == 0) {
            if (strlen(ptr[i+1])>=sizeof(nodeinfo.mess.balancer)) {
                *errtype = TYPESYNTAX;
                return SBALBIG;
            }
            strcpy(nodeinfo.mess.balancer, ptr[i+1]);
            strcpy(balancerinfo.balancer, ptr[i+1]);
        }
        if (strcasecmp(ptr[i], "StickySession") == 0) {
            if (strcasecmp(ptr[i+1], "no") == 0)
                balancerinfo.StickySession = 0;
        }
        if (strcasecmp(ptr[i], "StickySessionCookie") == 0) {
            if (strlen(ptr[i+1])>=sizeof(balancerinfo.StickySessionCookie)) {
                *errtype = TYPESYNTAX;
                return SBAFBIG;
            }
            strcpy(balancerinfo.StickySessionCookie, ptr[i+1]);
        }
        if (strcasecmp(ptr[i], "StickySessionPath") == 0) {
            if (strlen(ptr[i+1])>=sizeof(balancerinfo.StickySessionPath)) {
                *errtype = TYPESYNTAX;
                return SBAFBIG;
            }
            strcpy(balancerinfo.StickySessionPath, ptr[i+1]);
        }
        if (strcasecmp(ptr[i], "StickySessionRemove") == 0) {
            if (strcasecmp(ptr[i+1], "yes") == 0)
                balancerinfo.StickySessionRemove = 1;
        }
        if (strcasecmp(ptr[i], "StickySessionForce") == 0) {
            if (strcasecmp(ptr[i+1], "no") == 0)
                balancerinfo.StickySessionForce = 0;
        }
        if (strcasecmp(ptr[i], "WaitWorker") == 0) {
            balancerinfo.Timeout = apr_time_from_sec(atoi(ptr[i+1]));
        }
        if (strcasecmp(ptr[i], "Maxattempts") == 0) {
            balancerinfo.Maxattempts = atoi(ptr[i+1]);
        }

        /* XXX: Node part */
        if (strcasecmp(ptr[i], "JVMRoute") == 0) {
            if (strlen(ptr[i+1])>=sizeof(nodeinfo.mess.JVMRoute)) {
                *errtype = TYPESYNTAX;
                return SROUBIG;
            }
            strcpy(nodeinfo.mess.JVMRoute, ptr[i+1]);
        }
        if (strcasecmp(ptr[i], "Domain") == 0) {
            if (strlen(ptr[i+1])>=sizeof(nodeinfo.mess.Domain)) {
                *errtype = TYPESYNTAX;
                return SDOMBIG;
            }
            strcpy(nodeinfo.mess.Domain, ptr[i+1]);
        }
        if (strcasecmp(ptr[i], "Host") == 0) {
            if (strlen(ptr[i+1])>=sizeof(nodeinfo.mess.Host)) {
                *errtype = TYPESYNTAX;
                return SHOSBIG;
            }
            strcpy(nodeinfo.mess.Host, ptr[i+1]);
        }
        if (strcasecmp(ptr[i], "Port") == 0) {
            if (strlen(ptr[i+1])>=sizeof(nodeinfo.mess.Port)) {
                *errtype = TYPESYNTAX;
                return SPORBIG;
            }
            strcpy(nodeinfo.mess.Port, ptr[i+1]);
        }
        if (strcasecmp(ptr[i], "Type") == 0) {
            if (strlen(ptr[i+1])>=sizeof(nodeinfo.mess.Type)) {
                *errtype = TYPESYNTAX;
                return STYPBIG;
            }
            strcpy(nodeinfo.mess.Type, ptr[i+1]);
        }
        if (strcasecmp(ptr[i], "Reversed") == 0) {
            if (strcasecmp(ptr[i+1], "yes") == 0) {
            nodeinfo.mess.reversed = 1;
            }
        }
        if (strcasecmp(ptr[i], "flushpackets") == 0) {
            if (strcasecmp(ptr[i+1], "on") == 0) {
                nodeinfo.mess.flushpackets = flush_on;
            }
            else if (strcasecmp(ptr[i+1], "auto") == 0) {
                nodeinfo.mess.flushpackets = flush_auto;
            }
        }
        if (strcasecmp(ptr[i], "flushwait") == 0) {
            nodeinfo.mess.flushwait = atoi(ptr[i+1]) * 1000;
        }
        if (strcasecmp(ptr[i], "ping") == 0) {
            nodeinfo.mess.ping = apr_time_from_sec(atoi(ptr[i+1]));
        }
        if (strcasecmp(ptr[i], "smax") == 0) {
            nodeinfo.mess.smax = atoi(ptr[i+1]);
        }
        if (strcasecmp(ptr[i], "ttl") == 0) {
            nodeinfo.mess.ttl = apr_time_from_sec(atoi(ptr[i+1]));
        }
        if (strcasecmp(ptr[i], "Timeout") == 0) {
            nodeinfo.mess.timeout = apr_time_from_sec(atoi(ptr[i+1]));
        }

        /* Hosts and contexts (optional paramters) */
        if (strcasecmp(ptr[i], "Alias") == 0) {
            if (phost->host && !phost->context) {
                *errtype = TYPESYNTAX;
                return SALIBAD;
            }
            if (phost->host) {
               phost->next = apr_palloc(r->pool, sizeof(struct cluster_host));
               phost = phost->next;
               phost->next = NULL;
               phost->host = ptr[i+1];
               phost->context = NULL;
            } else {
               phost->host = ptr[i+1];
            }
        }
        if (strcasecmp(ptr[i], "Context") == 0) {
            if (phost->context) {
                *errtype = TYPESYNTAX;
                return SCONBAD;
            }
            phost->context = ptr[i+1];
        }
        i++;
        i++;
    }

    /* Check for JVMRoute */
    if (nodeinfo.mess.JVMRoute[0] == '\0') {
        *errtype = TYPESYNTAX;
        return SROUBAD;
    }

    /* Insert or update balancer description */
    if (insert_update_balancer(balancerstatsmem, &balancerinfo) != APR_SUCCESS) {
        *errtype = TYPEMEM;
        return MBALAUI;
    }

    /* check for removed node */
    node = read_node(nodestatsmem, &nodeinfo);
    if (node != NULL) {
        /* If the node is removed (or kill and restarted) and recreated unchanged that is ok: network problems */
        if (! is_same_node(node, &nodeinfo)) {
            /* Here we can't update it because the old one is still in */
            strcpy(node->mess.JVMRoute, "REMOVED");
            node->mess.remove = 1;
            insert_update_node(nodestatsmem, node, &id);
            remove_host_context(r, node->mess.id);
            *errtype = TYPEMEM;
            return MNODERM;
        }
    }

    /* Insert or update node description */
    if (insert_update_node(nodestatsmem, &nodeinfo, &id) != APR_SUCCESS) {
        *errtype = TYPEMEM;
        return MNODEUI;
    }

    /* Insert the Alias and corresponding Context */
    phost = vhost;
    if (phost->host == NULL && phost->context == NULL)
        return NULL; /* Alias and Context missing */
    while (phost) {
        insert_update_hosts(hoststatsmem, phost->host, id, vid);
        insert_update_contexts(contextstatsmem, phost->context, id, vid, STOPPED);
        phost = phost->next;
        vid++;
    }
    return NULL;
}
/*
 * Process a DUMP command.
 */
static char * process_dump(request_rec *r, char *buff, int *errtype)
{
    int size, i;
    int *id;

    ap_set_content_type(r, "text/plain");

    size = get_max_size_balancer(balancerstatsmem);
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_balancer(balancerstatsmem, id);
    for (i=0; i<size; i++) {
        balancerinfo_t *ou;
        if (get_balancer(balancerstatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        ap_rprintf(r, "balancer: [%d] Name: %s Sticky: %d [%s]/[%s] remove: %d force: %d Timeout: %d Maxtry: %d\n",
                   id[i], ou->balancer, ou->StickySession,
                   ou->StickySessionCookie, ou->StickySessionPath,
                   ou->StickySessionRemove, ou->StickySessionForce, ou->Timeout,
                   ou->Maxattempts);
    }

    size = get_max_size_node(nodestatsmem);
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_node(nodestatsmem, id);
    for (i=0; i<size; i++) {
        nodeinfo_t *ou;
        if (get_node(nodestatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        ap_rprintf(r, "node: [%d:%d],Balancer: %s,JVMRoute: %s,Domain: [%s],Host: %s,Port: %s,Type: %s,flushpackets: %d,flushwait: %d,ping: %d,smax: %d,ttl: %d,timeout: %d\n",
                   id[i], ou->mess.id, ou->mess.balancer, ou->mess.JVMRoute, ou->mess.Domain,
                   ou->mess.Host, ou->mess.Port, ou->mess.Type,
                   ou->mess.flushpackets, ou->mess.flushwait/1000, apr_time_sec(ou->mess.ping), ou->mess.smax,
                   apr_time_sec(ou->mess.ttl), apr_time_sec(ou->mess.timeout));
    }

    size = get_max_size_host(hoststatsmem);
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_host(hoststatsmem, id);
    for (i=0; i<size; i++) {
        hostinfo_t *ou;
        if (get_host(hoststatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        ap_rprintf(r, "host: %d [%s] vhost: %d node: %d\n", id[i], ou->host, ou->vhost,
                  ou->node);
    }

    size = get_max_size_context(contextstatsmem);
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_context(contextstatsmem, id);
    for (i=0; i<size; i++) {
        contextinfo_t *ou;
        if (get_context(contextstatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        ap_rprintf(r, "context: %d [%s] vhost: %d node: %d status: %d\n", id[i], ou->context,
                   ou->vhost, ou->node,
                   ou->status);
    }
    return NULL;
}
/*
 * Process a INFO command.
 * Statics informations ;-)
 */
static char * process_info(request_rec *r, char *buff, int *errtype)
{
    int size, i;
    int *id;

    ap_set_content_type(r, "text/plain");

    size = get_max_size_node(nodestatsmem);
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_node(nodestatsmem, id);
    for (i=0; i<size; i++) {
        nodeinfo_t *ou;
        proxy_worker_stat *proxystat;
        char *flushpackets;
        if (get_node(nodestatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        ap_rprintf(r, "Node: [%d],Name: %s,Balancer: %s,Domain: %s,Host: %s,Port: %s,Type: %s",
                   id[i], ou->mess.JVMRoute, ou->mess.balancer, ou->mess.Domain,
                   ou->mess.Host, ou->mess.Port, ou->mess.Type);
        flushpackets = "Off";
        switch (ou->mess.flushpackets) {
            case flush_on:
                flushpackets = "On";
                break;
            case flush_auto:
                flushpackets = "Auto";
        }
        ap_rprintf(r, ",Flushpackets: %s,Flushwait: %d,Ping: %d,Smax: %d,Ttl: %d",
                   flushpackets, ou->mess.flushwait,
                   ou->mess.ping, ou->mess.smax, ou->mess.ttl);
        proxystat  = (proxy_worker_stat *) ou->stat;
        ap_rprintf(r, ",Elected: %d,Read: %d,Transfered: %d,Connected: %d,Load: %d\n",
                   proxystat->elected, proxystat->read, proxystat->transferred,
                   proxystat->busy, proxystat->lbfactor);
        
    }

    /* Process the Vhosts */
    size = get_max_size_host(hoststatsmem);
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_host(hoststatsmem, id);
    for (i=0; i<size; i++) {
        hostinfo_t *ou;
        if (get_host(hoststatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        ap_rprintf(r, "Vhost: [%d:%d:%d], Alias: %s\n",
                   ou->node, ou->vhost, id[i], ou->host);
    }

    /* Process the Contexts */
    size = get_max_size_context(contextstatsmem);
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_context(contextstatsmem, id);
    for (i=0; i<size; i++) {
        contextinfo_t *ou;
        char *status;
        if (get_context(contextstatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        status = "REMOVED";
        switch (ou->status) {
            case ENABLED:
                status = "ENABLED";
                break;
            case DISABLED:
                status = "DISABLED";
                break;
            case STOPPED:
                status = "STOPPED";
                break;
        }
        ap_rprintf(r, "Context: [%d:%d:%d], Context: %s, Status: %s\n",
                   ou->node, ou->vhost, id[i],
                   ou->context,
                   status);
    }
    return NULL;
}

/* Process a *-APP command that applies to the node */
static char * process_node_cmd(request_rec *r, int status, int *errtype, nodeinfo_t *node)
{
    /* for read the hosts */
    int i,j;
    int size = get_max_size_host(hoststatsmem);
    int *id = apr_palloc(r->pool, sizeof(int) * size);

    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                "process_node_cmd %d processing node: %d", status, node->mess.id);
    size = get_ids_used_host(hoststatsmem, id);
    for (i=0; i<size; i++) {
        hostinfo_t *ou;
        int sizecontext;
        int *idcontext;

        if (get_host(hoststatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        if (ou->node != node->mess.id)
            continue;
        /* If the host corresponds to a node process all contextes */
        sizecontext = get_max_size_context(contextstatsmem);
        idcontext = apr_palloc(r->pool, sizeof(int) * sizecontext);
        sizecontext = get_ids_used_context(contextstatsmem, idcontext);
        for (j=0; j<sizecontext; j++) {
            contextinfo_t *context;
            if (get_context(contextstatsmem, &context, idcontext[j]) != APR_SUCCESS)
                continue;
            if (context->vhost == ou->vhost &&
                context->node == ou->node) {
                /* Process the context */
                if (status != REMOVE) {
                    context->status = status;
                    insert_update_context(contextstatsmem, context);
                } else
                    remove_context(contextstatsmem, context);

            }
        }
        if (status == REMOVE) {
            remove_host(hoststatsmem, ou);
        }
    }

    /* The REMOVE-APP * removes the node (well mark it removed) */
    if (status == REMOVE) {
        int id;
        node->mess.remove = 1;
        insert_update_node(nodestatsmem, node, &id);
    }
    return NULL;

}

/* Process an enable/disable/stop/remove application message */
static char * process_appl_cmd(request_rec *r, char *buff, int status, int *errtype)
{
    nodeinfo_t nodeinfo;
    nodeinfo_t *node;
    struct cluster_host *vhost;

    int i = 0;
    hostinfo_t hostinfo;
    hostinfo_t *host;
    

    char **ptr = process_buff(r, buff);
    if (ptr == NULL) {
        *errtype = TYPESYNTAX;
        return SMESPAR;
    }

    memset(&nodeinfo.mess, '\0', sizeof(nodeinfo.mess));
    /* Map nothing by default */
    vhost = apr_palloc(r->pool, sizeof(struct cluster_host));
    vhost->host = NULL;
    vhost->context = NULL;
    vhost->next = NULL;

    while (ptr[i]) {
        if (strcasecmp(ptr[i], "JVMRoute") == 0) {
            if (strlen(ptr[i+1])>=sizeof(nodeinfo.mess.JVMRoute)) {
                *errtype = TYPESYNTAX;
                return SROUBIG;
            }
            strcpy(nodeinfo.mess.JVMRoute, ptr[i+1]);
            nodeinfo.mess.id = 0;
        }
        if (strcasecmp(ptr[i], "Alias") == 0) {
            if (vhost->host) {
                *errtype = TYPESYNTAX;
                return SMULALB;
            }
            vhost->host = ptr[i+1];
        }
        if (strcasecmp(ptr[i], "Context") == 0) {
            if (vhost->context) {
                *errtype = TYPESYNTAX;
                return SMULCTB;
            }
            vhost->context = ptr[i+1];
        }
        i++;
        i++;
    }

    /* Check for JVMRoute */
    if (nodeinfo.mess.JVMRoute[0] == '\0') {
        *errtype = TYPESYNTAX;
        return SROUBAD;
    }

    /* Read the node */
    node = read_node(nodestatsmem, &nodeinfo);
    if (node == NULL) {
        if (status == REMOVE)
            return NULL; /* Already done */
        *errtype = TYPEMEM;
        return MNODERD;
    }

    /* If the node is marked removed check what to do */
    if (node->mess.remove) {
        if (status == REMOVE)
            return NULL; /* Already done */
        else {
            /* Act has if the node wasn't found */
            *errtype = TYPEMEM;
            return MNODERD;
        }
    }

    /* Process the * APP commands */
    i = strlen(r->uri);
    if (strcmp(r->uri, "*") == 0 || (i>=2 && r->uri[i-1] == '*' && r->uri[i-2] == '/')) {
        return (process_node_cmd(r, status, errtype, node));
    }

    /* Read the ID of the virtual host corresponding to the first Alias */
    hostinfo.node = node->mess.id;
    if (vhost->host != NULL)
        strcpy(hostinfo.host, vhost->host);
    else
        hostinfo.host[0] = '\0';

    hostinfo.id = 0;
    host = read_host(hoststatsmem, &hostinfo);
    if (host == NULL) {
        int vid = 1; /* XXX: That is not really the right value, but that works most time */
        /* If REMOVE ignores it */
        if (status == REMOVE)
            return NULL;
        /* If the Host doesn't exist yet create it */
        insert_update_hosts(hoststatsmem, vhost->host, node->mess.id, vid);
        hostinfo.id = 0;
        if (vhost->host != NULL)
            strcpy(hostinfo.host, vhost->host);
        else
            hostinfo.host[0] = '\0';
        host = read_host(hoststatsmem, &hostinfo);
        if (host == NULL) {
            *errtype = TYPEMEM;
            return MHOSTRD; 
        }
    }

    /* Now update each context from Context: part */
    insert_update_contexts(contextstatsmem, vhost->context, node->mess.id, host->vhost, status);

    /* Remove the host if all the contextes have been removed */
    if (status == REMOVE) {
        int size = get_max_size_context(contextstatsmem);
        int *id = apr_palloc(r->pool, sizeof(int) * size);
        size = get_ids_used_context(contextstatsmem, id);
        for (i=0; i<size; i++) {
            contextinfo_t *ou;
            if (get_context(contextstatsmem, &ou, id[i]) != APR_SUCCESS)
                continue;
            if (ou->vhost == host->vhost &&
                ou->node == node->mess.id)
                break;
        }
        if (i==size) {
            hostinfo.id = host->id;
            remove_host(hoststatsmem, &hostinfo);
        }
    }
    return NULL;
}
static char * process_enable(request_rec *r, char *buff, int *errtype)
{
    return process_appl_cmd(r, buff, ENABLED, errtype);
}
static char * process_disable(request_rec *r, char *buff, int *errtype)
{
    return process_appl_cmd(r, buff, DISABLED, errtype);
}
static char * process_stop(request_rec *r, char *buff, int *errtype)
{
    return process_appl_cmd(r, buff, STOPPED, errtype);
}
static char * process_remove(request_rec *r, char *buff, int *errtype)
{
    return process_appl_cmd(r, buff, REMOVE, errtype);
}

/*
 * Call the ping/pong logic
 * Do a ping/png request to the node and set the load factor.
 */
static int isnode_up(request_rec *r, int id, char *scheme, int Load)
{
    if (balancerhandler != NULL) {
        return (balancerhandler->proxy_node_isup(r, id, scheme, Load));
    }
    return OK;
}
/*
 * Process the STATUS command
 * Load -1 : Broken
 * Load 0  : Standby.
 * Load 1-100 : Load factor.
 */
static char * process_status(request_rec *r, char *buff, int *errtype)
{
    int Load = -1;
    nodeinfo_t nodeinfo;
    nodeinfo_t *node;

    int i = 0;

    char **ptr = process_buff(r, buff);
    if (ptr == NULL) {
        *errtype = TYPESYNTAX;
        return SMESPAR;
    }

    while (ptr[i]) {
        if (strcasecmp(ptr[i], "JVMRoute") == 0) {
            if (strlen(ptr[i+1])>=sizeof(nodeinfo.mess.JVMRoute)) {
                *errtype = TYPESYNTAX;
                return SROUBIG;
            }
            strcpy(nodeinfo.mess.JVMRoute, ptr[i+1]);
            nodeinfo.mess.id = 0;
        }
        else if (strcasecmp(ptr[i], "Load") == 0) {
            Load = atoi(ptr[i+1]);
        }
        else {
            *errtype = TYPESYNTAX;
            return apr_psprintf(r->pool, SBADFLD, ptr[i]);
        }
        i++;
        i++;
    }

    /* Read the node */
    node = read_node(nodestatsmem, &nodeinfo);
    if (node == NULL) {
        *errtype = TYPEMEM;
        return MNODERD;
    }

    /*
     * If the node is usualable do a ping/pong to prevent Split-Brain Syndrome
     * and update the worker status and load factor acccording to the test result.
     */
    ap_set_content_type(r, "text/plain");
    ap_rprintf(r, "Type=STATUS-RSP&JVMRoute=%s", nodeinfo.mess.JVMRoute);

    if (isnode_up(r, node->mess.id, node->mess.Type, Load) != OK)
        ap_rprintf(r, "&State=NOTOK");
    else
        ap_rprintf(r, "&State=OK");
    if (ap_my_generation)
        ap_rprintf(r, "&id=%d", ap_my_generation);
    else
        ap_rprintf(r, "&id=%d", ap_scoreboard_image->global->restart_time);

    ap_rprintf(r, "\n");
    return NULL;
}

/*
 * Decodes a '%' escaped string, and returns the number of characters
 * (From mod_proxy_ftp.c).
 */

/* already called in the knowledge that the characters are hex digits */
/* Copied from modules/proxy/proxy_util.c */
static int mod_manager_hex2c(const char *x)
{
    int i, ch;

#if !APR_CHARSET_EBCDIC
    ch = x[0];
    if (apr_isdigit(ch)) {
        i = ch - '0';
    }
    else if (apr_isupper(ch)) {
        i = ch - ('A' - 10);
    }
    else {
        i = ch - ('a' - 10);
    }
    i <<= 4;

    ch = x[1];
    if (apr_isdigit(ch)) {
        i += ch - '0';
    }
    else if (apr_isupper(ch)) {
        i += ch - ('A' - 10);
    }
    else {
        i += ch - ('a' - 10);
    }
    return i;
#else /*APR_CHARSET_EBCDIC*/
    /*
     * we assume that the hex value refers to an ASCII character
     * so convert to EBCDIC so that it makes sense locally;
     *
     * example:
     *
     * client specifies %20 in URL to refer to a space char;
     * at this point we're called with EBCDIC "20"; after turning
     * EBCDIC "20" into binary 0x20, we then need to assume that 0x20
     * represents an ASCII char and convert 0x20 to EBCDIC, yielding
     * 0x40
     */
    char buf[1];

    if (1 == sscanf(x, "%2x", &i)) {
        buf[0] = i & 0xFF;
        ap_xlate_proto_from_ascii(buf, 1);
        return buf[0];
    }
    else {
        return 0;
    }
#endif /*APR_CHARSET_EBCDIC*/
}
static int decodeenc(char *x)
{
    int i, j, ch;

    if (x[0] == '\0')
        return 0;               /* special case for no characters */
    for (i = 0, j = 0; x[i] != '\0'; i++, j++) {
        /* decode it if not already done */
        ch = x[i];
        if (ch == '%' && apr_isxdigit(x[i + 1]) && apr_isxdigit(x[i + 2])) {
            ch = mod_manager_hex2c(&x[i + 1]);
            i += 2;
        }
        x[j] = ch;
    }
    x[j] = '\0';
    return j;
}
/*
 * This routine is called before mod_proxy translate name.
 * This allows us to make decisions before mod_proxy
 * to be able to fill tables even with ProxyPass / balancer...
 */
static int manager_trans(request_rec *r)
{
    int ours = 0;
    core_dir_config *conf =
        (core_dir_config *)ap_get_module_config(r->per_dir_config,
                                                &core_module);
 
    if (conf && conf->handler && r->method_number == M_GET &&
        strcmp(conf->handler, "mod_cluster-manager") == 0) {
        r->handler = "mod_cluster-manager";
        r->filename = apr_pstrdup(r->pool, r->uri);
        return OK;
    }

    if (strcasecmp(r->method, "CONFIG") == 0)
        ours = 1;
    else if (strcasecmp(r->method, "ENABLE-APP") == 0)
        ours = 1;
    else if (strcasecmp(r->method, "DISABLE-APP") == 0)
        ours = 1;
    else if (strcasecmp(r->method, "STOP-APP") == 0)
        ours = 1;
    else if (strcasecmp(r->method, "REMOVE-APP") == 0)
        ours = 1;
    else if (strcasecmp(r->method, "STATUS") == 0)
        ours = 1;
    else if (strcasecmp(r->method, "DUMP") == 0)
        ours = 1;
    else if (strcasecmp(r->method, "ERROR") == 0)
        ours = 1;
    else if (strcasecmp(r->method, "INFO") == 0)
        ours = 1;
    if (ours) {
        /* The method one of ours */
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                    "manager_trans %s (%s)", r->method, r->uri);
        r->handler = "mod-cluster";
        r->filename = apr_pstrdup(r->pool, r->uri);
        return OK;
    }
    
    return DECLINED;
}

/*
 * Process the parameters and display corresponding informations.
 */
static void manager_info_contexts(request_rec *r, int node, int host)
{
    int size, i;
    int *id;
    /* Process the Contexts */
    ap_rprintf(r, "<h3>Contexts:</h3>");
    ap_rprintf(r, "<pre>");
    size = get_max_size_context(contextstatsmem);
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_context(contextstatsmem, id);
    for (i=0; i<size; i++) {
        contextinfo_t *ou;
        char *status;
        if (get_context(contextstatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        if (ou->node != node || ou->vhost != host)
            continue;
        status = "REMOVED";
        switch (ou->status) {
            case ENABLED:
                status = "ENABLED";
                break;
            case DISABLED:
                status = "DISABLED";
                break;
            case STOPPED:
                status = "STOPPED";
                break;
        }
        ap_rprintf(r, "%s, Status: %s\n", ou->context, status);
    }
    ap_rprintf(r, "</pre>");
}
static void manager_info_hosts(request_rec *r, int node)
{
    int size, i;
    int *id;
    int vhost = 0;

    /* Process the Vhosts */
    size = get_max_size_host(hoststatsmem);
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_host(hoststatsmem, id);
    for (i=0; i<size; i++) {
        hostinfo_t *ou;
        if (get_host(hoststatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        if (ou->node != node)
            continue;
        if (ou->vhost != vhost) {
            if (vhost)
                ap_rprintf(r, "</pre>");
            ap_rprintf(r, "<h2> Virtual Host %d:</h2>", ou->vhost);
            manager_info_contexts(r, ou->node, ou->vhost);
            ap_rprintf(r, "<h3>Aliases:</h3>");
            ap_rprintf(r, "<pre>");
            vhost = ou->vhost;
        }
        ap_rprintf(r, "%s\n", ou->host);
    }
    ap_rprintf(r, "</pre>");

}
static void manager_sessionid(request_rec *r)
{
    int size, i;
    int *id;

    /* Process the Sessionids */
    ap_rprintf(r, "<h1>SessionIDs:</h1>");
    ap_rprintf(r, "<pre>");
    size = get_max_size_sessionid(sessionidstatsmem);
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_sessionid(sessionidstatsmem, id);
    for (i=0; i<size; i++) {
        sessionidinfo_t *ou;
        if (get_sessionid(sessionidstatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        ap_rprintf(r, "id: %s route: %s\n", ou->sessionid, ou->JVMRoute);
    }
    ap_rprintf(r, "</pre>");

}
static int count_sessionid(request_rec *r, char *route)
{
    int size, i;
    int *id;
    int count = 0;

    /* Count the sessionid corresponding to the route */
    size = get_max_size_sessionid(sessionidstatsmem);
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_sessionid(sessionidstatsmem, id);
    for (i=0; i<size; i++) {
        sessionidinfo_t *ou;
        if (get_sessionid(sessionidstatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        if (strcmp(route, ou->JVMRoute) == 0)
            count++; 
    }
    return count;
}
static int manager_info(request_rec *r)
{
    int size, i;
    int *id;
    apr_table_t *params = apr_table_make(r->pool, 10);
    int access_status;
    const char *name;

    if (r->args) {
        char *args = apr_pstrdup(r->pool, r->args);
        char *tok, *val;
        while (args && *args) {
            if ((val = ap_strchr(args, '='))) {
                *val++ = '\0';
                if ((tok = ap_strchr(val, '&')))
                    *tok++ = '\0';
                /*
                 * Special case: contexts contain path information
                 */
                if ((access_status = ap_unescape_url(val)) != OK)
                    if (strcmp(args, "context") || (access_status !=  HTTP_NOT_FOUND))
                        return access_status;
                apr_table_setn(params, args, val);
                args = tok;
            }
            else
                return HTTP_BAD_REQUEST;
        }
    }

    /*
     * Check that the supplied nonce matches this server's nonce;
     * otherwise ignore all parameters, to prevent a CSRF attack.
     */
    if ((name = apr_table_get(params, "nonce")) == NULL
        || strcmp(balancer_nonce, name) != 0) {
        apr_table_clear(params);
    }

    /* process the parameters */
    if (r->args) {
        /* Process the Refresh parameter */
        const char *val = apr_table_get(params, "Refresh");
        if (val) {
            long t = atol(val);
            apr_table_set(r->headers_out, "Refresh", apr_ltoa(r->pool,t < 1 ? 10 : t));
        }
    }
    
    ap_set_content_type(r, "text/html; charset=ISO-8859-1");
    ap_rputs(DOCTYPE_HTML_3_2
             "<html><head>\n<title>Mod_cluster Status</title>\n</head><body>\n",
             r);
    ap_rvputs(r, "<a href=\"", r->uri, "?nonce=", balancer_nonce,
                 "&refresh=10",
                 "\">Auto Refresh</a>", NULL);

    ap_rputs("<pre>", r);
    size = get_max_size_node(nodestatsmem);
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_node(nodestatsmem, id);
    for (i=0; i<size; i++) {
        nodeinfo_t *ou;
        proxy_worker_stat *proxystat;
        char *flushpackets;
        if (get_node(nodestatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        ap_rprintf(r, "<h1> Node %s (%s://%s:%s):</h1>\n",
                   ou->mess.JVMRoute, ou->mess.Type, ou->mess.Host, ou->mess.Port);

        ap_rprintf(r, "Balancer: %s,Domain: %s", ou->mess.balancer, ou->mess.Domain);

        flushpackets = "Off";
        switch (ou->mess.flushpackets) {
            case flush_on:
                flushpackets = "On";
                break;
            case flush_auto:
                flushpackets = "Auto";
        }
        ap_rprintf(r, ",Flushpackets: %s,Flushwait: %d,Ping: %d,Smax: %d,Ttl: %d",
                   flushpackets, ou->mess.flushwait,
                   ou->mess.ping, ou->mess.smax, ou->mess.ttl);
        proxystat  = (proxy_worker_stat *) ou->stat;
        ap_rprintf(r, ",Elected: %d,Read: %d,Transfered: %d,Connected: %d,Load: %d Num sessions: %d\n",
                   proxystat->elected, proxystat->read, proxystat->transferred,
                   proxystat->busy, proxystat->lbfactor, count_sessionid(r, ou->mess.JVMRoute));

        /* Process the Vhosts */
        manager_info_hosts(r, id[i]); 
    }
    /* Display the sessions */
    manager_sessionid(r);


    ap_rputs("</body></html>\n", r);
    return OK;
}

/* Process the requests from the ModClusterService */
static int manager_handler(request_rec *r)
{
    apr_bucket_brigade *input_brigade;
    char *errstring = NULL;
    int errtype = 0;
    char *buff;
    apr_size_t bufsiz=MAXMESSSIZE;
    apr_status_t status;
  
    if (strcmp(r->handler, "mod_cluster-manager") == 0) {
        /* Display the nodes information */
        if (r->method_number != M_GET)
            return DECLINED;
        return(manager_info(r));
    }

    if (strcmp(r->handler, "mod-cluster"))
        return DECLINED;

    /* Use a buffer to read the message */
    buff = apr_pcalloc(r->pool, MAXMESSSIZE);
    input_brigade = apr_brigade_create(r->connection->pool, r->connection->bucket_alloc);
    status = ap_get_brigade(r->input_filters, input_brigade, AP_MODE_READBYTES, APR_BLOCK_READ, MAXMESSSIZE);
    if (status != APR_SUCCESS) {
        errstring = apr_psprintf(r->pool, SREADER, r->method);
        r->status_line = apr_psprintf(r->pool, "ERROR");
        apr_table_setn(r->err_headers_out, "Version", VERSION_PROTOCOL);
        apr_table_setn(r->err_headers_out, "Type", "SYNTAX");
        apr_table_setn(r->err_headers_out, "Mess", errstring);
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                "manager_handler %s error: %s", r->method, errstring);
        return 500;
    }
    apr_brigade_flatten(input_brigade, buff, &bufsiz);
    buff[bufsiz] = '\0';

    /* XXX: Size limit it? */
    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                "manager_handler %s (%s) processing: \"%s\"", r->method, r->uri, buff);
    decodeenc(buff);
    if (strcasecmp(r->method, "CONFIG") == 0)
        errstring = process_config(r, buff, &errtype);
    /* Application handling */
    else if (strcasecmp(r->method, "ENABLE-APP") == 0)
        errstring = process_enable(r, buff, &errtype);
    else if (strcasecmp(r->method, "DISABLE-APP") == 0)
        errstring = process_disable(r, buff, &errtype);
    else if (strcasecmp(r->method, "STOP-APP") == 0)
        errstring = process_stop(r, buff, &errtype);
    else if (strcasecmp(r->method, "REMOVE-APP") == 0)
        errstring = process_remove(r, buff, &errtype);
    /* Status handling */
    else if (strcasecmp(r->method, "STATUS") == 0)
        errstring = process_status(r, buff, &errtype);
    else if (strcasecmp(r->method, "DUMP") == 0)
        errstring = process_dump(r, buff, &errtype);
    else if (strcasecmp(r->method, "INFO") == 0)
        errstring = process_info(r, buff, &errtype);
    else {
        errstring = SCMDUNS;
        errtype = TYPESYNTAX;
    }

    /* Check error string and build the error message */
    if (errstring) {
        r->status_line = apr_psprintf(r->pool, "ERROR");
        apr_table_setn(r->err_headers_out, "Version", VERSION_PROTOCOL);
        switch (errtype) {
          case TYPESYNTAX:
             apr_table_setn(r->err_headers_out, "Type", "SYNTAX");
             break;
          case TYPEMEM:
             apr_table_setn(r->err_headers_out, "Type", "MEM");
             break;
          default:
             apr_table_setn(r->err_headers_out, "Type", "GENERAL");
             break;
        }
        apr_table_setn(r->err_headers_out, "Mess", errstring);
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                "manager_handler %s error: %s", r->method, errstring);
        return 500;
    }

    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                "manager_handler %s  OK", r->method);

    return (OK);
}

/*
 *  Attach to the shared memory when the child is created.
 */
static void  manager_child_init(apr_pool_t *p, server_rec *s)
{
    char *node;
    char *context;
    char *host;
    char *balancer;
    char *sessionid;
    mod_manager_config *mconf = ap_get_module_config(s->module_config, &manager_module);

    if (mconf->basefilename) {
        node = apr_pstrcat(p, mconf->basefilename, ".node", NULL);
        context = apr_pstrcat(p, mconf->basefilename, ".context", NULL);
        host = apr_pstrcat(p, mconf->basefilename, ".host", NULL);
        balancer = apr_pstrcat(p, mconf->basefilename, ".balancer", NULL);
        sessionid = apr_pstrcat(p, mconf->basefilename, ".sessionid", NULL);
    } else {
        node = ap_server_root_relative(p, "logs/manager.node");
        context = ap_server_root_relative(p, "logs/manager.context");
        host = ap_server_root_relative(p, "logs/manager.host");
        balancer = ap_server_root_relative(p, "logs/manager.balancer");
        sessionid = ap_server_root_relative(p, "logs/manager.sessionid");
    }

    nodestatsmem = get_mem_node(node, &mconf->maxnode, p, storage);
    if (nodestatsmem == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "get_mem_node %s failed", node);
        return;
    }

    contextstatsmem = get_mem_context(context, &mconf->maxcontext, p, storage);
    if (contextstatsmem == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "get_mem_context failed");
        return;
    }

    hoststatsmem = get_mem_host(host, &mconf->maxhost, p, storage);
    if (hoststatsmem == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "get_mem_host failed");
        return;
    }

    balancerstatsmem = get_mem_balancer(balancer, &mconf->maxhost, p, storage);
    if (balancerstatsmem == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "get_mem_balancer failed");
        return;
    }

    sessionidstatsmem = get_mem_sessionid(sessionid, &mconf->maxsessionid, p, storage);
    if (sessionidstatsmem == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "get_mem_sessionid failed");
        return;
    }
}

/*
 * Supported directives.
 */
static const char *cmd_manager_maxcontext(cmd_parms *cmd, void *mconfig, const char *word)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    mconf->maxcontext = atoi(word);
    return NULL;
}
static const char *cmd_manager_maxnode(cmd_parms *cmd, void *mconfig, const char *word)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    mconf->maxnode = atoi(word);
    return NULL;
}
static const char *cmd_manager_maxhost(cmd_parms *cmd, void *mconfig, const char *word)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    mconf->maxhost = atoi(word);
    return NULL;
}
static const char *cmd_manager_maxsessionid(cmd_parms *cmd, void *mconfig, const char *word)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    mconf->maxsessionid = atoi(word);
    return NULL;
}
static const char *cmd_manager_memmanagerfile(cmd_parms *cmd, void *mconfig, const char *word)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    mconf->basefilename = apr_pstrdup(cmd->pool, word);
    if (apr_dir_make_recursive(mconf->basefilename, APR_UREAD | APR_UWRITE | APR_UEXECUTE, cmd->pool) != APR_SUCCESS)
        return  "Can't create directory corresponding to MemManagerFile";
    return NULL;
}
static const char *cmd_manager_balancername(cmd_parms *cmd, void *mconfig, const char *word)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    /* XXX: create the entry in the shared balancer table */
    return NULL;
}
static const char*cmd_manager_pers(cmd_parms *cmd, void *dummy, const char *arg)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    if (strcasecmp(arg, "Off") == 0)
       mconf->persistent = 0;
    else if (strcasecmp(arg, "On") == 0)
       mconf->persistent = CREPER_SLOTMEM;
    else {
       return "PersistSlots must be one of: "
              "off | on";
    }
    return NULL;
}

static const command_rec  manager_cmds[] =
{
    AP_INIT_TAKE1(
        "Maxcontext",
        cmd_manager_maxcontext,
        NULL,
        OR_ALL,
        "Maxcontext - number max context supported by mod_cluster"
    ),
    AP_INIT_TAKE1(
        "Maxnode",
        cmd_manager_maxnode,
        NULL,
        OR_ALL,
        "Maxnode - number max node supported by mod_cluster"
    ),
    AP_INIT_TAKE1(
        "Maxhost",
        cmd_manager_maxhost,
        NULL,
        OR_ALL,
        "Maxhost - number max host (Alias in virtual hosts) supported by mod_cluster"
    ),
    AP_INIT_TAKE1(
        "Maxsessionid",
        cmd_manager_maxsessionid,
        NULL,
        OR_ALL,
        "Maxsessionid - number session (Used to track number of sessions per nodes) supported by mod_cluster"
    ),
    AP_INIT_TAKE1(
        "MemManagerFile",
        cmd_manager_memmanagerfile,
        NULL,
        OR_ALL,
        "MemManagerFile - base name of the files used to create/attach to shared memory"
    ),
    AP_INIT_TAKE1(
        "ManagerBalancerName",
        cmd_manager_balancername,
        NULL,
        OR_ALL,
        "ManagerBalancerName - name of a balancer corresponding to the manager"
    ),
    AP_INIT_TAKE1(
        "PersistSlots",
        cmd_manager_pers,
        NULL,
        OR_ALL,
        "PersistSlots - Persist the slot mem elements on | off (Default: off No persistence)"
    ),
    {NULL}
};

/* hooks declaration */

static void manager_hooks(apr_pool_t *p)
{
    static const char * const aszSucc[]={ "mod_proxy.c", NULL };

    /* Create the shared tables for mod_proxy_cluster */
    ap_hook_post_config(manager_init, NULL, NULL, APR_HOOK_MIDDLE);

    /* Attach to the shared tables with create the child */
    ap_hook_child_init(manager_child_init, NULL, NULL, APR_HOOK_MIDDLE);

    /* post read_request handling: to be handle to use ProxyPass / */
    ap_hook_translate_name(manager_trans, NULL, aszSucc,
                              APR_HOOK_FIRST);

    /* Process the request from the ModClusterService */
    ap_hook_handler(manager_handler, NULL, NULL, APR_HOOK_MIDDLE);

    /* Register nodes/hosts/contexts table provider */
    ap_register_provider(p, "manager" , "shared", "0", &node_storage);
    ap_register_provider(p, "manager" , "shared", "1", &host_storage);
    ap_register_provider(p, "manager" , "shared", "2", &context_storage);
    ap_register_provider(p, "manager" , "shared", "3", &balancer_storage);
    ap_register_provider(p, "manager" , "shared", "4", &sessionid_storage);
}

/*
 * Config creation stuff
 */
static void *create_manager_config(apr_pool_t *p)
{
    mod_manager_config *mconf = apr_pcalloc(p, sizeof(*mconf));

    mconf->basefilename = NULL;
    mconf->maxcontext = DEFMAXCONTEXT;
    mconf->maxnode = DEFMAXNODE;
    mconf->maxhost = DEFMAXHOST;
    mconf->maxsessionid = DEFMAXSESSIONID;
    mconf->last_updated = 0;
    mconf->persistent = 0;
    return mconf;
}

static void *create_manager_server_config(apr_pool_t *p, server_rec *s)
{
    return(create_manager_config(p));
}
static void *merge_manager_server_config(apr_pool_t *p, void *server1_conf,
                                         void *server2_conf)
{
    mod_manager_config *mconf1 = (mod_manager_config *) server1_conf;
    mod_manager_config *mconf2 = (mod_manager_config *) server2_conf;
    mod_manager_config *mconf = apr_pcalloc(p, sizeof(*mconf));

    mconf->basefilename = NULL;
    mconf->maxcontext = DEFMAXCONTEXT;
    mconf->maxnode = DEFMAXNODE;
    mconf->last_updated = 0;
    mconf->persistent = 0;

    if (mconf2->basefilename)
        mconf->basefilename = apr_pstrdup(p, mconf2->basefilename);
    else if (mconf1->basefilename)
        mconf->basefilename = apr_pstrdup(p, mconf1->basefilename);

    if (mconf2->maxcontext != DEFMAXCONTEXT)
        mconf->maxcontext = mconf2->maxcontext;
    else if (mconf1->maxcontext != DEFMAXCONTEXT)
        mconf->maxcontext = mconf1->maxcontext;

    if (mconf2->maxnode != DEFMAXNODE)
        mconf->maxnode = mconf2->maxnode;
    else if (mconf1->maxnode != DEFMAXNODE)
        mconf->maxnode = mconf1->maxnode;

    if (mconf2->maxhost != DEFMAXHOST)
        mconf->maxhost = mconf2->maxhost;
    else if (mconf1->maxhost != DEFMAXHOST)
        mconf->maxhost = mconf1->maxhost;

    if (mconf2->maxsessionid != DEFMAXSESSIONID)
        mconf->maxsessionid = mconf2->maxsessionid;
    else if (mconf1->maxsessionid != DEFMAXSESSIONID)
        mconf->maxsessionid = mconf1->maxsessionid;

    if (mconf2->persistent != 0)
        mconf->persistent = mconf2->persistent;
    else if (mconf1->persistent != 0)
        mconf->persistent = mconf1->persistent;

    return mconf;
}

/* Module declaration */

module AP_MODULE_DECLARE_DATA manager_module = {
    STANDARD20_MODULE_STUFF,
    NULL,
    NULL,
    create_manager_server_config,
    merge_manager_server_config,
    manager_cmds,      /* command table */
    manager_hooks      /* register hooks */
};
