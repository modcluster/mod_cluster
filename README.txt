mod_cluster
===========

Project structure:
container-spi (contains no dependencies on a specific web container)
container
  catalina (base Tomcat/JBW container impl, based on Tomcat 5.5)
  catalina-standalone (contains ModClusterListener, used for standalone Tomcat/JBW installations)
  jbossweb (JBoss Web container impl, all versions)
  tomcat6 (Tomcat 6.0 container impl)
  tomcat7 (Tomcat 7.0 container impl)
core
ha
demo
  client
  server


Instructions
------------

JBoss AS

1. Copy the exploded sar "mod_cluster.sar" directory into the deploy directory
   of a JBoss server profile.
2. Modify the server.xml within jbossweb.sar and add a clustered mode engine
   listener as documented here:
   http://www.jboss.org/mod_cluster/java/config.html


JBoss Web / Tomcat

The tar.gz from the assembly contains the JBossWeb-Tomcat.
   mvn -P dist package create the tar.gz (after mvn install).
1. Extract the JBossWeb-Tomcat directory.

2. Copy the jar files from JBossWeb-Tomcat/lib to the tomcat lib.
   remove the mod_cluster-container-tomcat(n).jar that does correspond to
   the Tomcat you are using.
   
3. Copy the following dependency jars into the same lib directory:
  JBossWeb-Tomcat/lib/jboss-logging-jdk.jar
  JBossWeb-Tomcat/lib/jboss-logging-spi.jar
 

4. Modify the server.xml within the conf directory and add a non-cluster mode
   engine listener as documented here:
   http://www.jboss.org/mod_cluster/java/config.html

Building:
    It is possible to build a single platform build (AS7/TC7/TC6/JBossWeb(2.1.x)).
    * AS7:
      mvn -P AS7 install (it will use JBossWeb 7.0.x).
    * distribution package:
      mvn -P dist package
    * the demo is in sub projects demo/client demo/server
      mvn install in demo should be both projects.

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
