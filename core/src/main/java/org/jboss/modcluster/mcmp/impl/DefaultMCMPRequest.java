/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.mcmp.impl;

import java.util.Collections;
import java.util.Map;

import net.jcip.annotations.Immutable;
import org.jboss.modcluster.mcmp.MCMPRequest;
import org.jboss.modcluster.mcmp.MCMPRequestType;

/**
 * Encapsulates the parameters for a request over MCMP.
 *
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
@Immutable
public class DefaultMCMPRequest implements MCMPRequest {
    /** The serialVersionUID */
    private static final long serialVersionUID = 7107364666635260031L;

    private final MCMPRequestType requestType;
    private final boolean wildcard;
    private final Map<String, String> parameters;
    private final String jvmRoute;

    /**
     * Create a new ModClusterRequest.
     */
    public DefaultMCMPRequest(MCMPRequestType requestType, boolean wildcard, String jvmRoute, Map<String, String> parameters) {
        this.requestType = requestType;
        this.wildcard = wildcard;
        this.jvmRoute = jvmRoute;
        this.parameters = Collections.unmodifiableMap(parameters);
    }

    @Override
    public MCMPRequestType getRequestType() {
        return this.requestType;
    }

    @Override
    public boolean isWildcard() {
        return this.wildcard;
    }

    @Override
    public String getJvmRoute() {
        return this.jvmRoute;
    }

    @Override
    public Map<String, String> getParameters() {
        return this.parameters;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getName());
        sb.append("{requestType=").append(this.requestType);
        sb.append(",wildcard=").append(this.wildcard);
        sb.append(",jvmRoute=").append(this.jvmRoute);
        sb.append(",parameters=").append(this.parameters);
        sb.append("}");

        return sb.toString();
    }
}
