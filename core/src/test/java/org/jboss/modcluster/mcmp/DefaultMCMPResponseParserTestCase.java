/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.mcmp;

import static org.junit.jupiter.api.Assertions.*;

import org.jboss.modcluster.mcmp.impl.DefaultMCMPResponseParser;
import org.junit.jupiter.api.Test;

/**
 * @author Paul Ferraro
 */
class DefaultMCMPResponseParserTestCase {
    private final MCMPResponseParser parser = new DefaultMCMPResponseParser();

    @Test
    void parseInfoResponse() {

    }

    @Test
    void parsePingResponse() {
        assertTrue(this.parser.parsePingResponse("State=OK&Type=PING-RSP&id=1"));
        assertTrue(this.parser.parsePingResponse("Type=PING-RSP&State=OK&id=1"));
        assertTrue(this.parser.parsePingResponse("Type=PING-RSP&id=1&State=OK"));

        assertFalse(this.parser.parsePingResponse("State=NOTOK&Type=PING-RSP&id=1"));
        assertFalse(this.parser.parsePingResponse("Type=PING-RSP&State=NOTOK&id=1"));
        assertFalse(this.parser.parsePingResponse("Type=PING-RSP&id=1&State=NOTOK"));

        assertFalse(this.parser.parsePingResponse("State=&Type=PING-RSP&id=1"));
        assertFalse(this.parser.parsePingResponse("Type=PING-RSP&State=&id=1"));
        assertFalse(this.parser.parsePingResponse("Type=PING-RSP&id=1&State="));

        assertFalse(this.parser.parsePingResponse("State&Type=PING-RSP&id=1"));
        assertFalse(this.parser.parsePingResponse("Type=PING-RSP&State&id=1"));
        assertFalse(this.parser.parsePingResponse("Type=PING-RSP&id=1&State"));

        assertFalse(this.parser.parsePingResponse("=&=&="));
        assertFalse(this.parser.parsePingResponse("&&"));
        assertFalse(this.parser.parsePingResponse("adsfhaskhlkjahsf"));
        assertFalse(this.parser.parsePingResponse(""));

        assertFalse(this.parser.parsePingResponse(null));
    }

    @Test
    void parseStopAppResponse() {
        assertEquals(0, this.parser.parseStopAppResponse(null));
        assertEquals(0, this.parser.parseStopAppResponse(""));
        assertEquals(
                3,
                this.parser
                        .parseStopAppResponse("Type=STOP-APP-RSP&JvmRoute=127.0.0.1:8009:jboss.web&Alias=localhos&Context=/context&Requests=3"));
    }
}
