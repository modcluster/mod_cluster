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
#ifndef MOD_CLUSTERSIZE_H
#define MOD_CLUSTERSIZE_H

/* For host.h */
#define HOSTALIASZ 100

/* For context.h */
#define CONTEXTSZ 80

/* For node.h */
#define BALANCERSZ 40
#define JVMROUTESZ 80
#define DOMAINNDSZ 20
#define HOSTNODESZ 64
#define PORTNODESZ 7
#define SCHEMENDSZ 6

/* For balancer.h */
#define COOKNAMESZ 30
#define PATHNAMESZ 30

/* For sessionid.h */
#define SESSIONIDSZ 128

#endif /* MOD_CLUSTERSIZE_H */
