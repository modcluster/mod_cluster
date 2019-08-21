mod_cluster [![Build Status](https://travis-ci.org/modcluster/mod_cluster.svg?branch=master)](https://travis-ci.org/modcluster/mod_cluster)
===========

Project mod_cluster is an intelligent load-balancer. It uses a communication channel to forward requests from a reverse
proxy server to one of a set of application server nodes. Unlike mod_jk and mod_proxy, mod_cluster leverages an
additional connection between the application server nodes and the reverse proxy to transmit server-side load-balance
factors and lifecycle events back to the proxy. This additional feedback channel allows mod_cluster to offer a level of
intelligence and granularity not found in other load-balancing solutions. There are currently two reverse proxy
implementations: a native [Apache HTTP Server](https://httpd.apache.org/) implementation and a pure Java 
[Undertow](http://undertow.io/)-based implementation.

Project mod_cluster boasts the following advantages over other httpd-based load-balancers:

* Dynamic configuration of httpd workers
* Server-side load balance factor calculation
* Fine grained web-app lifecycle control
* AJP is optional

[https://modcluster.io](https://modcluster.io)


Installation Instructions
-------------------------

### JBoss AS 7/WildFly 8 (or newer)

These versions already ship with bundled mod_cluster. It is configured via mod_cluster
subsystem.


### JBoss AS 6

This is the first version of AS that bundled mod_cluster, the configuration is located in
`/server/<profile>/deploy/mod_cluster.sar/META-INF` directory.


### Tomcat 7 (or newer)

Distribution archives are provided for each Tomcat version.

1. Obtain the distribution archive corresponding to the intended Tomcat version by either downloading from the project
   website or if building from source located in `dist/target/` directory.
2. Download and unzip or untar the distribution archive and navigate to the extracted directory.
3. Copy the `lib/` directory to the Tomcat installation directory adding jars to its `lib/` directory. If upgrading from
   a different version, it is necessary to remove all jars copied previously.
4. Modify `server.xml` within the `conf` directory and add the mod_cluster listener as documented
   [here](https://docs.modcluster.io/). The minimal listener configuration is as follows:
   
    ```xml
    <Listener className="org.jboss.modcluster.container.tomcat.ModClusterListener" connectorPort="8009"/>
    ```


Project Structure
-----------------

Project is split up into multiple modules:

```
core (contains the implementation of container-independent core mod_cluster concepts)
container
  spi (SPI classes for container integrations, has no dependencies on a specific web container)
  tomcat (base for Tomcat container implementations, based on Tomcat 7.0)
  tomcat8 (Tomcat 8.0 container implementation)
  tomcat85 (Tomcat 8.5 and 9.0 container implementation)
load-spi (SPI classes for load metric computation)
demo
  client
  server
```


Source Code
-----------

Source code for the mod_cluster project is located on GitHub:

[https://github.com/modcluster/mod_cluster](https://github.com/modcluster/mod_cluster)


Building
--------

### Servlet Container Modules

When building from source, first ensure that Maven version 3.2.5 or newer (run `mvn -version`) and JDK 7.0 or newer
(run `java -version`) are installed. The following command builds modules for all containers:

```
mvn install -Pdist
```

Distribution files for Tomcat and a demo application will be built in the `dist/target/` directory.


Reporting Issues
----------------

Project mod_cluster uses JBoss Jira issue tracker under MODCLUSTER project:

[https://issues.jboss.org/browse/MODCLUSTER](https://issues.jboss.org/browse/MODCLUSTER)


License
-------

This software is distributed under the terms of the GNU Lesser General Public License (see [LICENSE.txt](LICENSE.txt)).

