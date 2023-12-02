/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.config;

/**
 * A SSLConfiguration.
 *
 * @author Brian Stansberry
 */
public interface SSLConfiguration {
    /**
     * SSL ciphers.
     */
    String getSslCiphers();

    /**
     * SSL protocol.
     */
    String getSslProtocol();

    /**
     * Certificate encoding algorithm.
     */
    String getSslCertificateEncodingAlgorithm();

    /**
     * SSL keystore.
     */
    String getSslKeyStore();

    /**
     * SSL keystore password.
     */
    String getSslKeyStorePassword();

    /**
     * Keystore type.
     */
    String getSslKeyStoreType();

    /**
     * Keystore provider.
     */
    String getSslKeyStoreProvider();

    /**
     * Truststore algorithm.
     */
    String getSslTrustAlgorithm();

    /**
     * Key alias.
     */
    String getSslKeyAlias();

    /**
     * Certificate revocation list.
     */
    String getSslCrlFile();

    /**
     * Trust max certificate length.
     */
    int getSslTrustMaxCertLength();

    /**
     * Trust store file.
     */
    String getSslTrustStore();

    /**
     * Trust store password.
     */
    String getSslTrustStorePassword();

    /**
     * Trust store type.
     */
    String getSslTrustStoreType();

    /**
     * Trust store provider.
     */
    String getSslTrustStoreProvider();
}
