/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.mcmp;

import java.io.Serializable;
import java.util.Map;

/**
 * Encapsulates the parameters for a request over MCMP.
 *
 * @author Brian Stansberry
 */
public interface MCMPRequest extends Serializable {
    MCMPRequestType getRequestType();

    boolean isWildcard();

    String getJvmRoute();

    Map<String, String> getParameters();
}
