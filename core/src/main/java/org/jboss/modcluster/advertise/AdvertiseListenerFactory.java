/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.advertise;

import java.io.IOException;

import org.jboss.modcluster.config.AdvertiseConfiguration;
import org.jboss.modcluster.mcmp.MCMPHandler;

/**
 * @author Paul Ferraro
 */
public interface AdvertiseListenerFactory {
    AdvertiseListener createListener(MCMPHandler handler, AdvertiseConfiguration config) throws IOException;
}
