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
