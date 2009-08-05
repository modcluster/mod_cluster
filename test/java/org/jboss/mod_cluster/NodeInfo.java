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

package org.jboss.mod_cluster;

import java.util.ArrayList;

public class NodeInfo {
    String JVMRoute;
    int lbfactor;
    int elected;
  
    /**
     * Check that nodes in nodeinfos corresponds to nodes in nodenames
     */
    static public boolean check(ArrayList nodes, String [] nodenames)
    {
        boolean [] in = new boolean[nodenames.length];

        if (nodes == null || nodenames == null) {
            System.out.println("No nodes or no names");
            return false;
        }

        NodeInfo [] nodeinfos = new NodeInfo[nodes.size()];
        for (int i=0; i<nodeinfos.length; i++) {
            nodeinfos[i] = (NodeInfo) nodes.get(i);
        }

        for (int i=0; i<nodenames.length; i++)
            in[i] = false;

        for (int i=0; i<nodenames.length; i++) {
            boolean hasit = false;
            for (int j=0; j<nodeinfos.length; j++) {
                if (nodeinfos[j].JVMRoute.equals(nodenames[i])) {
                    if (in[i]) {
                        System.out.println("Name " + nodenames[i] + "found twice!!!");
                        return false; // twice.
                    }
                    in[i] = true;
                    hasit = true;
                }
            }
            if (!hasit) {
                System.out.println("Name " + nodenames[i] + " not found!!!");
                return false; // not found.
            }
        }
        if (nodeinfos.length != nodenames.length) {
            System.out.println("Too many route entries in httpd!!!");
            return false; // Too many route entries in httpd.
        }
        return true;
    }
    static public void print(ArrayList nodes, String [] nodenames) {
        if (nodes == null || nodes.size() == 0) {
            System.out.println("No nodes");
            return;
        }
        if (nodenames == null || nodenames.length == 0) {
            System.out.println("No names???");
            return;
        }

        for (int i=0; i<nodes.size(); i++) {
            NodeInfo nodeinfo = (NodeInfo) nodes.get(i);
            System.out.println("Node[" + i + "]: " + nodeinfo.JVMRoute);
        }
        for (int i=0; i<nodenames.length; i++) {
            System.out.println("Name[" + i + "]: " + nodenames[i]);
        }
    }
}
