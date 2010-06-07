/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.modcluster.demo.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jboss.modcluster.demo.Constants;

/**
 * @author Paul Ferraro
 *
 */
public class RecordServlet extends LoadServlet
{
   /** The serialVersionUID */
   private static final long serialVersionUID = -4143320241936636855L;
   
   private static final String DESTROY = "destroy";
   private static final String TIMEOUT = "timeout";
   
   /**
    * @{inheritDoc}
    * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
    */
   @Override
   protected void service(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException
   {
      HttpSession session = request.getSession(true);
      
      boolean destroy = Boolean.valueOf(request.getParameter(DESTROY));
      
      if (destroy)
      {
         session.invalidate();
      }
      else
      {
         String timeout = request.getParameter(TIMEOUT);

         if (timeout != null)
         {
            session.setMaxInactiveInterval(Integer.valueOf(timeout));
         }
      }
      
      response.setHeader(Constants.NODE_HEADER, this.getJvmRoute());
      
      this.writeLocalName(request, response);
   }
}
