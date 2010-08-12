/*
 *  Copyright(c) 2010 Red Hat Middleware, LLC,
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
 * @version $Revision: 420067 $, $Date: 2006-07-08 09:16:58 +0200 (sub, 08 srp 2006) $
 */

import javax.servlet.*;
import javax.servlet.http.*;
import java.net.URL;
import java.net.Socket;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStream;

/**
 * That servlet can be use to test if the node could reach httpd.
 */

public class testhttpd extends HttpServlet {

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        response.setContentType("text/html");

        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<body bgcolor=\"white\">");
        out.println("<head>");

        out.println("<title>" + "testhttpd" + "</title>");
        out.println("</head>");
        out.println("This test allows to check that httpd is up and configured to accept requests from the node</br>");
        out.println("The actual version doesn't support SSL so SSL won't be tested fully<br/>");
        out.println("<br/>");

        String testValue = request.getParameter("url");
        if (testValue != null) {
            out.println("Connecting to " + testValue + "</br>");
            try {
                URL u = new URL(testValue);
                Socket c = new Socket(u.getHost(), u.getPort());
                out.println("Connected to " + u.getHost() + " : " + u.getPort() + "</br>");
                PrintWriter o = new PrintWriter(c.getOutputStream());
                InputStream i = c.getInputStream();
                o.println("PING / HTTP/1.1");
                o.println("Host: " + u.getHost());
                o.println("");
                o.flush();
                byte buf[] = new byte [512];
                int ret = i.read(buf);
                if (ret >= 0) {
                    String res = new String(buf, 0, ret);
                    out.println("Result:<br/>");
                    out.println(res);
                    out.println("<br/>");
                    if (res.indexOf("PING-RSP") > 0)
                        out.println("<H1>OK</H1>");
                    else
                        out.println("<H1>FAILED</H1>");
                }
                c.close();
                out.println("<body>");
                return;
            } catch (Exception ex) {
                out.println("Failed: " + ex + "</br>");
            }
        }
       
        out.println("<br/>");
        out.println("Use: testhttpd/testhttpd?url=http://hostname:port to test<br/>");
        out.println("or   testhttpd/testhttpd?url=http://ip:port to test<br/>");
        out.println("<body>");

    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        doGet(request, response);
    }

}
