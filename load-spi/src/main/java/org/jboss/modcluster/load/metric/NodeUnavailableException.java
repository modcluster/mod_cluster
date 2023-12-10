/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.load.metric;

/**
 * Exception thrown by {@link LoadMetric} implementations to indicate that the node should be put into error state.
 * If any of the metrics throws this exception, no further metrics are queried and node is put into error state (-1).
 *
 * @author Radoslav Husar
 */
public class NodeUnavailableException extends Exception {
}
