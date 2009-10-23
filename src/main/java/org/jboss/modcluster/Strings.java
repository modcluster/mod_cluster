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
package org.jboss.modcluster;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * @author Paul Ferraro
 */
public enum Strings
{
   ADVERTISE_START("modcluster.advertise.start"),
   CONTEXT_DISABLE("modcluster.context.disable"),
   CONTEXT_ENABLE("modcluster.context.enable"),
   CONTEXT_START("modcluster.context.start"),
   CONTEXT_STOP("modcluster.context.stop"),
   ENGINE_CONFIG("modcluster.engine.config"),
   ENGINE_STATUS("modcluster.engine.status"),
   ENGINE_STOP("modcluster.engine.stop"),
   REQUEST_SEND("modcluster.request"),
   SERVER_INIT("modcluster.server.init"),
   SERVER_START("modcluster.server.start"),
   SERVER_STOP("modcluster.server.stop"),
   SHUTDOWN("modcluster.shutdown"),
   SINGLETON_IGNORE_STOP("modcluster.singleton.ignorestop"),
   DETECT_JVMROUTE("modcluster.detect.jvmRoute"),
   DEPRECATED("modcluster.deprecated"),
   ENGINE_REMOVE_CRASHED("modcluster.engine.removeCrashed"),
   
   ERROR_ADDRESS_JVMROUTE("modcluster.error.addressJvmRoute"),
   ERROR_CONTEXT_NOT_FOUND("modcluster.error.context.notfound"),
   ERROR_DISCOVERY_ADD("modcluster.error.discovery.add"),
   ERROR_DISCOVERY_REMOVE("modcluster.error.discovery.remove"),
   ERROR_DRM("modcluster.error.drm"),
   ERROR_HOST_INVALID("modcluster.error.host.invalid"),
   ERROR_HOST_NOT_FOUND("modcluster.error.host.notfound"),
   ERROR_REQUEST_SEND_IO("modcluster.error.io"),
   ERROR_JMX_REGISTER("modcluster.error.jmxRegister"),
   ERROR_JMX_UNREGISTER("modcluster.error.jmxUnregister"),
   ERROR_NO_PROXY("modcluster.error.noproxy"),
   ERROR_ATTRIBUTE_NOT_POSITIVE("modcluster.error.nonPositiveAttribute"),
   ERROR_ATTRIBUTE_NULL("modcluster.error.nullAttribute"),
   ERROR_REQUEST_SEND_OTHER("modcluster.error.other"),
   ERROR_ARGUMENT_NULL("modcluster.error.iae.null"),
   ERROR_ARGUMENT_INVALID("modcluster.error.iae.invalid"),
   ERROR_HEADER_PARSE("modcluster.error.parse"),
   ERROR_RPC_KNOWN("modcluster.error.rpc.known"),
   ERROR_RPC_NO_RESPONSE("modcluster.error.rpc.noresp"),
   ERROR_RPC_UNKNOWN("modcluster.error.rpc.unknown"),
   ERROR_RPC_UNEXPECTED("modcluster.error.rpc.unexpected"),
   ERROR_ADVERTISE_START("modcluster.error.startListener"),
   ERROR_STATUS_COMPLETE("modcluster.error.status.complete"),
   ERROR_STATUS_UNSUPPORTED("modcluster.error.status.unsupported"),
   ERROR_ADVERTISE_STOP("modcluster.error.stopListener"),
   ERROR_STOP_OLD_MASTER("modcluster.error.stopOldMaster"),
   ERROR_REQUEST_SYNTAX("modcluster.error.syntax"),
   ERROR_UNINITIALIZED("modcluster.error.uninitialized");
   
   private static ResourceBundle resource = ResourceBundle.getBundle(Strings.class.getName());
   
   private String key;
   
   private Strings(String key)
   {
       this.key = key;
   }
   
   @Override
   public String toString()
   {
      return this.key;
   }

   /**
    * Returns the localized message using the supplied arguments.
    * @param args a variable number of arguments
    * @return a localized message
    */
   public String getString(Object... args)
   {
       String pattern = resource.getString(this.key);
       
       return (args.length == 0) ? pattern : MessageFormat.format(pattern, args);
   }
}
