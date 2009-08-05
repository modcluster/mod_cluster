mod-cluster
===========

Instructions
------------

JBoss AS

1. Copy the exploded sar "mod-cluster.sar" directory into the deploy directory
   of a JBoss server profile.
2. Modify the server.xml within jbossweb.sar and add a clustered mode engine
   listener as documented here:
   http://www.jboss.org/mod_cluster/java/config.html


JBoss Web

1. Copy the jar file contained in the mod-cluster.sar directory into the lib
   directory of your JBoss Web installation.
2. Copy the following dependency jars into the same lib directory:

3. Modify the server.xml within the conf directory and add a non-cluster mode
   engine listener as documented here:
   http://www.jboss.org/mod_cluster/java/config.html


Additional notes for Tomcat

1. 


This software is distributed under the terms of the FSF Lesser Gnu 
Public License (see lgpl.txt).