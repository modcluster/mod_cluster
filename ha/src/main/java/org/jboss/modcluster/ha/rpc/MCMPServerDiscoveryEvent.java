/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.modcluster.ha.rpc;

import java.io.Serializable;
import java.net.InetSocketAddress;

import net.jcip.annotations.Immutable;

import org.jboss.ha.framework.interfaces.ClusterNode;
import org.jboss.modcluster.mcmp.MCMPServer;

/**
 * Event object indicating the discovery or requested removal of an {@link MCMPServer}.
 * 
 * @author Brian Stansberry
 */
@Immutable
public class MCMPServerDiscoveryEvent implements Serializable, Comparable<MCMPServerDiscoveryEvent> {
    /** The serialVersionUID */
    private static final long serialVersionUID = -4615651826967237065L;

    private final ClusterNode sender;
    private final InetSocketAddress mcmpServer;
    private final boolean addition;
    private final int eventIndex;

    public MCMPServerDiscoveryEvent(ClusterNode sender, InetSocketAddress mcmpServer, boolean addition, int eventIndex) {
        assert sender != null : "sender is null";
        assert mcmpServer != null : "mcmpServer is null";

        this.sender = sender;
        this.mcmpServer = mcmpServer;
        this.addition = addition;
        this.eventIndex = eventIndex;
    }

    /**
     * Creates a new MCMPServerDiscoveryEvent with the same values as an existing one, but a new event index. Used in resending
     * events.
     * 
     * @param toRecreate
     * @param newEventIndex
     */
    public MCMPServerDiscoveryEvent(MCMPServerDiscoveryEvent toRecreate, int newEventIndex) {
        this(toRecreate.getSender(), toRecreate.getMCMPServer(), toRecreate.isAddition(), newEventIndex);
    }

    public ClusterNode getSender() {
        return this.sender;
    }

    public InetSocketAddress getMCMPServer() {
        return this.mcmpServer;
    }

    public boolean isAddition() {
        return this.addition;
    }

    public int getEventIndex() {
        return this.eventIndex;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{sender=").append(this.sender);
        builder.append(", mcmpServer=").append(this.mcmpServer);
        builder.append(", addition=").append(this.addition);
        builder.append(", eventIndex=").append(this.eventIndex);
        builder.append("}");
        return builder.toString();
    }

    /**
     * @{inheritDoc
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(MCMPServerDiscoveryEvent event) {
        return this.eventIndex - event.eventIndex;
    }
}
