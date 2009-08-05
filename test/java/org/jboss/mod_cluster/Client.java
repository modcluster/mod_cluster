/*
 *  mod_cluster
 *
 *  Copyright(c) 2008 Red Hat Middleware, LLC,
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


public class  Client extends Thread {

    private String jsessionid = null;

    private String URL = null;

    private String BaseURL = "http://localhost:8000";
    String post = null;
    String user = null;
    String pass = null;
    InputStream fd = null;

    private int nbtest = 10;
    private int delay = 1000;
    private int wait = 100;
    private Random rand = null;
    private boolean checkcookie = true;
    private boolean checknode = true;
    private boolean success = true;
    private String node = null;

    public int httpResponseCode = 0;
    public String requestedSessionId = null;

    private HttpClient httpClient = null;
    /*
     *  
     * Usage:
     *          java Client http://mywebserver:80/ test
     * 
     *  @param args command line arguments
     *                 Argument 0 is a URL to a web server
     *                 Argument 1 is the max time to wait between requests (in 10 milliseconds units)
     * 
     */
    public static void main(String[] args) throws Exception
        {
                if (args.length != 2)
                {
                        System.err.println("missing command line arguments");
                        System.exit(1);
                }
                Client client[] = new Client[500];
                for (int i=0; i<client.length; i++) {
                        client[i] = new Client();  
                        client[i].runit(args[0], 100000, true, Integer.parseInt(args[1]));
                }
                System.out.println("making \"second\" requests");
                for (int i=0; i<client.length; i++) {
                        client[i].start();
                }
                for (int i=0; i<client.length; i++) {
                    try {
                        client[i].join();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
        }

        /**
          * Run the first test.
          *
          * @param URL URL to connect to.
          * @param nbtest number of test the thread will run (not counting this one).
          * @param checkcookie check that the jsessionid cookie is not changing during the test.
          * @param post data to send in the POST.
          * @param user username of the BASIC authentication.
          * @param pass password of the usr for the BASIC authentication.
          * @param fd file containing the data to send chuncked in the POST (post content is ignored in this case).
          *
          * @return The http code returned by the server.
          *
          * @throws IOException for any failures.
          */
        public int runit(String URL, int nbtest, boolean checkcookie, String post, String user, String pass, InputStream fd) throws Exception
        {
                this.fd = fd;
                return runit(URL, nbtest, checkcookie, post, user, pass);
        }
        public int runit(String URL, int nbtest, boolean checkcookie, String post, String user, String pass, File fd) throws Exception
        {
                this.fd = new FileInputStream(fd);
                return runit(URL, nbtest, checkcookie, post, user, pass);
        }
        public int runit(String URL, int nbtest, boolean checkcookie, String post, String user, String pass) throws Exception
        {
                this.post = post;
                this.user = user;
                this.pass = pass;
                return runit(URL, nbtest, checkcookie);
        }
        public int runit(String URL, int nbtest, boolean checkcookie, boolean checknode) throws Exception
        {
                this.checknode = checknode;
                return runit(URL, nbtest, checkcookie);
        }
        public int runit(String URL, int nbtest, boolean checkcookie) throws Exception
        {
                if (URL.startsWith("http://"))
                     this.URL = URL;
                else {
                     this.URL = BaseURL.concat(URL);
                }
                this.checkcookie = checkcookie;
                this.nbtest = nbtest;
 
                return runit();
        }
        public int runit(String URL, int nbtest, boolean checkcookie, int wait) throws Exception
        {
                this.wait = wait;
                return runit(URL, nbtest, checkcookie);
        }

        public int runit() throws Exception
        {

                PostMethod pm = null;
                GetMethod gm = null;
                HttpMethodBase bm = null;
                if (httpClient == null)
                     httpClient = new HttpClient();
                if (fd != null) {
                     pm = new PostMethod(URL);
                     // InputStreamRequestEntity buf = new InputStreamRequestEntity(fd);
                     // XXX: Ugly hack to test...
                     byte [] buffet = new byte[6144];
                     for (int i=0; i<buffet.length;i++)
                         buffet[i] = 'a';
                     ByteArrayRequestEntity buf = new ByteArrayRequestEntity(buffet);
                     pm.setRequestEntity(buf);
                     // pm.setRequestBody(fd);
                     pm.setHttp11(true);
                     pm.setContentChunked(true);
                     // pm.setRequestContentLength(PostMethod.CONTENT_LENGTH_CHUNKED);
                     bm = pm;
                } else if (post != null) {
                     pm = new PostMethod(URL);
                     pm.setRequestEntity(new StringRequestEntity(post,
                                                            "application/x-www-form-urlencoded",
                                                            "UTF8"));
                     bm = pm;
                } else {
                     gm = new GetMethod(URL);
                     bm = gm;
                } 
                if (user != null) {
                     Credentials cred = new UsernamePasswordCredentials(user,pass);
                     httpClient.getState().setCredentials(org.apache.commons.httpclient.auth.AuthScope.ANY, cred);
                }

                // System.out.println("Connecting to " + URL);

                Integer connectionTimeout = 40000;
                bm.getParams().setParameter("http.socket.timeout", connectionTimeout);
                bm.getParams().setParameter("http.connection.timeout", connectionTimeout);
                httpClient.getParams().setParameter("http.socket.timeout", connectionTimeout);
                httpClient.getParams().setParameter("http.connection.timeout", connectionTimeout);
                if (jsessionid != null) {
                    // System.out.println("jsessionid: " + jsessionid);
                    bm.setRequestHeader("Cookie", "JSESSIONID=" + jsessionid);
                }

                try {
                    if (gm == null) {
                        httpResponseCode = httpClient.executeMethod(pm);
                    } else {
                        httpResponseCode = httpClient.executeMethod(gm);
                    }

                    if (httpResponseCode == 200) {
                        Cookie[] cookies = httpClient.getState().getCookies();
                        // System.out.println( "Cookies: " + cookies);
                        if (cookies != null && cookies.length!=0) {
                            for (int i = 0; i < cookies.length; i++) {
                                Cookie cookie = cookies[i];
                                // System.out.println( "Cookie: " + cookie.getName() + ", Value: " + cookie.getValue());
                                if (cookie.getName().equals("JSESSIONID")) {
                                    if (jsessionid == null) {
                                        jsessionid = cookie.getValue();
                                        String nodes[] = jsessionid.split("\\.");
                                        if (nodes.length == 2) 
                                            node = nodes[1];
                                        System.out.println("cookie first time: " + jsessionid);
                                        bm.releaseConnection();
                                        return 0; // first time ok.
                                    } else {
                                        if (jsessionid.compareTo(cookie.getValue()) == 0) {
                                            // System.out.println("cookie ok");
                                            bm.releaseConnection();
                                            return 0;
                                        } else {
                                            System.out.println("cookie \"second\" time: " + cookie.getValue());
                                            System.out.println("cookie changed");
                                            bm.releaseConnection();
                                            if (checkcookie)
                                                return -1;
                                            else
                                                if (checknode) {
                                                    String nodes[] = cookie.getValue().split("\\.");
                                                    if (nodes.length != 2) {
                                                        System.out.println("Can't find node in cookie");
                                                        return -1;
                                                    }
                                                    if (nodes[1].compareTo(node) == 0) {
                                                        return 0;
                                                    } else {
                                                        System.out.println("node " + nodes[1] + " changed too");
                                                        return -1;
                                                    }
                                                } else 
                                                    return 0;
                                        }
                                    }
                                }
                            }
                        } else {
                            // Look in the response to make sure that there is a cookie.
                            int len = (int) bm.getResponseContentLength();
                            if (bm.getResponseBodyAsString(len).indexOf(jsessionid) != -1) {
                                bm.releaseConnection();
                                return 0;
                            }
                            System.out.println("No cookies");
                        }
                        Header head = bm.getResponseHeader("getRequestedSessionId");
                        if (head != null) {
                            HeaderElement[] heade = head.getElements();
                            requestedSessionId = heade[0].getValue();
                        } else {
                            requestedSessionId = null;
                        }
                    } else {
                        System.out.println("response: " + httpResponseCode);
                        System.out.println("response: " + bm.getStatusLine());
                        success = false;
                        httpClient = null;
                    }
                    // System.out.println("response:\n" + bm.getResponseBodyAsString(len)); 
                } catch(HttpException e) { 
                    e.printStackTrace();
                    success = false;
                    httpClient = null;
                }
                System.out.println("DONE: " + httpResponseCode);
                bm.releaseConnection();
                return httpResponseCode;
        }
        public void run() {
            if (rand == null)
                rand = new Random();
            try {
                sleep(delay);
            } catch (InterruptedException e) {}

            for (int i = 0; i < nbtest; i++) {
                try {
                    if (runit() != 0) {
                        success = false;
                        return;
                     }
                    sleep((int)(rand.nextInt(wait) * 10));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    success = false;
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    success = false;
                    return;
                }
            }
            System.out.println("DONE!");
        }
        public boolean getresultok() {
            return success;
        }
        public void setdelay(int delay) {
            this.delay = delay;
        }
        public void setnode(String node) {
            this.node = node;
        }
        public String getnode() {
            return(node);
        }
}
