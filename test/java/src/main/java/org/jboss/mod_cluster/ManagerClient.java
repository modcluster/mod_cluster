/*
 *  mod_cluster
 *
 *  Copyright(c) 2009 Red Hat Middleware, LLC,
 *  and individual contributors as indicated by the @authors tag.
 *  See the copyright.txt in the distribution for a
 *  full listing of individual contributors.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library in the file COPYING.LIB;
 *  if not, write to the Free Software Foundation, Inc.,
 *  59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
 *
 * @author Jean-Frederic Clere
 * @version $Revision$
 */

package org.jboss.mod_cluster;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Random;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;


public class  ManagerClient {

    private String URL = null;

    private String nonce = null;

    public int httpResponseCode = 0;
    public String requestedSessionId = null;

    private HttpClient httpClient = null;

        /**
          * Run the first test.
          *
          * @param string a part of URL to connect to.
          *
          * @return ManagerClient object
          *
          * @throws IOException for any failures.
          */
        public ManagerClient(String string) throws Exception
        {
            URL = "http://" + string + "/mod_cluster_manager/";
                GetMethod gm = null;
                HttpMethodBase bm = null;
                if (httpClient == null) {
                     httpClient = new HttpClient();
                     gm = new GetMethod(URL);
                     bm = gm;
                } 

                System.out.println("Connecting to " + URL);

                Integer connectionTimeout = 40000;
                bm.getParams().setParameter("http.socket.timeout", connectionTimeout);
                bm.getParams().setParameter("http.connection.timeout", connectionTimeout);
                httpClient.getParams().setParameter("http.socket.timeout", connectionTimeout);
                httpClient.getParams().setParameter("http.connection.timeout", connectionTimeout);

                try {
                    httpResponseCode = httpClient.executeMethod(gm);

                    if (httpResponseCode == 200) {
                        // Read the nonce.
                        String result = gm.getResponseBodyAsString();
                        String [] records = result.split("\n");
                        for (int i=0; i<records.length; i++) {
                            int j = records[i].indexOf("?nonce=");
                            if (j < 0)
                                continue;
                            j = j + 7;
                            String nnonce = records[i].substring(j);
                            int k = nnonce.indexOf('&');
                            if (k > 0) {
                                nonce = nnonce.substring(0, k); 
                                break;
                            }
                        }
                    } else {
                        System.out.println("response: " + httpResponseCode);
                        System.out.println("response: " + bm.getStatusLine());
                        throw(new Exception("Reponse notok"));
                    }
                    // System.out.println("response:\n" + bm.getResponseBodyAsString(len)); 
                } catch(HttpException e) { 
                    System.out.println("error: " + e);
                    throw(e);
                }
                bm.releaseConnection();
        }
        /*
         * Disable a Node
         */
        public void disable(String node) throws Exception {
            String DURL = URL + "?nonce=" + nonce + "&Cmd=DISABLE-APP&Range=NODE&JVMRoute=" + node;
            DoCmd(DURL);
        }
        /* Disable a Context */
        public void disable(String node, String host, String context) throws Exception {
            String DURL = URL + "?nonce=" + nonce + "&Cmd=DISABLE-APP&Range=CONTEXT&JVMRoute=" + node +
                          "&Alias=" + host +
                          "&Context=" + context;
            DoCmd(DURL);
        }
 
        private String DoCmd(String DURL) throws Exception {
            GetMethod gm = new GetMethod(DURL);
            try {
                httpResponseCode = httpClient.executeMethod(gm);

                if (httpResponseCode == 200) {
                    String result = gm.getResponseBodyAsString();
                    gm.releaseConnection();
                    return result;
                }
            } catch (HttpException e) {
                    System.out.println("error: " + e);
                    throw(e);
            }
            gm.releaseConnection();
            throw(new Exception("Reponse notok"));

        }
        public String getProxyInfo() throws Exception {
            String DURL = URL + "?nonce=" + nonce + "&Cmd=INFO&Range=ALL";
            return DoCmd(DURL);
        }
}
