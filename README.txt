mod_cluster
===========

Instructions
------------

JBoss AS

1. Copy the exploded sar "mod_cluster.sar" directory into the deploy directory
   of a JBoss server profile.
2. Modify the server.xml within jbossweb.sar and add a clustered mode engine
   listener as documented here:
   http://www.jboss.org/mod_cluster/java/config.html


JBoss Web / Tomcat

1. Copy the jar file contained in the mod_cluster.sar directory into the lib
   directory of your JBoss Web installation.
   ./target/JBossWeb-Tomcat/lib/mod_cluster.jar
2. Copy the following dependency jars into the same lib directory:
  ./target/JBossWeb-Tomcat/lib/jboss-logging-jdk.jar
  ./target/JBossWeb-Tomcat/lib/jboss-logging-spi.jar
 

3. Modify the server.xml within the conf directory and add a non-cluster mode
   engine listener as documented here:
   http://www.jboss.org/mod_cluster/java/config.html

Building:
    mvn -P dist package
    ant -f build-demo.xml 

Native:

  To build the native component from the sources you need a C compiler and
  the following tools:
  m4, perl, autoconf, automake, libtool, make, patch and python.
  Of course the make and the patch must be gnu ones.
  For example on Solaris you need:
    SMCm4 (requires libsigsegv and libgcc34).
    SMCperl
    SMCautoc
    SMCautom
    SMClibt
    SMCmake
    SMCpatch
    SMCpython
    All those can be downloaded from http://www.sunfreeware.com/

This software is distributed under the terms of the FSF Lesser Gnu 
Public License (see lgpl.txt).
