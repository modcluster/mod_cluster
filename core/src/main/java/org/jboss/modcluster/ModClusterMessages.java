/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
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
