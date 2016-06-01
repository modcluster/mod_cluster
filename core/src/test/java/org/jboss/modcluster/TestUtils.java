/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * <p>
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * <p>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.modcluster;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.xml.bind.DatatypeConverter;

/**
 * Utility class to be used in tests.
 *
 * @author Radoslav Husar
 * @since 1.3.0
 */
public class TestUtils {

    private static final String ADVERTISE_INTERFACE_ADDRESS = System.getProperty("multicast.interface.address");
    private static final String ADVERTISE_INTERFACE_NAME = System.getProperty("multicast.interface.name");

    public static final String RFC_822_FMT = "EEE, d MMM yyyy HH:mm:ss Z";
    public static final DateFormat df = new SimpleDateFormat(RFC_822_FMT, Locale.US);
    public static final byte[] zeroMd5Sum = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    public static NetworkInterface getAdvertiseInterface() throws SocketException, UnknownHostException {
        InetAddress address = InetAddress.getByName(ADVERTISE_INTERFACE_ADDRESS);
        NetworkInterface ifaceByAddress = NetworkInterface.getByInetAddress(address);

        NetworkInterface ifaceByName;
        if (ADVERTISE_INTERFACE_NAME != null) {
            ifaceByName = NetworkInterface.getByName(ADVERTISE_INTERFACE_NAME);
        } else {
            return ifaceByAddress;
        }

        if (ADVERTISE_INTERFACE_ADDRESS == null || ifaceByAddress.equals(ifaceByName)) {
            return ifaceByName;
        } else {
            throw new IllegalStateException("Both -Dmulticast.interface.address and -Dmulticast.interface.name specified but resolve to different interfaces!");
        }
    }

    /**
     * Generates datagram packet content buffer including all fields as sent by native code.
     */
    public static byte[] generateAdvertisePacketData(Date date, int sequence, String server, String serverAddress) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");

        String rfcDate = df.format(date);

        md.update(zeroMd5Sum);
        digestString(md, rfcDate);
        digestString(md, String.valueOf(sequence));
        digestString(md, server);

        // Convert to hex
        String digestHex = DatatypeConverter.printHexBinary(md.digest());

        StringBuilder data = new StringBuilder("HTTP/1.1 200 OK\r\n");
        data.append("Date: ");
        data.append(rfcDate);
        data.append("\r\n");
        data.append("Sequence: ");
        data.append(sequence);
        data.append("\r\n");
        data.append("Digest: ");
        data.append(digestHex);
        data.append("\r\n");
        data.append("Server: ");
        data.append(server);
        data.append("\r\n");
        data.append("X-Manager-Address: ");
        data.append(serverAddress);
        data.append("\r\n");

        return data.toString().getBytes();
    }

    /**
     * Utility method to digest {@link String}s.
     *
     * @param md {@link MessageDigest}
     * @param s  {@link String} to update the digest with
     */
    private static void digestString(MessageDigest md, String s) {
        int len = s.length();
        byte[] b = new byte[len];
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c < 127) {
                b[i] = (byte) c;
            } else {
                b[i] = '?';
            }
        }
        md.update(b);
    }

    // Utility class
    private TestUtils() {
    }
}
