/*
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
 * @version $Revision: 420067 $, $Date: 2006-07-08 09:16:58 +0200 (sub, 08 srp 2006) $
 */

import java.io.*;
import java.text.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;



/**
 * TestServlet for JBWEB-117
 */

public class JBWEB_117 extends HttpServlet {

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        response.setContentType("text/html");

        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<body bgcolor=\"white\">");
        out.println("<head>");

        out.println("<title>" + "JBWEB_117" + "</title>");
        out.println("</head>");
        out.println("<body>");


        // Read the inputstream.
        InputStream in = request.getInputStream();
        if (in!=null) {
            byte[] buff =  new byte[128];
            int i=0;
            try {
                int ret=0;
                while (ret!=-1) {
                    ret  = in.read(buff);
                    if (ret>0) {
                        i = i + ret;
                        String str = new String(buff,0,ret);
                        out.println(str);
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace(out);
                out.println("Merde bug velu!!!");
                out.println("<P>");
            }
            out.println("Size of input: " + i);
        } else {
            out.println("No input");
        }

        // Read session and create it if needed...
        HttpSession session = request.getSession(true);
    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        doGet(request, response);
    }

}
