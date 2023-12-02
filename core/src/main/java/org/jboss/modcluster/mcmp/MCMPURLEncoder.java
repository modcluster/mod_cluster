/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.mcmp;

import java.io.IOException;

/**
 * Allow to use TC and JBoss url converter.
 *
 * @author Jean-Frederic Clere
 */
public interface MCMPURLEncoder {
    /**
     * Add parameter to the buffer
     */
    void encodeParameter(String key, String value, boolean hasNext) throws IOException;

    /**
     * buffer of the encoded data
     */
    char[] getBuffer();

    /**
     * length of the encoded data
     */
    int getLength();
}
