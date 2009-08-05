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
    int status;
    int lbfactor;
    int elected;
    int removed;
  
    /**
     * Check that nodes in nodeinfos corresponds to nodes in nodenames
     */
    static public boolean check(ArrayList nodes, String [] nodenames)
    {
        boolean [] in = new boolean[nodenames.length];

        if (nodes == null || nodenames == null)
            return false;

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
                    if (in[i])
                        return false; // twice.
                    if (nodeinfos[j].removed == 0) {
                        in[i] = true;
                        hasit = true;
                    }
                }
            }
            if (!hasit)
                return false; // not found.
        }
        int i = 0;
        for (int j=0; j<nodeinfos.length; j++) {
            if (nodeinfos[j].removed == 0)
                i++;
        }
        if (i != nodenames.length)
            return false; // Too many route entries in httpd.
        return true;
    }
}
