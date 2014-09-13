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

#ifndef MOD_PROXY_CLUSTER_H
#define MOD_PROXY_CLUSTER_H

#define MOD_CLUSTER_EXPOSED_VERSION "mod_cluster/1.3.1.Alpha2"

/* define the values for sticky_force */
#define STSESSION 0x01 /* Use sticky session logic (first sessionid and then domain) */
#define STSESSREM 0x02 /* Remove session information if the failover can't use sticky */
#define STSESSFOR 0x04 /* Force sticky (return error if no worker corresponds to sessionid or domain) */

struct balancer_method {
/**
 * Check that the node is responding
 * @param r request_rec structure.
 * @param id ident of the worker.
 * @param load load factor to set if test is ok.
 * @return 0: All OK 500 : Error
 */ 
int (* proxy_node_isup)(request_rec *r, int id, int load);
/**
 * Check that the node is responding
 * @param r request_rec structure.
 * @param scheme something like ajp, http or https.
 * @param host the hostname.
 * @param port the port on which the node connector is running
 * @return 0: All OK 500 : Error
 */ 
int (* proxy_host_isup)(request_rec *r, char *scheme, char *host, char *port);
};
typedef struct balancer_method balancer_method;
#endif /*MOD_PROXY_CLUSTER_H*/
