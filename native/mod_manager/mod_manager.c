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
#include "domain.h"
#include "jgroupsid.h"

#define DEFMAXCONTEXT   100
#define DEFMAXNODE      20
#define DEFMAXHOST      20
#define DEFMAXSESSIONID 0 /* it has performance/security impact */
#define DEFMAXJGROUPSID  0
#define MAXMESSSIZE     1024

/* Error messages */
#define TYPESYNTAX 1
#define SMESPAR "SYNTAX: Can't parse message"
#define SBALBIG "SYNTAX: Balancer field too big"
#define SBAFBIG "SYNTAX: A field is too big"
#define SROUBIG "SYNTAX: JVMRoute field too big"
#define SROUBAD "SYNTAX: JVMRoute can't be empty"
#define SDOMBIG "SYNTAX: LBGroup field too big"
#define SHOSBIG "SYNTAX: Host field too big"
#define SPORBIG "SYNTAX: Port field too big"    
#define STYPBIG "SYNTAX: Type field too big"
#define SALIBAD "SYNTAX: Alias without Context"
#define SCONBAD "SYNTAX: Context without Alias"
#define SBADFLD "SYNTAX: Invalid field \"%s\" in message"
#define SMISFLD "SYNTAX: Mandatory field(s) missing in message"
#define SCMDUNS "SYNTAX: Command is not supported"
#define SMULALB "SYNTAX: Only one Alias in APP command"
#define SMULCTB "SYNTAX: Only one Context in APP command"
#define SREADER "SYNTAX: %s can't read POST data"

#define SJIDBIG "SYNTAX: JGroupUuid field too big"
#define SJDDBIG "SYNTAX: JGroupData field too big"
#define SJIDBAD "SYNTAX: JGroupUuid can't be empty"

#define TYPEMEM 2
#define MNODEUI "MEM: Can't update or insert node"
#define MNODERM "MEM: Old node still exist"
#define MBALAUI "MEM: Can't update or insert balancer"
#define MNODERD "MEM: Can't read node"
#define MHOSTRD "MEM: Can't read host alias"
#define MHOSTUI "MEM: Can't update or insert host alias"
#define MCONTUI "MEM: Can't update or insert context"
#define MJBIDRD "MEM: Can't read JGroupId"
#define MJBIDUI "MEM: Can't update or insert JGroupId"

/* Protocol version supported */
#define VERSION_PROTOCOL "0.2.1"

/* Internal substitution for node commands */
#define NODE_COMMAND "/NODE_COMMAND"

/* range of the commands */
#define RANGECONTEXT 0
#define RANGENODE    1
#define RANGEDOMAIN  2

/* define HAVE_CLUSTER_EX_DEBUG to have extented debug in mod_cluster */
#define HAVE_CLUSTER_EX_DEBUG 0

/* shared memory */
static mem_t *contextstatsmem = NULL;
static mem_t *nodestatsmem = NULL;
static mem_t *hoststatsmem = NULL;
static mem_t *balancerstatsmem = NULL;
static mem_t *sessionidstatsmem = NULL;
static mem_t *domainstatsmem = NULL;
static mem_t *jgroupsidstatsmem = NULL;

static slotmem_storage_method *storage = NULL;
static balancer_method *balancerhandler = NULL;
static void (*advertise_info)(request_rec *) = NULL;

module AP_MODULE_DECLARE_DATA manager_module;

static char balancer_nonce[APR_UUID_FORMATTED_LENGTH + 1];

typedef struct mod_manager_config
{
    /* base name for the shared memory */
    char *basefilename;
    /* max number of context supported */
    int maxcontext;
    /* max number of node supported */
    int maxnode;
    /* max number of host supported */
    int maxhost;
    /* max number of session supported */
    int maxsessionid;
    /* max number of jgroupsid supported */
    int maxjgroupsid;

    /* last time the node update logic was called */
    apr_time_t last_updated;

    /* Should be the slotmem persisted (1) or not (0) */
    int persistent;

    /* check for nonce in the command logic */
    int nonce;

    /* default name for balancer */
    char *balancername;

    /* allow aditional display */
    int allow_display;
    /* allow command logic */
    int allow_cmd;
    /* don't context in first status page */
    int reduce_display;  
    /* maximum message size */
    int maxmesssize;
    /* Enable MCPM receiver */
    int enable_mcpm_receive;

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
    if (nodestatsmem)
        return(get_max_size_node(nodestatsmem));
    else
        return 0;
}
static int loc_get_max_size_jgroupsid()
{
    if (jgroupsidstatsmem)
        return(get_max_size_jgroupsid(jgroupsidstatsmem));
    else
        return 0;
}
static apr_status_t loc_remove_node(nodeinfo_t *node)
{
    return (remove_node(nodestatsmem, node));
}
static apr_status_t loc_find_node(nodeinfo_t **node, const char *route)
{
    return (find_node(nodestatsmem, node, route));
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

    size = loc_get_max_size_node();
    if (size == 0)
        return 0; /* broken */
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
static int loc_get_max_size_context()
{
    if (contextstatsmem)
        return(get_max_size_context(contextstatsmem));
    else
        return 0;
}
static int loc_get_max_size_host()
{
    if (hoststatsmem)
        return(get_max_size_host(hoststatsmem));
    else
        return 0;
}
/* Remove the virtual hosts and contexts corresponding the node */
static void loc_remove_host_context(int node, apr_pool_t *pool)
{
    /* for read the hosts */
    int i;
    int size = loc_get_max_size_host();
    int *id;
    int sizecontext = loc_get_max_size_context();
    int *idcontext;

    if (size == 0)
        return;
    id = apr_palloc(pool, sizeof(int) * size);
    idcontext = apr_palloc(pool, sizeof(int) * sizecontext);
    size = get_ids_used_host(hoststatsmem, id);
    for (i=0; i<size; i++) {
        hostinfo_t *ou;

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
static const struct node_storage_method node_storage =
{
    loc_read_node,
    loc_get_ids_used_node,
    loc_get_max_size_node,
    loc_worker_nodes_need_update,
    loc_worker_nodes_are_updated,
    loc_remove_node,
    loc_find_node,
    loc_remove_host_context,
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
    if (balancerstatsmem)
        return(get_max_size_balancer(balancerstatsmem));
    else
        return 0;
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
    if (sessionidstatsmem)
        return(get_max_size_sessionid(sessionidstatsmem));
    else
        return 0;
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

/*
 * routines for the domain_storage_method
 */
static apr_status_t loc_read_domain(int ids, domaininfo_t **domain)
{
    return (get_domain(domainstatsmem, domain, ids));
}
static int loc_get_ids_used_domain(int *ids)
{
    return(get_ids_used_domain(domainstatsmem, ids)); 
}
static int loc_get_max_size_domain()
{
    if (domainstatsmem)
        return(get_max_size_domain(domainstatsmem));
    else
        return 0;
}
static apr_status_t loc_remove_domain(domaininfo_t *domain)
{
    return (remove_domain(domainstatsmem, domain));
}
static apr_status_t loc_insert_update_domain(domaininfo_t *domain)
{
    return (insert_update_domain(domainstatsmem, domain));
}
static apr_status_t loc_find_domain(domaininfo_t **domain, const char *route, const char *balancer)
{
    return (find_domain(domainstatsmem, domain, route, balancer));
}
static const struct  domain_storage_method domain_storage =
{
    loc_read_domain,
    loc_get_ids_used_domain,
    loc_get_max_size_domain,
    loc_remove_domain,
    loc_insert_update_domain,
    loc_find_domain
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
    char *domain;
    char *jgroupsid;
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
        node = apr_pstrcat(ptemp, mconf->basefilename, "/manager.node", NULL);
        context = apr_pstrcat(ptemp, mconf->basefilename, "/manager.context", NULL);
        host = apr_pstrcat(ptemp, mconf->basefilename, "/manager.host", NULL);
        balancer = apr_pstrcat(ptemp, mconf->basefilename, "/manager.balancer", NULL);
        sessionid = apr_pstrcat(ptemp, mconf->basefilename, "/manager.sessionid", NULL);
        domain = apr_pstrcat(ptemp, mconf->basefilename, "/manager.domain", NULL);
        jgroupsid = apr_pstrcat(ptemp, mconf->basefilename, "/manager.jgroupsid", NULL);
    } else {
        node = ap_server_root_relative(ptemp, "logs/manager.node");
        context = ap_server_root_relative(ptemp, "logs/manager.context");
        host = ap_server_root_relative(ptemp, "logs/manager.host");
        balancer = ap_server_root_relative(ptemp, "logs/manager.balancer");
        sessionid = ap_server_root_relative(ptemp, "logs/manager.sessionid");
        domain = ap_server_root_relative(ptemp, "logs/manager.domain");
        jgroupsid = ap_server_root_relative(ptemp, "logs/manager.jgroupsid");
    }

    /* Do some sanity checks */
    if (mconf->maxhost < mconf->maxnode)
        mconf->maxhost = mconf->maxnode;
    if (mconf->maxcontext < mconf->maxhost)
        mconf->maxcontext = mconf->maxhost;

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
    if (get_last_mem_error(nodestatsmem) != APR_SUCCESS) {
        char buf[120];
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "create_mem_node %s failed: %s",
                     node, apr_strerror(get_last_mem_error(nodestatsmem), buf, sizeof(buf)));
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

    sessionidstatsmem = create_mem_sessionid(sessionid, &mconf->maxsessionid, mconf->persistent, p, storage);
    if (sessionidstatsmem == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "create_mem_sessionid failed");
        return  !OK;
    }

    domainstatsmem = create_mem_domain(domain, &mconf->maxnode, mconf->persistent, p, storage);
    if (domainstatsmem == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "create_mem_domain failed");
        return  !OK;
    }

    jgroupsidstatsmem = create_mem_jgroupsid(jgroupsid, &mconf->maxjgroupsid, mconf->persistent, p, storage);
    if (jgroupsidstatsmem == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "create_mem_jgroupsid failed");
        return  !OK;
    }

    /* Get a provider to ping/pong logics */

    balancerhandler = ap_lookup_provider("proxy_cluster", "balancer", "0");
    if (balancerhandler == NULL) {
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_WARNING, 0, s, "can't find a ping/pong logic");
    }

    advertise_info = ap_lookup_provider("advertise", "info", "0");

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
static apr_status_t  insert_update_hosts(mem_t *mem, char *str, int node, int vhost)
{
    char *ptr = str;
    char *previous = str;
    hostinfo_t info;
    char empty[1] = {'\0'};
    apr_status_t status;

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
            status = insert_update_host(mem, &info); 
            if (status != APR_SUCCESS)
                return status;
            previous = ptr + 1;
        }
        ptr ++;
    }
    strncpy(info.host, previous, sizeof(info.host));
    return insert_update_host(mem, &info); 
}
/*
 * Insert the context from Context information
 * Note:
 * 1 - if status is REMOVE remove_context will be called.
 * 2 - return codes of REMOVE are ignored (always success).
 *
 */
static apr_status_t  insert_update_contexts(mem_t *mem, char *str, int node, int vhost, int status)
{
    char *ptr = str;
    char *previous = str;
    apr_status_t ret = APR_SUCCESS;
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
            if (status != REMOVE) {
                ret = insert_update_context(mem, &info);
                if (ret != APR_SUCCESS)
                    return ret;
            } else
                remove_context(mem, &info);

            previous = ptr + 1;
        }
        ptr ++;
    }
    info.id = 0;
    strncpy(info.context, previous, sizeof(info.context));
    if (status != REMOVE)
        ret = insert_update_context(mem, &info); 
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
    int i;
    int size = loc_get_max_size_host();
    int *id;
    int sizecontext = loc_get_max_size_context();
    int *idcontext;


    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                "remove_host_context processing node: %d", node);
    if (size == 0)
        return;
    id = apr_palloc(r->pool, sizeof(int) * size);
    idcontext = apr_palloc(r->pool, sizeof(int) * sizecontext);
    size = get_ids_used_host(hoststatsmem, id);
    for (i=0; i<size; i++) {
        hostinfo_t *ou;

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
static char * process_config(request_rec *r, char **ptr, int *errtype)
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
    void *sconf = r->server->module_config;
    mod_manager_config *mconf = ap_get_module_config(sconf, &manager_module);

    vhost = apr_palloc(r->pool, sizeof(struct cluster_host));

    /* Map nothing by default */
    vhost->host = NULL;
    vhost->context = NULL;
    vhost->next = NULL;
    phost = vhost;

    /* Fill default nodes values */
    memset(&nodeinfo.mess, '\0', sizeof(nodeinfo.mess));
    if (mconf->balancername != NULL) {
        strcpy(nodeinfo.mess.balancer, mconf->balancername);
    } else {
        strcpy(nodeinfo.mess.balancer, "mycluster");
    }
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
    if (mconf->balancername != NULL) {
        strcpy(balancerinfo.balancer, mconf->balancername);
    } else {
        strcpy(balancerinfo.balancer, "mycluster");
    }
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
        /* Note that it is workerTimeout (set/getWorkerTimeout in java code) */ 
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
        /* We renamed it LBGroup */
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
            loc_remove_host_context(node->mess.id, r->pool);
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
        if (insert_update_hosts(hoststatsmem, phost->host, id, vid) != APR_SUCCESS)
            return MHOSTUI;
        if (insert_update_contexts(contextstatsmem, phost->context, id, vid, STOPPED) != APR_SUCCESS)
            return MCONTUI;
        phost = phost->next;
        vid++;
    }
    return NULL;
}
/*
 * Process a DUMP command.
 */
static char * process_dump(request_rec *r, int *errtype)
{
    int size, i;
    int *id;

    ap_set_content_type(r, "text/plain");

    size = loc_get_max_size_balancer();
    if (size == 0)
       return NULL;
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_balancer(balancerstatsmem, id);
    for (i=0; i<size; i++) {
        balancerinfo_t *ou;
        if (get_balancer(balancerstatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        ap_rprintf(r, "balancer: [%d] Name: %.*s Sticky: %d [%.*s]/[%.*s] remove: %d force: %d Timeout: %d maxAttempts: %d\n",
                   id[i], (int) sizeof(ou->balancer), ou->balancer, ou->StickySession,
                   (int) sizeof(ou->StickySessionCookie), ou->StickySessionCookie, (int) sizeof(ou->StickySessionPath), ou->StickySessionPath,
                   ou->StickySessionRemove, ou->StickySessionForce,
                   (int) apr_time_sec(ou->Timeout),
                   ou->Maxattempts);
    }

    size = loc_get_max_size_node();
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_node(nodestatsmem, id);
    for (i=0; i<size; i++) {
        nodeinfo_t *ou;
        if (get_node(nodestatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        ap_rprintf(r, "node: [%d:%d],Balancer: %.*s,JVMRoute: %.*s,LBGroup: [%.*s],Host: %.*s,Port: %.*s,Type: %.*s,flushpackets: %d,flushwait: %d,ping: %d,smax: %d,ttl: %d,timeout: %d\n",
                   id[i], ou->mess.id,
                   (int) sizeof(ou->mess.balancer), ou->mess.balancer,
                   (int) sizeof(ou->mess.JVMRoute), ou->mess.JVMRoute,
                   (int) sizeof(ou->mess.Domain), ou->mess.Domain,
                   (int) sizeof(ou->mess.Host), ou->mess.Host,
                   (int) sizeof(ou->mess.Port), ou->mess.Port,
                   (int) sizeof(ou->mess.Type), ou->mess.Type,
                   ou->mess.flushpackets, ou->mess.flushwait/1000, (int) apr_time_sec(ou->mess.ping), ou->mess.smax,
                   (int) apr_time_sec(ou->mess.ttl), (int) apr_time_sec(ou->mess.timeout));
    }

    size = loc_get_max_size_host();
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_host(hoststatsmem, id);
    for (i=0; i<size; i++) {
        hostinfo_t *ou;
        if (get_host(hoststatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        ap_rprintf(r, "host: %d [%.*s] vhost: %d node: %d\n", id[i], (int) sizeof(ou->host), ou->host, ou->vhost,
                  ou->node);
    }

    size = loc_get_max_size_context();
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_context(contextstatsmem, id);
    for (i=0; i<size; i++) {
        contextinfo_t *ou;
        if (get_context(contextstatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        ap_rprintf(r, "context: %d [%.*s] vhost: %d node: %d status: %d\n", id[i],
                   (int) sizeof(ou->context), ou->context,
                   ou->vhost, ou->node,
                   ou->status);
    }
    return NULL;
}
/*
 * Process a INFO command.
 * Statics informations ;-)
 */
static char * process_info(request_rec *r, int *errtype)
{
    int size, i;
    int *id;

    ap_set_content_type(r, "text/plain");

    size = loc_get_max_size_node();
    if (size == 0)
        return NULL;
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_node(nodestatsmem, id);
    for (i=0; i<size; i++) {
        nodeinfo_t *ou;
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
        proxy_worker_shared *proxystat;
#else
        proxy_worker_stat *proxystat;
#endif
        char *flushpackets;
        char *pptr;
        if (get_node(nodestatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        ap_rprintf(r, "Node: [%d],Name: %.*s,Balancer: %.*s,LBGroup: %.*s,Host: %.*s,Port: %.*s,Type: %.*s",
                   id[i],
                   (int) sizeof(ou->mess.JVMRoute), ou->mess.JVMRoute,
                   (int) sizeof(ou->mess.balancer), ou->mess.balancer,
                   (int) sizeof(ou->mess.Domain), ou->mess.Domain,
                   (int) sizeof(ou->mess.Host), ou->mess.Host,
                   (int) sizeof(ou->mess.Port), ou->mess.Port,
                   (int) sizeof(ou->mess.Type), ou->mess.Type);
        flushpackets = "Off";
        switch (ou->mess.flushpackets) {
            case flush_on:
                flushpackets = "On";
                break;
            case flush_auto:
                flushpackets = "Auto";
        }
        ap_rprintf(r, ",Flushpackets: %s,Flushwait: %d,Ping: %d,Smax: %d,Ttl: %d",
                   flushpackets, ou->mess.flushwait/1000,
                   (int) apr_time_sec(ou->mess.ping),
                   ou->mess.smax,
                   (int) apr_time_sec(ou->mess.ttl));
        pptr = (char *) ou;
        pptr = pptr + ou->offset;
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
        proxystat  = (proxy_worker_shared *) pptr;
#else
        proxystat  = (proxy_worker_stat *) pptr;
#endif
        ap_rprintf(r, ",Elected: %d,Read: %d,Transfered: %d,Connected: %d,Load: %d\n",
                   (int) proxystat->elected, (int) proxystat->read, (int) proxystat->transferred,
                   (int) proxystat->busy, proxystat->lbfactor);
        
    }

    /* Process the Vhosts */
    size = loc_get_max_size_host();
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_host(hoststatsmem, id);
    for (i=0; i<size; i++) {
        hostinfo_t *ou;
        if (get_host(hoststatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        ap_rprintf(r, "Vhost: [%d:%d:%d], Alias: %.*s\n",
                   ou->node, ou->vhost, id[i], (int ) sizeof(ou->host), ou->host);
    }

    /* Process the Contexts */
    size = loc_get_max_size_context();
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
        ap_rprintf(r, "Context: [%d:%d:%d], Context: %.*s, Status: %s\n",
                   ou->node, ou->vhost, id[i],
                   (int) sizeof(ou->context), ou->context,
                   status);
    }
    return NULL;
}

/* Process a *-APP command that applies to the node */
static char * process_node_cmd(request_rec *r, int status, int *errtype, nodeinfo_t *node)
{
    /* for read the hosts */
    int i,j;
    int size = loc_get_max_size_host();
    int *id;

    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                "process_node_cmd %d processing node: %d", status, node->mess.id);
    if (size == 0)
        return NULL;
    id = apr_palloc(r->pool, sizeof(int) * size);
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
static char * process_appl_cmd(request_rec *r, char **ptr, int status, int *errtype, int global)
{
    nodeinfo_t nodeinfo;
    nodeinfo_t *node;
    struct cluster_host *vhost;

    int i = 0;
    hostinfo_t hostinfo;
    hostinfo_t *host;
    
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
    if (global) {
        return (process_node_cmd(r, status, errtype, node));
    }

    /* Read the ID of the virtual host corresponding to the first Alias */
    hostinfo.node = node->mess.id;
    if (vhost->host != NULL) {
        char *s = hostinfo.host;
        int j = 1;
        strncpy(hostinfo.host, vhost->host, sizeof(hostinfo.host));
        while (*s != ',' && j<sizeof(hostinfo.host)) {
           j++;
           s++;
        }
        *s = '\0';
    } else
        hostinfo.host[0] = '\0';

    hostinfo.id = 0;
    host = read_host(hoststatsmem, &hostinfo);
    if (host == NULL) {
        int vid = 1; /* XXX: That is not really the right value, but that works most time */
        /* If REMOVE ignores it */
        if (status == REMOVE)
            return NULL;
        /* If the Host doesn't exist yet create it */
        if (insert_update_hosts(hoststatsmem, vhost->host, node->mess.id, vid) != APR_SUCCESS) {
            *errtype = TYPEMEM;
            return MHOSTUI; 
        }
        hostinfo.id = 0;
        hostinfo.node = node->mess.id;
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
    if (insert_update_contexts(contextstatsmem, vhost->context, node->mess.id, host->vhost, status) != APR_SUCCESS) {
        *errtype = TYPEMEM;
        return MCONTUI; 
    }

    /* Remove the host if all the contextes have been removed */
    if (status == REMOVE) {
        int size = loc_get_max_size_context();
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
    } else if (status == STOPPED) {
        /* insert_update_contexts in fact makes that vhost->context corresponds only to the first context... */
        contextinfo_t in;
        contextinfo_t *ou;
        in.id = 0;
        strncpy(in.context, vhost->context, sizeof(in.context));
        in.vhost = host->vhost;
        in.node = node->mess.id;
        ou = read_context(contextstatsmem, &in);
        if (ou != NULL) {
            ap_set_content_type(r, "text/plain");
            ap_rprintf(r, "Type=STOP-APP-RSP&JvmRoute=%.*s&Alias=%.*s&Context=%.*s&Requests=%d",
                       (int) sizeof(nodeinfo.mess.JVMRoute), nodeinfo.mess.JVMRoute,
                       (int) sizeof(vhost->host), vhost->host,
                       (int) sizeof(vhost->context), vhost->context,
                       ou->nbrequests);
            ap_rprintf(r, "\n");
        }
    }
    return NULL;
}
static char * process_enable(request_rec *r, char **ptr, int *errtype, int global)
{
    return process_appl_cmd(r, ptr, ENABLED, errtype, global);
}
static char * process_disable(request_rec *r, char **ptr, int *errtype, int global)
{
    return process_appl_cmd(r, ptr, DISABLED, errtype, global);
}
static char * process_stop(request_rec *r, char **ptr, int *errtype, int global)
{
    return process_appl_cmd(r, ptr, STOPPED, errtype, global);
}
static char * process_remove(request_rec *r, char **ptr, int *errtype, int global)
{
    return process_appl_cmd(r, ptr, REMOVE, errtype, global);
}

/*
 * JGroups feature routines
 */
static char * process_addid(request_rec *r, char **ptr, int *errtype)
{
    jgroupsidinfo_t jgroupsid;
    int i = 0;
    jgroupsid.jgroupsid[0] = '\0';
    while (ptr[i]) {
        if (strcasecmp(ptr[i], "JGroupUuid") == 0) {
            if (strlen(ptr[i+1])>=sizeof(jgroupsid.jgroupsid)) {
                 *errtype = TYPESYNTAX;
                 return SJIDBIG;
            }
            strcpy(jgroupsid.jgroupsid, ptr[i+1]);
        }
        if (strcasecmp(ptr[i], "JGroupData") == 0) {
            if (strlen(ptr[i+1])>=sizeof(jgroupsid.data)) {
                 *errtype = TYPESYNTAX;
                 return SJDDBIG;
            }
            strcpy(jgroupsid.data, ptr[i+1]);
        }
        i++;
        i++;
    }
    if (jgroupsid.jgroupsid[0] == '\0') {
        *errtype = TYPESYNTAX;
        return SJIDBAD;
    }
    if (insert_update_jgroupsid(jgroupsidstatsmem, &jgroupsid) != APR_SUCCESS) {
        *errtype = TYPEMEM;
        return MJBIDUI;
    }

    return NULL;
}
static char * process_removeid(request_rec *r, char **ptr, int *errtype)
{
    jgroupsidinfo_t jgroupsid;
    int i = 0;
    jgroupsid.jgroupsid[0] = '\0';
    while (ptr[i]) {
        if (strcasecmp(ptr[i], "JGroupUuid") == 0) {
            if (strlen(ptr[i+1])>=sizeof(jgroupsid.jgroupsid)) {
                 *errtype = TYPESYNTAX;
                 return SJIDBIG;
            }
            strcpy(jgroupsid.jgroupsid, ptr[i+1]);
        }
        i++;
        i++;
    }
    if (jgroupsid.jgroupsid[0] == '\0') {
        *errtype = TYPESYNTAX;
        return SJIDBAD;
    }
    remove_jgroupsid(jgroupsidstatsmem, &jgroupsid);
    return NULL;
}
/*
 * Query out should be something like:
 * JGroup: [9],JGroupUuid: node4, JGroupData: jgroupdata4
 * JGroup: [11],JGroupUuid: node6, JGroupData: jgroupdata6
 */
static void print_jgroupsid(request_rec *r, int id, jgroupsidinfo_t *jgroupsid)
{
    ap_rprintf(r, "JGroup: [%d],JGroupUuid: %.*s,JGroupData: %.*s\n",
               id,
               (int) sizeof(jgroupsid->jgroupsid), jgroupsid->jgroupsid,
               (int) sizeof(jgroupsid->data), jgroupsid->data);
}
static char * process_query(request_rec *r, char **ptr, int *errtype)
{
    jgroupsidinfo_t jgroupsid;
    int i = 0;
    jgroupsid.jgroupsid[0] = '\0';
    while (ptr[i]) {
        if (strcasecmp(ptr[i], "JGroupUuid") == 0) {
            if (strlen(ptr[i+1])>=sizeof(jgroupsid.jgroupsid)) {
                 *errtype = TYPESYNTAX;
                 return SJIDBIG;
            }
            strcpy(jgroupsid.jgroupsid, ptr[i+1]);
        }
        i++;
        i++;
    }
    if (jgroupsid.jgroupsid[0] == '\0') {
        jgroupsid.jgroupsid[0] = '*';
        jgroupsid.jgroupsid[1] = '\0';
    }
    if (strcmp(jgroupsid.jgroupsid, "*") == 0) {
        int size, i;
        int *id;
        size = loc_get_max_size_jgroupsid();
        if (size == 0)
            return NULL;
        id = apr_palloc(r->pool, sizeof(int) * size);
        size = get_ids_used_jgroupsid(jgroupsidstatsmem, id);
        for (i=0; i<size; i++) {
             jgroupsidinfo_t *ou;
             if (get_jgroupsid(jgroupsidstatsmem, &ou, id[i]) != APR_SUCCESS)
                 continue;
             print_jgroupsid(r, id[i], ou);
        }
    } else {
        jgroupsidinfo_t *ou;
        ou = read_jgroupsid(jgroupsidstatsmem, &jgroupsid);
        if (ou == NULL) {
           *errtype = TYPEMEM;
           return MJBIDRD;
        } else
            print_jgroupsid(r, ou->id, ou);
    }
    return NULL;
}

/*
 * Call the ping/pong logic
 * Do a ping/png request to the node and set the load factor.
 */
static int isnode_up(request_rec *r, int id, int Load)
{
    if (balancerhandler != NULL) {
        return (balancerhandler->proxy_node_isup(r, id, Load));
    }
    return OK;
}
/*
 * Call the ping/pong logic using scheme://host:port
 * Do a ping/png request to the node and set the load factor.
 */
static int ishost_up(request_rec *r, char *scheme, char *host, char *port)
{
    if (balancerhandler != NULL) {
        return (balancerhandler->proxy_host_isup(r, scheme, host, port));
    }
    return OK;
}
/*
 * Process the STATUS command
 * Load -1 : Broken
 * Load 0  : Standby.
 * Load 1-100 : Load factor.
 */
static char * process_status(request_rec *r, char **ptr, int *errtype)
{
    int Load = -1;
    nodeinfo_t nodeinfo;
    nodeinfo_t *node;

    int i = 0;

    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server, "Processing STATUS");
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
    ap_rprintf(r, "Type=STATUS-RSP&JVMRoute=%.*s", (int) sizeof(nodeinfo.mess.JVMRoute), nodeinfo.mess.JVMRoute);

    if (isnode_up(r, node->mess.id, Load) != OK)
        ap_rprintf(r, "&State=NOTOK");
    else
        ap_rprintf(r, "&State=OK");
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
    ap_rprintf(r, "&id=%d", (int) ap_scoreboard_image->global->restart_time);
#else
    if (ap_my_generation)
        ap_rprintf(r, "&id=%d", ap_my_generation);
    else
        ap_rprintf(r, "&id=%d", (int) ap_scoreboard_image->global->restart_time);
#endif

    ap_rprintf(r, "\n");
    return NULL;
}

/*
 * Process the PING command
 * With a JVMRoute does a cping/cpong in the node.
 * Without just answers ok.
 * NOTE: It is hard to cping/cpong a host + port but CONFIG + PING + REMOVE_APP *
 *       would do the same.
 */
static char * process_ping(request_rec *r, char **ptr, int *errtype)
{
    nodeinfo_t nodeinfo;
    nodeinfo_t *node;
    char *scheme = NULL;
    char *host = NULL;
    char *port = NULL;

    int i = 0;

    ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server, "Processing PING");
    nodeinfo.mess.id = -1;
    while (ptr[i] && ptr[i][0] != '\0') {
        if (strcasecmp(ptr[i], "JVMRoute") == 0) {
            if (strlen(ptr[i+1])>=sizeof(nodeinfo.mess.JVMRoute)) {
                *errtype = TYPESYNTAX;
                return SROUBIG;
            }
            strcpy(nodeinfo.mess.JVMRoute, ptr[i+1]);
            nodeinfo.mess.id = 0;
        }
        else if (strcasecmp(ptr[i], "Scheme") == 0)
            scheme = apr_pstrdup(r->pool, ptr[i+1]);
        else if (strcasecmp(ptr[i], "Host") == 0)
            host = apr_pstrdup(r->pool, ptr[i+1]);
        else if (strcasecmp(ptr[i], "Port") == 0)
            port = apr_pstrdup(r->pool, ptr[i+1]);
        else {
            *errtype = TYPESYNTAX;
            return apr_psprintf(r->pool, SBADFLD, ptr[i]);
        }
        i++;
        i++;
    }
    if (nodeinfo.mess.id == -1) {
        /* PING scheme, host, port or just httpd */
        if (scheme == NULL && host == NULL && port == NULL) {
            ap_set_content_type(r, "text/plain");
            ap_rprintf(r, "Type=PING-RSP&State=OK");
        }  else {
            if (scheme == NULL || host == NULL || port == NULL) {
                *errtype = TYPESYNTAX;
                return apr_psprintf(r->pool, SMISFLD);
            }
            ap_set_content_type(r, "text/plain");
            ap_rprintf(r, "Type=PING-RSP");

            if (ishost_up(r, scheme, host, port) != OK)
                ap_rprintf(r, "&State=NOTOK");
            else
                ap_rprintf(r, "&State=OK");
        }
    } else {

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
        ap_rprintf(r, "Type=PING-RSP&JVMRoute=%.*s", (int) sizeof(nodeinfo.mess.JVMRoute), nodeinfo.mess.JVMRoute);

        if (isnode_up(r, node->mess.id, -2) != OK)
            ap_rprintf(r, "&State=NOTOK");
        else
            ap_rprintf(r, "&State=OK");
    }
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
    ap_rprintf(r, "&id=%d", (int) ap_scoreboard_image->global->restart_time);
#else
    if (ap_my_generation)
        ap_rprintf(r, "&id=%d", ap_my_generation);
    else
        ap_rprintf(r, "&id=%d", (int) ap_scoreboard_image->global->restart_time);
#endif

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

/* Check that the method is one of ours */
static int check_method(request_rec *r)
{
    int ours = 0;
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
    else if (strcasecmp(r->method, "PING") == 0)
        ours = 1;
    else if (strcasecmp(r->method, "ADDID") == 0)
        ours = 1;
    else if (strcasecmp(r->method, "REMOVEID") == 0)
        ours = 1;
    else if (strcasecmp(r->method, "QUERY") == 0)
        ours = 1;
    return ours;
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
    mod_manager_config *mconf = ap_get_module_config(r->server->module_config,
                                                     &manager_module);
 
    if (conf && conf->handler && r->method_number == M_GET &&
        strcmp(conf->handler, "mod_cluster-manager") == 0) {
        r->handler = "mod_cluster-manager";
        r->filename = apr_pstrdup(r->pool, r->uri);
        return OK;
    }
    if (r->method_number != M_INVALID)
        return DECLINED;
    if (!mconf->enable_mcpm_receive)
        return DECLINED; /* Not allowed to receive MCMP */

    ours = check_method(r); 
    if (ours) {
        int i;
        /* The method one of ours */
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                    "manager_trans %s (%s)", r->method, r->uri);
        r->handler = "mod-cluster"; /* that hack doesn't work on httpd-2.4.x */
        i = strlen(r->uri);
        if (strcmp(r->uri, "*") == 0 || (i>=2 && r->uri[i-1] == '*' && r->uri[i-2] == '/')) {
            r->filename = apr_pstrdup(r->pool, NODE_COMMAND);
        } else {
            r->filename = apr_pstrdup(r->pool, r->uri);
        }
        return OK;
    }
    
    return DECLINED;
}

/* Create the commands that are possible on the context */
static char*context_string(request_rec *r, contextinfo_t *ou, char *Alias, char *JVMRoute)
{
    char context[sizeof(ou->context)+1];
    char *raw;
    context[sizeof(ou->context)] = '\0';
    strncpy(context, ou->context, sizeof(ou->context));
    raw = apr_pstrcat(r->pool, "JVMRoute=", JVMRoute, "&Alias=", Alias, "&Context=", context, NULL);
    return raw;
}
static char *balancer_nonce_string(request_rec *r)
{
    char *ret = "";
    void *sconf = r->server->module_config;
    mod_manager_config *mconf = ap_get_module_config(sconf, &manager_module);
    if (mconf->nonce)
        ret = apr_psprintf(r->pool, "nonce=%s&", balancer_nonce);
    return ret;
}
static void context_command_string(request_rec *r, contextinfo_t *ou, char *Alias, char *JVMRoute)
{
    if (ou->status == DISABLED)
        ap_rprintf(r, "<a href=\"%s?%sCmd=ENABLE-APP&Range=CONTEXT&%s\">Enable</a> ",
                   r->uri, balancer_nonce_string(r), context_string(r, ou, Alias, JVMRoute));
    if (ou->status == ENABLED)
        ap_rprintf(r, "<a href=\"%s?%sCmd=DISABLE-APP&Range=CONTEXT&%s\">Disable</a>",
                   r->uri, balancer_nonce_string(r), context_string(r, ou, Alias, JVMRoute));
}
/* Create the commands that are possible on the node */
static char*node_string(request_rec *r, char *JVMRoute)
{
    char *raw = apr_pstrcat(r->pool, "JVMRoute=", JVMRoute, NULL);
    return raw;
}
static void node_command_string(request_rec *r, int status, char *JVMRoute)
{
    if (status == ENABLED)
        ap_rprintf(r, "<a href=\"%s?%sCmd=ENABLE-APP&Range=NODE&%s\">Enable Contexts</a> ",
                   r->uri, balancer_nonce_string(r), node_string(r, JVMRoute));
    if (status == DISABLED)
        ap_rprintf(r, "<a href=\"%s?%sCmd=DISABLE-APP&Range=NODE&%s\">Disable Contexts</a>",
                   r->uri, balancer_nonce_string(r), node_string(r, JVMRoute));
}
static void domain_command_string(request_rec *r, int status, char *Domain)
{
    if (status == ENABLED)
        ap_rprintf(r, "<a href=\"%s?%sCmd=ENABLE-APP&Range=DOMAIN&Domain=%s\">Enable Nodes</a> ",
                   r->uri, balancer_nonce_string(r), Domain);
    if (status == DISABLED)
        ap_rprintf(r, "<a href=\"%s?%sCmd=DISABLE-APP&Range=DOMAIN&Domain=%s\">Disable Nodes</a>",
                   r->uri, balancer_nonce_string(r), Domain);
}

/*
 * Process the parameters and display corresponding informations.
 */
static void manager_info_contexts(request_rec *r, int reduce_display, int allow_cmd, int node, int host, char *Alias, char *JVMRoute)
{
    int size, i;
    int *id;
    /* Process the Contexts */
    if (!reduce_display)
        ap_rprintf(r, "<h3>Contexts:</h3>");
    ap_rprintf(r, "<pre>");
    size = loc_get_max_size_context();
    if (size == 0)
        return;
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
        ap_rprintf(r, "%.*s, Status: %s Request: %d ", (int) sizeof(ou->context), ou->context, status, ou->nbrequests);
        if (allow_cmd)
            context_command_string(r, ou, Alias, JVMRoute);
        ap_rprintf(r, "\n");
    }
    ap_rprintf(r, "</pre>");
}
static void manager_info_hosts(request_rec *r, int reduce_display, int allow_cmd, int node, char *JVMRoute)
{
    int size, i;
    int *id;
    int vhost = 0;

    /* Process the Vhosts */
    size = loc_get_max_size_host();
    if (size == 0)
        return;
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_host(hoststatsmem, id);
    for (i=0; i<size; i++) {
        hostinfo_t *ou;
        if (get_host(hoststatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        if (ou->node != node)
            continue;
        if (ou->vhost != vhost) {
            if (vhost && !reduce_display)
                ap_rprintf(r, "</pre>");
            if (!reduce_display)
                ap_rprintf(r, "<h2> Virtual Host %d:</h2>", ou->vhost);
            manager_info_contexts(r, reduce_display, allow_cmd, ou->node, ou->vhost, ou->host, JVMRoute);
            if (reduce_display)
                ap_rprintf(r, "Aliases: ");
            else {
                ap_rprintf(r, "<h3>Aliases:</h3>");
                ap_rprintf(r, "<pre>");
            }
            vhost = ou->vhost;
        }
        if (reduce_display)
            ap_rprintf(r, "%.*s ", (int) sizeof(ou->host), ou->host);
        else
            ap_rprintf(r, "%.*s\n", (int) sizeof(ou->host), ou->host);
    }
    if (size && !reduce_display)
        ap_rprintf(r, "</pre>");

}
static void manager_sessionid(request_rec *r)
{
    int size, i;
    int *id;

    /* Process the Sessionids */
    size = loc_get_max_size_sessionid();
    if (size == 0)
        return;
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_sessionid(sessionidstatsmem, id);
    if (!size)
        return;
    ap_rprintf(r, "<h1>SessionIDs:</h1>");
    ap_rprintf(r, "<pre>");
    for (i=0; i<size; i++) {
        sessionidinfo_t *ou;
        if (get_sessionid(sessionidstatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        ap_rprintf(r, "id: %.*s route: %.*s\n", (int) sizeof(ou->sessionid), ou->sessionid, (int) sizeof(ou->JVMRoute), ou->JVMRoute);
    }
    ap_rprintf(r, "</pre>");

}

#if HAVE_CLUSTER_EX_DEBUG
static void manager_domain(request_rec *r, int reduce_display)
{
    int size, i;
    int *id;

    /* Process the domain information: the removed node belonging to a domain are stored there */
    if (reduce_display)
        ap_rprintf(r, "<br/>LBGroup:");
    else
        ap_rprintf(r, "<h1>LBGroup:</h1>");
    ap_rprintf(r, "<pre>");
    size = loc_get_max_size_domain();
    if (size == 0)
        return;
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_domain(domainstatsmem, id);
    for (i=0; i<size; i++) {
        domaininfo_t *ou;
        if (get_domain(domainstatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        ap_rprintf(r, "dom: %.*s route: %.*s balancer: %.*s\n",
                   sizeof(ou->domain), ou->domain,
                   sizeof(ou->JVMRoute), ou->JVMRoute,
                   sizeof(ou->balancer), ou->balancer);
    }
    ap_rprintf(r, "</pre>");

}
#endif

static int count_sessionid(request_rec *r, char *route)
{
    int size, i;
    int *id;
    int count = 0;

    /* Count the sessionid corresponding to the route */
    size = loc_get_max_size_sessionid();
    if (size == 0)
        return 0;
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
static void process_error(request_rec *r, char *errstring, int errtype)
{
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
    ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_WARNING, 0, r->server,
            "manager_handler %s error: %s", r->method, errstring);
}
static void sort_nodes(nodeinfo_t *nodes, int nbnodes)
{
    int i;
    int changed = -1;
    if (nbnodes <=1)
        return;
    while(changed) {
        changed = 0;
        for (i=0; i<nbnodes-1; i++) {
            if (strcmp(nodes[i].mess.Domain, nodes[i+1].mess.Domain)> 0) {
                nodeinfo_t node;
                node = nodes[i+1];
                nodes[i+1] = nodes[i];
                nodes[i] = node;
                changed = -1;
            }
        }
    }
}
static char *process_domain(request_rec *r, char **ptr, int *errtype, const char *cmd, const char *domain)
{
    int size, i;
    int *id;
    int pos;
    char *errstring = NULL;
    size = loc_get_max_size_node();
    if (size == 0)
        return NULL;
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_node(nodestatsmem, id);

    for (pos=0;ptr[pos]!=NULL && ptr[pos+1]!=NULL; pos=pos+2) ;

    ptr[pos] = apr_pstrdup(r->pool, "JVMRoute");
    ptr[pos+2] = NULL;
    ptr[pos+3] = NULL;
    ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, r->server, "process_domain");
    for (i=0; i<size; i++) {
        nodeinfo_t *ou;
        if (get_node(nodestatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        if (strcmp(ou->mess.Domain, domain) != 0)
            continue;
        /* add the JVMRoute */
        ptr[pos+1] = apr_pstrdup(r->pool, ou->mess.JVMRoute);
        if (strcasecmp(cmd, "ENABLE-APP") == 0)
            errstring = process_enable(r, ptr, errtype, RANGENODE);
        else if (strcasecmp(cmd, "DISABLE-APP") == 0)
            errstring = process_disable(r, ptr, errtype, RANGENODE);
        else if (strcasecmp(cmd, "STOP-APP") == 0)
            errstring = process_stop(r, ptr, errtype, RANGENODE);
        else if (strcasecmp(cmd, "REMOVE-APP") == 0)
            errstring = process_remove(r, ptr, errtype, RANGENODE);
    }
    return errstring;
}
/* XXX: move to mod_proxy_cluster as a provider ? */
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
static void printproxy_stat(request_rec *r, int reduce_display, proxy_worker_shared *proxystat)
#else
static void printproxy_stat(request_rec *r, int reduce_display, proxy_worker_stat *proxystat)
#endif
{
    char *status = NULL;
    if (proxystat->status & PROXY_WORKER_NOT_USABLE_BITMAP)
        status = "NOTOK";
    else
        status = "OK";
    if (reduce_display)
        ap_rprintf(r, " %s ", status);
    else
        ap_rprintf(r, ",Status: %s,Elected: %d,Read: %d,Transferred: %d,Connected: %d,Load: %d",
               status,
               (int) proxystat->elected, (int) proxystat->read, (int) proxystat->transferred,
               (int) proxystat->busy, proxystat->lbfactor);
}
/* Display module information */
static void modules_info(request_rec *r)
{
    if (ap_find_linked_module("mod_proxy_cluster.c") != NULL)
        ap_rputs("mod_proxy_cluster.c: OK<br/>", r);
    else
        ap_rputs("mod_proxy_cluster.c: missing<br/>", r);

    if (ap_find_linked_module("mod_sharedmem.c") != NULL)
        ap_rputs("mod_sharedmem.c: OK<br/>", r);
    else
        ap_rputs("mod_sharedmem.c: missing<br/>", r);

    ap_rputs("Protocol supported: ", r);
    if (ap_find_linked_module("mod_proxy_http.c") != NULL)
        ap_rputs("http ", r);
    if (ap_find_linked_module("mod_proxy_ajp.c") != NULL) 
        ap_rputs("AJP ", r);
    if (ap_find_linked_module("mod_ssl.c") != NULL) 
        ap_rputs("https", r);
    ap_rputs("<br/>", r);

    if (ap_find_linked_module("mod_advertise.c") != NULL)
        ap_rputs("mod_advertise.c: OK<br/>", r);
    else
        ap_rputs("mod_advertise.c: not loaded<br/>", r);

}
/* Process INFO message and mod_cluster_manager pages generation */
static int manager_info(request_rec *r)
{
    int size, i, sizesessionid;
    int *id;
    apr_table_t *params = apr_table_make(r->pool, 10);
    int access_status;
    const char *name;
    nodeinfo_t *nodes;
    int nbnodes = 0;
    char *domain = "";
    char *errstring = NULL;
    void *sconf = r->server->module_config;
    mod_manager_config *mconf = ap_get_module_config(sconf, &manager_module);

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
                    if (strcmp(args, "Context") || (access_status !=  HTTP_NOT_FOUND))
                        return access_status;
                apr_table_setn(params, args, val);
                args = tok;
            }
            else
                return HTTP_BAD_REQUEST;
        }
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
                "manager_info request:%s", r->args);
    }

    /*
     * Check that the supplied nonce matches this server's nonce;
     * otherwise ignore all parameters, to prevent a CSRF attack.
     */
    if (mconf->nonce && ((name = apr_table_get(params, "nonce")) == NULL
        || strcmp(balancer_nonce, name) != 0)) {
        apr_table_clear(params);
    }

    /* process the parameters */
    if (r->args) {
        const char *val = apr_table_get(params, "Refresh");
        const char *cmd = apr_table_get(params, "Cmd");
        const char *typ = apr_table_get(params, "Range");
        const char *domain = apr_table_get(params, "Domain");
        /* Process the Refresh parameter */
        if (val) {
            long t = atol(val);
            apr_table_set(r->headers_out, "Refresh", apr_ltoa(r->pool,t < 1 ? 10 : t));
        }
        /* Process INFO and DUMP */
        if (cmd != NULL) {
            int errtype = 0;
            if (strcasecmp(cmd, "DUMP") == 0) {
                errstring = process_dump(r, &errtype);
                if (!errstring)
                    return OK;
            } else if (strcasecmp(cmd, "INFO") == 0) {
                errstring = process_info(r, &errtype);
                if (!errstring)
                    return OK;
            }
            if (errstring) {
                process_error(r, errstring, errtype);
            }
        }
        /* Process other command if any */
        if (cmd != NULL && typ !=NULL && mconf->allow_cmd && errstring == NULL) {
            int global = RANGECONTEXT;
            int errtype = 0;
            int i;
            char **ptr;
            const apr_array_header_t *arr = apr_table_elts(params);
            const apr_table_entry_t *elts = (const apr_table_entry_t *)arr->elts;

            if (strcasecmp(typ,"NODE")==0)
                global = RANGENODE;
            else if (strcasecmp(typ,"DOMAIN")==0)
                global = RANGEDOMAIN;

            if (global == RANGEDOMAIN)
                ptr = apr_palloc(r->pool, sizeof(char *) * (arr->nelts + 2) * 2);
            else
                ptr = apr_palloc(r->pool, sizeof(char *) * (arr->nelts + 1) * 2);
            for (i = 0; i < arr->nelts; i++) {
                ptr[i*2] = elts[i].key;
                ptr[i*2+1] = elts[i].val;
            }
            ptr[arr->nelts*2] = NULL;
            ptr[arr->nelts*2+1] = NULL;
             
            if (global == RANGEDOMAIN)
                errstring = process_domain(r, ptr, &errtype, cmd, domain);
            else if (strcasecmp(cmd, "ENABLE-APP") == 0)
                errstring = process_enable(r, ptr, &errtype, global);
            else if (strcasecmp(cmd, "DISABLE-APP") == 0)
                errstring = process_disable(r, ptr, &errtype, global);
            else if (strcasecmp(cmd, "STOP-APP") == 0)
                errstring = process_stop(r, ptr, &errtype, global);
            else if (strcasecmp(cmd, "REMOVE-APP") == 0)
                errstring = process_remove(r, ptr, &errtype, global);
            else {
                errstring = SCMDUNS;
                errtype = TYPESYNTAX;
            }
            if (errstring) {
                process_error(r, errstring, errtype);
            }
        }
    }
    
    ap_set_content_type(r, "text/html; charset=ISO-8859-1");
    ap_rputs(DOCTYPE_HTML_3_2
             "<html><head>\n<title>Mod_cluster Status</title>\n</head><body>\n",
             r);
    ap_rvputs(r, "<h1>", MOD_CLUSTER_EXPOSED_VERSION, "</h1>", NULL);

    if (errstring) {
        ap_rvputs(r, "<h1> Command failed: ", errstring , "</h1>\n", NULL);
        ap_rvputs(r, " <a href=\"", r->uri, "\">Continue</a>\n", NULL);
        ap_rputs("</body></html>\n", r);
        return OK;
    }

    /* Advertise information */
    if (mconf->allow_display) {
        ap_rputs("start of \"httpd.conf\" configuration<br/>", r); 
        modules_info(r);
        if (advertise_info != NULL)
            advertise_info(r);
        ap_rputs("end of \"httpd.conf\" configuration<br/><br/>", r);
    }

    ap_rvputs(r, "<a href=\"", r->uri, "?", balancer_nonce_string(r),
                 "refresh=10",
                 "\">Auto Refresh</a>", NULL);

    ap_rvputs(r, " <a href=\"", r->uri, "?", balancer_nonce_string(r),
                 "Cmd=DUMP&Range=ALL",
                 "\">show DUMP output</a>", NULL);

    ap_rvputs(r, " <a href=\"", r->uri, "?", balancer_nonce_string(r),
                 "Cmd=INFO&Range=ALL",
                 "\">show INFO output</a>", NULL);

    ap_rputs("\n", r);

    sizesessionid = loc_get_max_size_sessionid();

    size = loc_get_max_size_node();
    if (size == 0)
        return OK;
    id = apr_palloc(r->pool, sizeof(int) * size);
    size = get_ids_used_node(nodestatsmem, id);


    /* read the node to sort them by domain */
    nodes = apr_palloc(r->pool, sizeof(nodeinfo_t) * size);
    for (i=0; i<size; i++) {
        nodeinfo_t *ou;
        if (get_node(nodestatsmem, &ou, id[i]) != APR_SUCCESS)
            continue;
        memcpy(&nodes[nbnodes],ou, sizeof(nodeinfo_t));
        nbnodes++;
    }
    sort_nodes(nodes, nbnodes);

    /* display the ordered nodes */
    for (i=0; i<size; i++) {
        char *flushpackets;
        nodeinfo_t *ou = &nodes[i];
        char *pptr = (char *) ou;

        if (strcmp(domain, ou->mess.Domain) != 0) {
            if (mconf->reduce_display)
                ap_rprintf(r, "<br/><br/>LBGroup %.*s: ", (int) sizeof(ou->mess.Domain), ou->mess.Domain);
            else
                ap_rprintf(r, "<h1> LBGroup %.*s: ", (int) sizeof(ou->mess.Domain), ou->mess.Domain);
            domain = ou->mess.Domain;
            if (mconf->allow_cmd)
                domain_command_string(r, ENABLED, domain);
            if (mconf->allow_cmd)
                domain_command_string(r, DISABLED, domain);
            if (!mconf->reduce_display)
                ap_rprintf(r, "</h1>\n");
        }
        if (mconf->reduce_display)
            ap_rprintf(r, "<br/><br/>Node %.*s ",
                   (int) sizeof(ou->mess.JVMRoute), ou->mess.JVMRoute);
        else 
            ap_rprintf(r, "<h1> Node %.*s (%.*s://%.*s:%.*s): </h1>\n",
                   (int) sizeof(ou->mess.JVMRoute), ou->mess.JVMRoute,
                   (int) sizeof(ou->mess.Type), ou->mess.Type,
                   (int) sizeof(ou->mess.Host), ou->mess.Host,
                   (int) sizeof(ou->mess.Port), ou->mess.Port);
        pptr = pptr + ou->offset;
        if (mconf->reduce_display) {
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
            printproxy_stat(r, mconf->reduce_display, (proxy_worker_shared *) pptr);
#else
            printproxy_stat(r, mconf->reduce_display, (proxy_worker_stat *) pptr);
#endif
        }

        if (mconf->allow_cmd)
            node_command_string(r, ENABLED, ou->mess.JVMRoute);
        if (mconf->allow_cmd)
            node_command_string(r, DISABLED, ou->mess.JVMRoute);

        if (!mconf->reduce_display) {
            ap_rprintf(r, "<br/>\n");
            ap_rprintf(r, "Balancer: %.*s,LBGroup: %.*s", (int) sizeof(ou->mess.balancer), ou->mess.balancer,
                   (int) sizeof(ou->mess.Domain), ou->mess.Domain);

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
                   (int) ou->mess.ping, ou->mess.smax, (int) ou->mess.ttl);
        }

        if (mconf->reduce_display)
            ap_rprintf(r, "<br/>\n");
        else {
#if AP_MODULE_MAGIC_AT_LEAST(20101223,1)
            printproxy_stat(r, mconf->reduce_display, (proxy_worker_shared *) pptr);
#else
            printproxy_stat(r, mconf->reduce_display, (proxy_worker_stat *) pptr);
#endif
        }

        if (sizesessionid) {
            ap_rprintf(r, ",Num sessions: %d",  count_sessionid(r, ou->mess.JVMRoute));
        }
        ap_rprintf(r, "\n");

        /* Process the Vhosts */
        manager_info_hosts(r, mconf->reduce_display, mconf->allow_cmd, ou->mess.id, ou->mess.JVMRoute); 
    }
    /* Display the sessions */
    if (sizesessionid)
        manager_sessionid(r);
#if HAVE_CLUSTER_EX_DEBUG
    manager_domain(r, mconf->reduce_display);
#endif


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
    apr_size_t bufsiz;
    apr_status_t status;
    int global = 0;
    int ours = 0;
    char **ptr;
    void *sconf = r->server->module_config;
    mod_manager_config *mconf;
  
    if (strcmp(r->handler, "mod_cluster-manager") == 0) {
        /* Display the nodes information */
        if (r->method_number != M_GET)
            return DECLINED;
        return(manager_info(r));
    }

    mconf = ap_get_module_config(sconf, &manager_module);
    if (!mconf->enable_mcpm_receive)
        return DECLINED; /* Not allowed to receive MCMP */

    ours = check_method(r);
    if (!ours)
        return DECLINED;

    /* Use a buffer to read the message */
    if (mconf->maxmesssize)
       bufsiz = mconf->maxmesssize;
    else {
       /* we calculate it */
       bufsiz = 9 + JVMROUTESZ;
       bufsiz = bufsiz + (mconf->maxhost * HOSTALIASZ) + 7;
       bufsiz = bufsiz + (mconf->maxcontext * CONTEXTSZ) + 8;
    }
    if (bufsiz< MAXMESSSIZE)
       bufsiz = MAXMESSSIZE;
    buff = apr_pcalloc(r->pool, bufsiz);
    input_brigade = apr_brigade_create(r->pool, r->connection->bucket_alloc);
    status = ap_get_brigade(r->input_filters, input_brigade, AP_MODE_READBYTES, APR_BLOCK_READ, bufsiz);
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
                "manager_handler %s (%s) processing: \"%s\"", r->method, r->filename, buff);

    decodeenc(buff);
    ptr = process_buff(r, buff);
    if (ptr == NULL) {
        process_error(r, SMESPAR, TYPESYNTAX);
        return 500;
    }
    if (strstr(r->filename, NODE_COMMAND))
        global = 1;

    if (strcasecmp(r->method, "CONFIG") == 0)
        errstring = process_config(r, ptr, &errtype);
    /* Application handling */
    else if (strcasecmp(r->method, "ENABLE-APP") == 0)
        errstring = process_enable(r, ptr, &errtype, global);
    else if (strcasecmp(r->method, "DISABLE-APP") == 0)
        errstring = process_disable(r, ptr, &errtype, global);
    else if (strcasecmp(r->method, "STOP-APP") == 0)
        errstring = process_stop(r, ptr, &errtype, global);
    else if (strcasecmp(r->method, "REMOVE-APP") == 0)
        errstring = process_remove(r, ptr, &errtype, global);
    /* Status handling */
    else if (strcasecmp(r->method, "STATUS") == 0)
        errstring = process_status(r, ptr, &errtype);
    else if (strcasecmp(r->method, "DUMP") == 0)
        errstring = process_dump(r, &errtype);
    else if (strcasecmp(r->method, "INFO") == 0)
        errstring = process_info(r, &errtype);
    else if (strcasecmp(r->method, "PING") == 0)
        errstring = process_ping(r, ptr, &errtype);
    else if (strcasecmp(r->method, "ADDID") == 0)
        errstring = process_addid(r, ptr, &errtype);
    else if (strcasecmp(r->method, "REMOVEID") == 0)
        errstring = process_removeid(r, ptr, &errtype);
    else if (strcasecmp(r->method, "QUERY") == 0)
        errstring = process_query(r, ptr, &errtype);
    else {
        errstring = SCMDUNS;
        errtype = TYPESYNTAX;
    }

    /* Check error string and build the error message */
    if (errstring) {
        process_error(r, errstring, errtype);
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

    if (storage == NULL) {
        /* that happens when doing a gracefull restart for example after additing/changing the storage provider */
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "Fatal storage provider not initialized");
        return;
    }

    mconf->last_updated = 0;

    if (mconf->basefilename) {
        node = apr_pstrcat(p, mconf->basefilename, "/manager.node", NULL);
        context = apr_pstrcat(p, mconf->basefilename, "/manager.context", NULL);
        host = apr_pstrcat(p, mconf->basefilename, "/manager.host", NULL);
        balancer = apr_pstrcat(p, mconf->basefilename, "/manager.balancer", NULL);
        sessionid = apr_pstrcat(p, mconf->basefilename, "/manager.sessionid", NULL);
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
    if (get_last_mem_error(nodestatsmem) != APR_SUCCESS) {
        char buf[120];
        ap_log_error(APLOG_MARK, APLOG_NOERRNO|APLOG_EMERG, 0, s, "get_mem_node %s failed: %s",
                     node, apr_strerror(get_last_mem_error(nodestatsmem), buf, sizeof(buf)));
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
    const char *err = ap_check_cmd_context(cmd, GLOBAL_ONLY);
    if (err != NULL) {
        return err;
    }
    mconf->maxcontext = atoi(word);
    return NULL;
}
static const char *cmd_manager_maxnode(cmd_parms *cmd, void *mconfig, const char *word)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    const char *err = ap_check_cmd_context(cmd, GLOBAL_ONLY);
    if (err != NULL) {
        return err;
    }
    mconf->maxnode = atoi(word);
    return NULL;
}
static const char *cmd_manager_maxhost(cmd_parms *cmd, void *mconfig, const char *word)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    const char *err = ap_check_cmd_context(cmd, GLOBAL_ONLY);
    if (err != NULL) {
        return err;
    }
    mconf->maxhost = atoi(word);
    return NULL;
}
static const char *cmd_manager_maxsessionid(cmd_parms *cmd, void *mconfig, const char *word)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    const char *err = ap_check_cmd_context(cmd, GLOBAL_ONLY);
    if (err != NULL) {
        return err;
    }
    mconf->maxsessionid = atoi(word);
    return NULL;
}
static const char *cmd_manager_maxjgroupsid(cmd_parms *cmd, void *mconfig, const char *word)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    const char *err = ap_check_cmd_context(cmd, GLOBAL_ONLY);
    if (err != NULL) {
        return err;
    }
    mconf->maxjgroupsid = atoi(word);
    return NULL;
}
static const char *cmd_manager_memmanagerfile(cmd_parms *cmd, void *mconfig, const char *word)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    const char *err = ap_check_cmd_context(cmd, GLOBAL_ONLY);
    if (err != NULL) {
        return err;
    }
    mconf->basefilename = apr_pstrdup(cmd->pool, word);
    if (apr_dir_make_recursive(mconf->basefilename, APR_UREAD | APR_UWRITE | APR_UEXECUTE, cmd->pool) != APR_SUCCESS)
        return  "Can't create directory corresponding to MemManagerFile";
    return NULL;
}
static const char *cmd_manager_balancername(cmd_parms *cmd, void *mconfig, const char *word)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    mconf->balancername = apr_pstrdup(cmd->pool, word);
    return NULL;
}
static const char*cmd_manager_pers(cmd_parms *cmd, void *dummy, const char *arg)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    const char *err = ap_check_cmd_context(cmd, GLOBAL_ONLY);
    if (err != NULL) {
        return err;
    }
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

static const char*cmd_manager_nonce(cmd_parms *cmd, void *dummy, const char *arg)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    if (strcasecmp(arg, "Off") == 0)
       mconf->nonce = 0;
    else if (strcasecmp(arg, "On") == 0)
       mconf->nonce = -1;
    else {
       return "CheckNonce must be one of: "
              "off | on";
    }
    return NULL;
}
static const char*cmd_manager_allow_display(cmd_parms *cmd, void *dummy, const char *arg)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    if (strcasecmp(arg, "Off") == 0)
       mconf->allow_display = 0;
    else if (strcasecmp(arg, "On") == 0)
       mconf->allow_display = -1;
    else {
       return "AllowDisplay must be one of: "
              "off | on";
    }
    return NULL;
}
static const char*cmd_manager_allow_cmd(cmd_parms *cmd, void *dummy, const char *arg)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    if (strcasecmp(arg, "Off") == 0)
       mconf->allow_cmd = 0;
    else if (strcasecmp(arg, "On") == 0)
       mconf->allow_cmd = -1;
    else {
       return "AllowCmd must be one of: "
              "off | on";
    }
    return NULL;
}
static const char*cmd_manager_reduce_display(cmd_parms *cmd, void *dummy, const char *arg)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    if (strcasecmp(arg, "Off") == 0)
       mconf->reduce_display = 0;
    else if (strcasecmp(arg, "On") == 0)
       mconf->reduce_display = -1;
    else {
       return "ReduceDisplay must be one of: "
              "off | on";
    }
    return NULL;
}
static const char*cmd_manager_maxmesssize(cmd_parms *cmd, void *mconfig, const char *word)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    const char *err = ap_check_cmd_context(cmd, GLOBAL_ONLY);
    if (err != NULL) {
        return err;
    }
    mconf->maxmesssize = atoi(word);
    if (mconf->maxmesssize < MAXMESSSIZE)
       return "MaxMCMPMessSize must bigger than 1024";
    return NULL;
}
static const char*cmd_manager_enable_mcpm_receive(cmd_parms *cmd, void *dummy)
{
    mod_manager_config *mconf = ap_get_module_config(cmd->server->module_config, &manager_module);
    if (!cmd->server->is_virtual)
        return "EnableMCPMReceive must be in a VirtualHost";
    mconf->enable_mcpm_receive = -1;
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
        "Maxjgroupsid",
        cmd_manager_maxjgroupsid,
        NULL,
        OR_ALL,
        "Maxjgroupsid - number jgroupsid supported by mod_cluster"
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
    AP_INIT_TAKE1(
        "CheckNonce",
        cmd_manager_nonce,
        NULL,
        OR_ALL,
        "CheckNonce - Switch check of nonce when using mod_cluster-manager handler on | off (Default: on Nonce checked)"
    ),
    AP_INIT_TAKE1(
        "AllowDisplay",
        cmd_manager_allow_display,
        NULL,
        OR_ALL,
        "AllowDisplay - Display additional information in the mod_cluster-manager page on | off (Default: off Only version displayed)"
    ),
    AP_INIT_TAKE1(
        "AllowCmd",
        cmd_manager_allow_cmd,
        NULL,
        OR_ALL,
        "AllowCmd - Allow commands using mod_cluster-manager URL on | off (Default: on Commmands allowed)"
    ),
    AP_INIT_TAKE1(
        "ReduceDisplay",
        cmd_manager_reduce_display,
        NULL,
        OR_ALL,
        "ReduceDisplay - Don't contexts in the main mod_cluster-manager page. on | off (Default: off Context displayed)"
    ),
   AP_INIT_TAKE1(
        "MaxMCMPMessSize",
        cmd_manager_maxmesssize,
        NULL,
        OR_ALL,
        "MaxMCMPMaxMessSize - Maximum size of MCMP messages. (Default: calculated min value: 1024)"
    ),
    AP_INIT_NO_ARGS(
        "EnableMCPMReceive",
         cmd_manager_enable_mcpm_receive,
         NULL,
         OR_ALL,
         "EnableMCPMReceive - Allow the VirtualHost to receive MCPM."
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
    ap_hook_handler(manager_handler, NULL, NULL, APR_HOOK_REALLY_FIRST);

    /* Register nodes/hosts/contexts table provider */
    ap_register_provider(p, "manager" , "shared", "0", &node_storage);
    ap_register_provider(p, "manager" , "shared", "1", &host_storage);
    ap_register_provider(p, "manager" , "shared", "2", &context_storage);
    ap_register_provider(p, "manager" , "shared", "3", &balancer_storage);
    ap_register_provider(p, "manager" , "shared", "4", &sessionid_storage);
    ap_register_provider(p, "manager" , "shared", "5", &domain_storage);
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
    mconf->maxjgroupsid = DEFMAXJGROUPSID;
    mconf->last_updated = 0;
    mconf->persistent = 0;
    mconf->nonce = -1;
    mconf->balancername = NULL;
    mconf->allow_display = 0;
    mconf->allow_cmd = -1;
    mconf->reduce_display = 0;
    mconf->enable_mcpm_receive = 0;
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
    mconf->nonce = -1;
    mconf->balancername = NULL;
    mconf->allow_display = 0;
    mconf->allow_cmd = -1;
    mconf->reduce_display = 0;

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

    if (mconf2->maxjgroupsid != DEFMAXJGROUPSID)
        mconf->maxjgroupsid = mconf2->maxjgroupsid;
    else if (mconf1->maxjgroupsid != DEFMAXJGROUPSID)
        mconf->maxjgroupsid = mconf1->maxjgroupsid;

    if (mconf2->persistent != 0)
        mconf->persistent = mconf2->persistent;
    else if (mconf1->persistent != 0)
        mconf->persistent = mconf1->persistent;

    if (mconf2->nonce != -1)
        mconf->nonce = mconf2->nonce;
    else if (mconf1->nonce != -1)
        mconf->nonce = mconf1->nonce;

    if (mconf2->balancername)
        mconf->balancername = apr_pstrdup(p, mconf2->balancername);
    else if (mconf1->balancername)
        mconf->balancername = apr_pstrdup(p, mconf1->balancername);

    if (mconf2->allow_display != 0)
        mconf->allow_display = mconf2->allow_display;
    else if (mconf1->allow_display != 0)
        mconf->allow_display = mconf1->allow_display;

    if (mconf2->allow_cmd != -1)
        mconf->allow_cmd = mconf2->allow_cmd;
    else if (mconf1->allow_cmd != -1)
        mconf->allow_cmd = mconf1->allow_cmd;

    if (mconf2->reduce_display != 0)
        mconf->reduce_display = mconf2->reduce_display;
    else if (mconf1->reduce_display != 0)
        mconf->reduce_display = mconf1->reduce_display;

    if (mconf2->enable_mcpm_receive != 0)
        mconf->enable_mcpm_receive = mconf2->enable_mcpm_receive;
    else if (mconf1->enable_mcpm_receive != 0)
        mconf->enable_mcpm_receive = mconf1->enable_mcpm_receive;

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
