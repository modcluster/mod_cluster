package org.jboss.modcluster.container.tomcat;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

import org.apache.catalina.comet.CometEvent;
import org.apache.catalina.Context;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.container.catalina.CatalinaContext;
import org.jboss.modcluster.container.catalina.RequestListenerValveFactory;

public class TomcatContext extends CatalinaContext {

    public TomcatContext(Context context, Host host) {
        super(context, host, new RequestListenerValveFactory () {
            @Override
            public Valve createValve(ServletRequestListener listener) {
                return new RequestListenerValve(listener);
            }
        });
    }
    
    private static class RequestListenerValve extends ValveBase {
        private final ServletRequestListener listener;
        
        RequestListenerValve(ServletRequestListener listener) {
           this.listener = listener;
        }

        @Override
        public void invoke(Request request, Response response) throws IOException, ServletException {
            this.event(request, response, null);
        }

        @Override
        public void event(Request request, Response response, CometEvent event) throws IOException, ServletException {
            ServletRequestEvent requestEvent = new ServletRequestEvent(request.getContext().getServletContext(), request);
            
            this.listener.requestInitialized(requestEvent);
            
            Valve valve = this.getNext();
            
            try {
               if (event != null) {
                  valve.event(request, response, event);
               } else {
                  valve.invoke(request, response);
               }
            } finally {
               this.listener.requestDestroyed(requestEvent);
            }
        }

        /**
         * {@inheritDoc}
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
           return this.listener.hashCode();
        }

        /**
         * {@inheritDoc}
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object object) {
           if ((object == null) || !(object instanceof RequestListenerValve)) return false;
           
           RequestListenerValve valve = (RequestListenerValve) object;
           
           return this.listener == valve.listener;
        }
    }
}
