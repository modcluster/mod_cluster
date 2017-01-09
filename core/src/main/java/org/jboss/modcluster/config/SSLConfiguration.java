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

import javax.net.ssl.SSLContext;

/**
 * A SSLConfiguration.
 *
 * @author Brian Stansberry
 * @author Radoslav Husar
 */
public interface SSLConfiguration {

    /**
     * Configured {@link SSLContext} instance to use ignoring all other configuration options.
     * @return SSLContext configured context instance
     */
    SSLContext getSslContext();

    /**
     * SSL ciphers.
     * Ignored if {@link SSLContext} is provided directly via {@link SSLConfiguration#getSslContext()}.
     */
    String getSslCiphers();

    /**
     * SSL protocol.
     * Ignored if {@link SSLContext} is provided directly via {@link SSLConfiguration#getSslContext()}.
     */
    String getSslProtocol();

    /**
     * Certificate encoding algorithm.
     * Ignored if {@link SSLContext} is provided directly via {@link SSLConfiguration#getSslContext()}.
     */
    String getSslCertificateEncodingAlgorithm();

    /**
     * SSL keystore.
     * Ignored if {@link SSLContext} is provided directly via {@link SSLConfiguration#getSslContext()}.
     */
    String getSslKeyStore();

    /**
     * SSL keystore password.
     * Ignored if {@link SSLContext} is provided directly via {@link SSLConfiguration#getSslContext()}.
     */
    String getSslKeyStorePassword();

    /**
     * Keystore type.
     * Ignored if {@link SSLContext} is provided directly via {@link SSLConfiguration#getSslContext()}.
     */
    String getSslKeyStoreType();

    /**
     * Keystore provider.
     * Ignored if {@link SSLContext} is provided directly via {@link SSLConfiguration#getSslContext()}.
     */
    String getSslKeyStoreProvider();

    /**
     * Truststore algorithm.
     * Ignored if {@link SSLContext} is provided directly via {@link SSLConfiguration#getSslContext()}.
     */
    String getSslTrustAlgorithm();

    /**
     * Key alias.
     * Ignored if {@link SSLContext} is provided directly via {@link SSLConfiguration#getSslContext()}.
     */
    String getSslKeyAlias();

    /**
     * Certificate revocation list.
     * Ignored if {@link SSLContext} is provided directly via {@link SSLConfiguration#getSslContext()}.
     */
    String getSslCrlFile();

    /**
     * Trust max certificate length.
     * Ignored if {@link SSLContext} is provided directly via {@link SSLConfiguration#getSslContext()}.
     */
    int getSslTrustMaxCertLength();

    /**
     * Trust store file.
     * Ignored if {@link SSLContext} is provided directly via {@link SSLConfiguration#getSslContext()}.
     */
    String getSslTrustStore();

    /**
     * Trust store password.
     * Ignored if {@link SSLContext} is provided directly via {@link SSLConfiguration#getSslContext()}.
     */
    String getSslTrustStorePassword();

    /**
     * Trust store type.
     * Ignored if {@link SSLContext} is provided directly via {@link SSLConfiguration#getSslContext()}.
     */
    String getSslTrustStoreType();

    /**
     * Trust store provider.
     * Ignored if {@link SSLContext} is provided directly via {@link SSLConfiguration#getSslContext()}.
     */
    String getSslTrustStoreProvider();
}
