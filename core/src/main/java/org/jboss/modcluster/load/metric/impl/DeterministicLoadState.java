/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.metric.impl;

/**
 * @author Paul Ferraro
 */
public interface DeterministicLoadState {
    double delta(double currentLoad);
}