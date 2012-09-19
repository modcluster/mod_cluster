/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.modcluster;

import java.security.cert.CRLException;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.modcluster.container.Host;

/**
 * @author Paul Ferraro
 */
@MessageBundle(projectCode = "MODCLUSTER")
public interface ModClusterMessages {
    ModClusterMessages MESSAGES = Messages.getBundle(ModClusterMessages.class);

    @Message(id = 100, value = "Unable to locate host %s")
    IllegalArgumentException hostNotFound(String host);

    @Message(id = 101, value = "Unable to locate context %s within %s")
    IllegalArgumentException contextNotFound(String context, Host host);

    @Message(id = 102, value = "Load metric weight must be greater than or equal to zero.")
    IllegalArgumentException invalidWeight();

    @Message(id = 103, value = "Load metric capacity must be greater than zero.")
    IllegalArgumentException invalidCapacity();

    @Message(id = 104, value = "%s algorithm does not support certificate revocation lists.")
    CRLException crlNotSupported(String algorithm);
}
