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
