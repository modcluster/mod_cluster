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

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;

public class  Client extends Thread {

    private String jsessionid = null;
    private String URL = null;
    private String command = null;
    private int nbtest = 10;
    private int delay = 1000;
    private boolean checkcookie = true;
    private boolean success = true;
    /*
     *  
     * Usage:
     *          java Client http://mywebserver:80/ test
     * 
     *  @param args command line arguments
     *                 Argument 0 is a URL to a web server
     *                 Argument 1 is the command to execute.
     * 
     */
    public static void main(String[] args) throws Exception
        {
                if (args.length != 2)
                {
                        System.err.println("missing command line arguments");
                        System.exit(1);
                }
                Client client = new Client();  
                client.runit(args[0], args[1], 10, true);
                client.start();
                try {
                    client.join();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
        }

        public int runit(String URL, String command, int nbtest, boolean checkcookie) throws Exception
        {
                this.URL = URL;
                this.command = command;
                this.checkcookie = checkcookie;
                this.nbtest = nbtest;
 
                return runit();
        }

        public int runit() throws Exception
        {

                HttpClient httpClient = new HttpClient(); 
                GetMethod pm = new GetMethod(URL);

                System.out.println("Connecting to " + URL);

                Integer connectionTimeout = 40000;
                pm.getParams().setParameter("http.socket.timeout", connectionTimeout);
                pm.getParams().setParameter("http.connection.timeout", connectionTimeout);
                httpClient.getParams().setParameter("http.socket.timeout", connectionTimeout);
                httpClient.getParams().setParameter("http.connection.timeout", connectionTimeout);
                if (jsessionid != null) {
                    System.out.println("jsessionid: " + jsessionid);
                    pm.setRequestHeader("Cookie", "JSESSIONID=" + jsessionid);
                }

                int httpResponseCode = 0;
                try {
                    httpResponseCode = httpClient.executeMethod(pm);
                    System.out.println("response: " + httpResponseCode);
                    System.out.println("response: " + pm.getStatusLine());
                    if (httpResponseCode == 200) {
                        Cookie[] cookies = httpClient.getState().getCookies();
                        System.out.println( "Cookies: " + cookies);
                        if (cookies != null && cookies.length!=0) {
                            for (int i = 0; i < cookies.length; i++) {
                                Cookie cookie = cookies[i];
                                System.out.println( "Cookie: " + cookie.getName() + ", Value: " + cookie.getValue());
                                if (cookie.getName().equals("JSESSIONID")) {
                                    if (jsessionid == null) {
                                        jsessionid = cookie.getValue();
                                        System.out.println("cookie first time: " + jsessionid);
                                        pm.releaseConnection();
                                        return 0; // first time ok.
                                    } else {
                                        System.out.println("cookie second time: " + jsessionid);
                                        if (jsessionid.compareTo(cookie.getValue()) == 0) {
                                            System.out.println("cookie ok");
                                            pm.releaseConnection();
                                            return 0;
                                        }
                                    }
                                }
                            }
                        } else {
                            // Look in the response to make sure that there is a cookie.
                            int len = (int) pm.getResponseContentLength();
                            if (pm.getResponseBodyAsString(len).indexOf(jsessionid) != -1) {
                                pm.releaseConnection();
                                return 0;
                            }
                            System.out.println("No cookies");
                        }
                    } else {
                        System.out.println("Not 200");
                        success = false;
                    }
                    // System.out.println("response:\n" + pm.getResponseBodyAsString(len)); 
                } catch(HttpException e) { 
                    e.printStackTrace();
                    success = false;
                }
                System.out.println("DONE: " + httpResponseCode);
                pm.releaseConnection();
                return httpResponseCode;
        }
        public void run() {
            try {
                sleep(delay);
            } catch (InterruptedException e) {}

            for (int i = 0; i < nbtest; i++) {
                try {
                    if (runit() != 0) {
                        success = false;
                        return;
                     }
                    sleep((int)(Math.random() * 1000));
                } catch (InterruptedException e) {
                    success = false;
                } catch (Exception e) {
                    success = false;
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

}
