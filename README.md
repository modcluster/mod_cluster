mod_cluster
===========

Project mod_cluster is a httpd-based load-balancer. It uses a communication channel to forward
requests from httpd to one of a set of application server nodes. Unlike mod_jk and mod_proxy,
mod_cluster leverages an additional connection between the application server nodes and httpd
to transmit server-side load-balance factors and lifecycle events back to httpd. This additional
feedback channel allows mod_cluster to offer a level of intelligence and granularity not found in
other load-balancing solutions.

Mod_cluster boasts the following advantages over other httpd-based load-balancers:

* Dynamic configuration of httpd workers
* Server-side load balance factor calculation
* Fine grained web-app lifecycle control
* AJP is optional

[http://www.jboss.org/mod_cluster](http://www.jboss.org/mod_cluster)


Installation
------------

### JBoss AS 7/WildFly 8+

These versions already ship with bundled mod_cluster. It is configured via mod_cluster
subsystem.


### JBoss AS 6

This is the first version of AS that bundled mod_cluster, the configuration is located in
`/server/<profile>/deploy/mod_cluster.sar/META-INF` directory.


### JBoss Web / Tomcat

The `tar.gz` from the assembly contains the `JBossWeb-Tomcat`.

1. Extract the `JBossWeb-Tomcat` directory.
2. Copy the JAR files from `JBossWeb-Tomcat/lib` to the Tomcat lib.
3. Remove the `mod_cluster-container-tomcat(n).jar` that don't correspond to
   the Tomcat version you are using.
3. Copy the `JBossWeb-Tomcat/lib/jboss-logging-jdk.jar` and `JBossWeb-Tomcat/lib/jboss-logging-spi.jar`
   dependency jars into the same `lib` directory.
4. Modify `server.xml` within the `conf` directory and add a non-cluster mode
   engine listener as documented [here](http://docs.jboss.org/mod_cluster/1.2.0/html/Quick_Start_Guide.html).


Project Structure
-----------------

```
container-spi (contains no dependencies on a specific web container)
container
  catalina (base for Tomcat container implementations, based on Tomcat 7)
  tomcat7 (Tomcat 7.0 container implementation)
  tomcat8 (Tomcat 8.0 container implementation)
  tomcat85 (Tomcat 8.5 and 9.0 milestone container implementation)
core
demo
  client
  server
```


Building
--------

### Servlet Container Modules

Before building, ensure you have Maven version 3.0 or newer (`mvn -version`) and JDK 6.0 or newer (`java -version`). 
It is possible to build modules for all containers:

    mvn install

Distribution package:

    mvn -P dist package

Dynamic load-balancing demo is located in the `/demo` directory:

    mvn install


License
-------

This software is distributed under the terms of the GNU Lesser General Public License (see [lgpl.txt](lgpl.txt)).

