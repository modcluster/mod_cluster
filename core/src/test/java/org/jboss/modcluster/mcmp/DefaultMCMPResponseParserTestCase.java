/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.mcmp;

import org.jboss.modcluster.mcmp.impl.DefaultMCMPResponseParser;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class DefaultMCMPResponseParserTestCase {
    private final MCMPResponseParser parser = new DefaultMCMPResponseParser();

    @Test
    public void parseInfoResponse() {

    }

    @Test
    public void parsePingResponse() {
        Assert.assertTrue(this.parser.parsePingResponse("State=OK&Type=PING-RSP&id=1"));
        Assert.assertTrue(this.parser.parsePingResponse("Type=PING-RSP&State=OK&id=1"));
        Assert.assertTrue(this.parser.parsePingResponse("Type=PING-RSP&id=1&State=OK"));

        Assert.assertFalse(this.parser.parsePingResponse("State=NOTOK&Type=PING-RSP&id=1"));
        Assert.assertFalse(this.parser.parsePingResponse("Type=PING-RSP&State=NOTOK&id=1"));
        Assert.assertFalse(this.parser.parsePingResponse("Type=PING-RSP&id=1&State=NOTOK"));

        Assert.assertFalse(this.parser.parsePingResponse("State=&Type=PING-RSP&id=1"));
        Assert.assertFalse(this.parser.parsePingResponse("Type=PING-RSP&State=&id=1"));
        Assert.assertFalse(this.parser.parsePingResponse("Type=PING-RSP&id=1&State="));

        Assert.assertFalse(this.parser.parsePingResponse("State&Type=PING-RSP&id=1"));
        Assert.assertFalse(this.parser.parsePingResponse("Type=PING-RSP&State&id=1"));
        Assert.assertFalse(this.parser.parsePingResponse("Type=PING-RSP&id=1&State"));

        Assert.assertFalse(this.parser.parsePingResponse("=&=&="));
        Assert.assertFalse(this.parser.parsePingResponse("&&"));
        Assert.assertFalse(this.parser.parsePingResponse("adsfhaskhlkjahsf"));
        Assert.assertFalse(this.parser.parsePingResponse(""));

        Assert.assertFalse(this.parser.parsePingResponse(null));
    }

    @Test
    public void parseStopAppResponse() {
        Assert.assertEquals(0, this.parser.parseStopAppResponse(null));
        Assert.assertEquals(0, this.parser.parseStopAppResponse(""));
        Assert.assertEquals(
                3,
                this.parser
                        .parseStopAppResponse("Type=STOP-APP-RSP&JvmRoute=127.0.0.1:8009:jboss.web&Alias=localhos&Context=/context&Requests=3"));
    }
}
