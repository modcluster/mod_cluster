/*
 *  Copyright(c) 2006 Red Hat Middleware, LLC,
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

import java.io.*;
import java.text.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;



/**
 * Example servlet showing cookies handling (counter).
 *
 */

public class MyCount extends HttpServlet {

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        response.setContentType("text/html");

        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<body bgcolor=\"white\">");
        out.println("<head>");

        String title = "sessions.title";
        out.println("<title>" + title + "</title>");
        out.println("</head>");
        out.println("<body>");

        out.println("<h3>" + title + "</h3>");
        HttpSession session = request.getSession(false);
        Integer ii = new Integer(0);
        if (session == null) {
          // Create it.
          out.println("create");
          session = request.getSession(true);
          session.setAttribute("count", ii);
        }
        out.println("sessions.id " + session.getId());
        out.println("<br>");
        out.println("sessions.created ");
        out.println(new Date(session.getCreationTime()) + "<br>");
        out.println("sessions.lastaccessed ");
        out.println(new Date(session.getLastAccessedTime()));
        out.println("sessions.count ");
        out.println(session.getAttribute("count"));

        ii = (Integer) session.getAttribute("count");
        int i = 0;
        if (ii != null)
            i = ii.intValue();
        i++;
        ii = new Integer(i); // JAVA5 : ii.valueOf(i);
        session.setAttribute("count", ii);

        out.println("<P>");
        out.println("sessions.data<br>");
        Enumeration names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement(); 
            String value = session.getAttribute(name).toString();
            out.println(name + " = " + value + "<br>");
            // response.addHeader(name, value);
        }

        out.println("<P>");
        out.print("<form action=\"");
	out.print(response.encodeURL("MyCount"));
        out.print("\" ");
        out.println("method=POST>");
        out.println("sessions.dataname");
        out.println("<input type=text size=20 name=dataname>");
        out.println("<br>");
        out.println("sessions.datavalue");
        out.println("<input type=text size=20 name=datavalue>");
        out.println("<br>");
        out.println("<input type=submit>");
        out.println("</form>");

        out.println("<P>GET based form:<br>");
        out.print("<form action=\"");
	out.print(response.encodeURL("MyCount"));
        out.print("\" ");
        out.println("method=GET>");
        out.println("sessions.dataname");
        out.println("<input type=text size=20 name=dataname>");
        out.println("<br>");
        out.println("sessions.datavalue");
        out.println("<input type=text size=20 name=datavalue>");
        out.println("<br>");
        out.println("<input type=submit>");
        out.println("</form>");

        out.print("<p><a href=\"");
	out.print(response.encodeURL("MyCount?dataname=foo&datavalue=bar"));
	out.println("\" >URL encoded </a>");
	
        out.println("</body>");
        out.println("</html>");
        
        out.println("</body>");
        out.println("</html>");

        /* Use headers */
        response.setHeader("RequestedSessionId", request.getRequestedSessionId());

    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        doGet(request, response);
    }

}
