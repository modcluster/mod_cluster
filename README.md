mod_cluster [![CI Status](https://github.com/modcluster/mod_cluster/workflows/CI/badge.svg)](https://github.com/modcluster/mod_cluster/actions)
===========

Project mod_cluster is an intelligent load balancer. It uses a communication channel to forward requests from a reverse
proxy server to one of a set of application server nodes. Unlike mod_jk and mod_proxy, mod_cluster leverages an
additional connection between the application server nodes and the reverse proxy to transmit server-side load balance
factors and lifecycle events back to the proxy. This additional feedback channel allows mod_cluster to offer a level of
intelligence and granularity not found in other load balancing solutions. There are currently two reverse proxy
implementations: a native [Apache HTTP Server](https://httpd.apache.org/) implementation and a pure Java 
[Undertow](http://undertow.io/)-based implementation.

Project mod_cluster boasts the following advantages over other httpd-based load balancers:

* Dynamic configuration of httpd workers
* Server-side load balance factor calculation
* Fine-grained web-app lifecycle control
* AJP is optional

[https://www.modcluster.io](https://www.modcluster.io)


Installation Instructions
-------------------------

### WildFly 8 and newer

These versions already ship with bundled mod_cluster. It is configured via mod_cluster subsystem.

### Tomcat 9.0 and newer

Distribution archives are provided for each Tomcat version.

1. Get the distribution archive corresponding to the intended Tomcat version by either downloading from the project
   website or if building from source located in `dist/target/` directory.
2. Download and unzip or untar the distribution archive and navigate to the extracted directory.
3. Copy the `lib/` directory to the Tomcat installation directory adding jars to its `lib/` directory. If upgrading from
   a different version, it is necessary to remove all jars copied previously.
4. Modify `server.xml` within the `conf` directory and add the mod_cluster listener as documented
   [here](https://docs.modcluster.io/). The minimal listener configuration is as follows:
   
    ```xml
    <Listener className="org.jboss.modcluster.container.tomcat.ModClusterListener" connectorPort="8080" advertiseInterfaceName="lo0"/>
    ```


Project Structure
-----------------

The Project is split up into multiple modules:

```
core (contains the implementation of container-independent core mod_cluster concepts)
container
  spi (SPI classes for container integrations, has no dependencies on a specific web container)
  tomcat-9.0 (Tomcat 9.0 container implementation)
  tomcat-10.1 (Tomcat 10.1 container implementation)
  tomcat-11.0 (Tomcat 11.0 container implementation)
load-spi (SPI classes for load metric computation)
```


Source Code
-----------

Source code for the mod_cluster project is located on GitHub:

[https://github.com/modcluster/mod_cluster](https://github.com/modcluster/mod_cluster)


Building
--------

### Servlet Container Modules

When building from source, first ensure that Maven version 3.2.5 or newer (run `mvn -version`) and JDK 17 or newer
(run `java -version`) are installed. The following command builds modules for all containers:

```
mvn clean install
```

Distribution files for Tomcat will be built in the `dist/target/` directory.

### Code Coverage Report

This project currently supports JaCoCo to generate a code coverage report bound to `verify` goal:

```
mvn clean verify -P coverage
```

The resulting report can be viewed by opening `code-coverage/target/site/jacoco-aggregate/index.html`.

Reporting Issues
----------------

Project mod_cluster uses Red Hat Jira issue tracker under MODCLUSTER project:

[https://issues.redhat.com/browse/MODCLUSTER](https://issues.redhat.com/browse/MODCLUSTER)

License
-------

* [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
