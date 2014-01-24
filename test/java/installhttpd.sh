#!/bin/sh
#
# Script to install httpd in $BASELOC and configure it for mod_cluster tests.
# $1: IP address to listen (both for normal requests and cluster management ones.
# $2: IP address for the advertise
# $3: sub network to accept cluster management requests.
# $4: Use a binaries from a directory.
# $5: version to build.
# $6: root to find the binaries.
IP=localhost
ADVIP=224.0.1.105
SUBIP=127.0.0.1
BUILDTEST=N
build_version=1.0.0.dev
root=.
if [ "x$1" != "x" ]
then
  IP=$1
fi
if [ "x$2" != "x" ]
then
  ADVIP=$2
fi
if [ "x$3" != "x" ]
then
  SUBIP=$3
fi
if [ "x$4" != "x" ]
then
  BUILDTEST=$4
fi
if [ "x$5" != "x" ]
then
  build_version=$5
fi
if [ "x$6" != "x" ]
then
  root=$6
fi

#
# Set the platform and arch for the download bundles.
if [  "x" = "x$BUILD_CPU" ]; then
  BUILD_CPU=`uname -m`
fi
case ${BUILD_CPU} in
  sun4u*)
    BUILD_CPU=sparcv9
  ;;
  i86pc*)
    BUILD_CPU=x86
  ;;
  i[3-6]86*)
    BUILD_CPU=x86
  ;;
  x86_64*)
    BUILD_CPU=x64
  ;;
  ia64*)
    BUILD_CPU=i64
  ;;
  9000/800*)
    BUILD_CPU=parisc2
  ;;
  Power*)
    BUILD_CPU=ppc
  ;;
esac

if [  "x" = "x$BUILD_SYS" ]; then
  BUILD_SYS=`uname -s`
fi
case ${BUILD_SYS} in
    Linux*)
    BUILD_SYS="linux2"
    ;;
    SunOS*)
    BUILD_SYS="solaris"
    ;;
    Darwin*)
    BUILD_SYS="macosx"
    ;;
    HP-UX*)
    BUILD_SYS="hpux"
    ;;
    CYGWIN*)
    BUILD_SYS=windows
    ;;
esac

# Display what we are going to do.
echo "on ${BUILD_SYS} ${BUILD_CPU}"
BUILD_TAG=${BUILD_SYS}-${BUILD_CPU}
EXT=tar.gz
BASEHTTPD=opt/jboss/httpd
BASEHTTPDCONF=opt/jboss/httpd/httpd/conf
BASEHTTPDSBIN=opt/jboss/httpd/sbin
BASEHTTPDBIN=opt/jboss/httpd/bin
BASEHTTPDBUILD=opt/jboss/httpd/htdocs/build
case $BUILD_TAG in
   *hpux-parisc2*)
      BASE=mod_cluster-hp-ux-9000_800
      ;;
   *hpux-i64*)
      BASE=mod_cluster-hp-ux-ia64
      ;;
   *linux2-x86*)
      BASE=mod_cluster-linux-i686
      ;;
   *linux2-i64*)
      BASE=mod_cluster-linux-ia64
      ;;
   *linux2-x64*)
      BASE=mod_cluster-linux-x86_64
      ;;
   *solaris-sparcv9*)
      BASE=mod_cluster-solaris-sparc
      ;;
   *solaris-x86*)
      BASE=mod_cluster-solaris-x86
      ;;
   *windows*)
      BASE=mod_cluster-windows
      EXT=zip
      BASEHTTPD=httpd-2.2
      BASEHTTPDCONF=httpd-2.2/conf
      BASEHTTPDSBIN=httpd-2.2/bin
      ;;
esac
#PACKVER=rhel-httpd-2.2.8-1.el5s2
PACKVER=mod_cluster-${build_version}

# Something like (note don't use ssl for the moment.
# http://hudson.qa.jboss.com/hudson/view/Native/job/mod_cluster-linux-x86_64/lastSuccessfulBuild/artifact/jbossnative/build/unix/output/rhel-httpd-2.2.8-1.el5s2-linux2-x64-ssl.tar.gz
# http://hudson.qa.jboss.com/hudson/view/Native/job/mod_cluster-solaris-x86/lastSuccessfulBuild/artifact/jbossnative/build/unix/output/mod_cluster-1.0.0.dev-solaris-x86.tar.gz
# The result of the build is something like:
# /qa/services/hudson/hudson_workspace/workspace/mod_cluster-linux-i686/jbossnative/build/unix/output/mod_cluster-1.0.0.dev-linux2-x86.tar.gz
#
TARBALL=http://hudson.qa.jboss.com/hudson/view/Native/job/${BASE}/lastSuccessfulBuild/artifact/jbossnative/build/unix/output/${PACKVER}-${BUILD_SYS}-${BUILD_CPU}.${EXT}

if [ "x${BUILD_SYS}" = "xwindows" ]; then
  BASELOC=`ant base | grep echo | sed 's:\[echo\]::' | sed 's:^ *::'`
else
  BASELOC=`ant base | grep echo | sed 's:\[echo\]::' | sed 's:^ *::' | sed 's: :/:g'`
fi

REMOVE=true
if [ "x${BASELOC}" = "x" ]
then
  BASELOC=`pwd`
  echo "Using current dir for Base: $BASELOC !!!"
  if [ ! -f ${PACKVER}-${BUILD_SYS}-${BUILD_CPU}.${EXT} ]
  then
    ant downloadfile -Dsourcefile=${TARBALL} -Ddestfile=${PACKVER}-${BUILD_SYS}-${BUILD_CPU}.${EXT} -Ddestdir="$BASELOC"
    if [ ! -f ${PACKVER}-${BUILD_SYS}-${BUILD_CPU}.${EXT} ]
    then
      REMOVE=false
    fi 
  fi
else
  if [  "x${BUILDTEST}" = "xN" ]
  then
    # rm -f ${PACKVER}-${BUILD_SYS}-${BUILD_CPU}.${EXT}
    if [ ! -f ${PACKVER}-${BUILD_SYS}-${BUILD_CPU}.${EXT} ]
    then
       ant downloadfile -Dsourcefile=${TARBALL} -Ddestfile=${PACKVER}-${BUILD_SYS}-${BUILD_CPU}.${EXT} -Ddestdir="$BASELOC"
     else
       echo "Warning the file ${PACKVER}-${BUILD_SYS}-${BUILD_CPU}.${EXT} is reused"
     fi
  else
    echo "Using $root (no download)"
    EXT=file
  fi
fi
TARBALL=`pwd`/${PACKVER}-${BUILD_SYS}-${BUILD_CPU}.${EXT}

export BASELOC
echo "Base is: $BASELOC !!!"

# Clean previous install
if $REMOVE
then
  rm -rf "$BASELOC/$BASEHTTPD"
fi
case ${EXT} in
  file)
    echo "copying the install from $root to $BASELOC"
    cp -rp $root/* "$BASELOC"
    case $BUILD_TAG in
      *windows*)
        EXT=zip
        ;;
      *)
        EXT=tar.gz
        ;;
    esac
    ;;
  tar.gz)
    (cd $BASELOC
     gzip -dc ${TARBALL} | tar xvf -
    )
    ;;
  *)
    (cd "$BASELOC"
     unzip ${TARBALL}
    )
    ;;
esac

INSTWIN=false
case ${EXT} in
  tar.gz)
    # Arrange the installed files
    if [ -x ${BASELOC}/${BASEHTTPDSBIN}/installhome.sh ]; then
      # Use / test the installhome script
      ${BASELOC}/${BASEHTTPDSBIN}/installhome.sh
    else
      if [ -f ${BASEHTTPDSBIN}/apxs ]; then
         files="${BASEHTTPDSBIN}/apachectl ${BASEHTTPDCONF}/httpd.conf ${BASEHTTPDSBIN}/envvars ${BASEHTTPDSBIN}/apxs ${BASEHTTPDBUILD}/config_vars.mk"
         echo "httpd-2.2.x"
      else
         files="${BASEHTTPDSBIN}/apachectl ${BASEHTTPDCONF}/httpd.conf ${BASEHTTPDSBIN}/envvars ${BASEHTTPDBIN}/apxs ${BASEHTTPDBUILD}/config_vars.mk"
         echo "httpd-2.4.x"
      fi
      for FILE in `echo $files`
      do
        file=${BASELOC}/$FILE
        echo "$file"
        cp -p $file $file.new
        sed "s:${BASEHTTPD}:${BASELOC}/${BASEHTTPD}:" $file > $file.new
        mv $file $file.`date +%y%m%d.%H%M%S`.org
        mv $file.new $file
      done
      # Arrange apachectl
      file=$BASELOC/${BASEHTTPDSBIN}/apachectl
      cp -p $file $file.new
      echo "s:\$HTTPD -k \$ARGV:\$HTTPD -k \$ARGV -d ${BASELOC}/${BASEHTTPD}/httpd:" > sed.cmd
      sed -f sed.cmd $file > $file.new
      mv $file $file.`date +%y%m%d.%H%M%S`.1.org
      mv $file.new $file
    fi
    ;;
  *)
    # Arrange the installed files
    (cd "$BASELOC/httpd-2.2/bin"
     ./installconf.bat
    )
    INSTWIN=true
    ;;
esac

#
# Arrange httpd.conf
file="$BASELOC/${BASEHTTPDCONF}/httpd.conf"
cp -p "$file" "$file.new"
grep MOD_CLUSTER_ADDS "$file"
if [ $? -ne 0 ]
then
  echo "s/Listen 80.*/Listen @IP@:8000/" > sed.cmd
  sed -f sed.cmd "$file" > "$file.new"
else
  # Uncomment out conf stuff
  echo "s/#ServerAdvertise/ServerAdvertise/" > sed.cmd
  echo "s/#Advertise/Advertise/" >> sed.cmd
  if $INSTWIN
  then
    # windoze has 8080 and already the MOD_CLUSTER_ADDS.
    echo "s/Listen 80.*/Listen @IP@:8000/" >> sed.cmd
  else
    echo "s/127.0.0.1:6666/@IP@:6666/" >> sed.cmd
    echo "s/127.0.0/@SUBIP@/" >> sed.cmd
  fi
  sed -f sed.cmd "$file" > "$file.new"
fi

grep MOD_CLUSTER_ADDS "$file"
if [ $? -ne 0 ]
then
  cat >> "$file.new" <<EOF
<IfModule manager_module>
  Listen @IP@:6666
  ManagerBalancerName mycluster
  <VirtualHost @IP@:6666>
    <Directory />
     Order deny,allow
     Deny from all
     Allow from @SUBIP@
    </Directory>

    KeepAliveTimeout 300
    MaxKeepAliveRequests 0
    ServerAdvertise on http://@IP@:6666
    AdvertiseFrequency 5
    AdvertiseSecurityKey secret
    AdvertiseGroup @ADVIP@:23364

    <Location /mod_cluster_manager>
       SetHandler mod_cluster-manager
       Order deny,allow
       Deny from all
       Allow from @SUBIP@
    </Location>

  </VirtualHost>
</IfModule>
EOF
fi

# Add rewrite tests and UseAlias
# Add ProxyPass to test MODCLUSTER-274 (2 should be enough :)).
cat >> "$file.new" <<EOF
ProxyPass /testappli1 http://testhost1:8080/
ProxyPass /testappli2 http://testhost2:8080/
ProxyPass /testappli3 http://testhost3:8080/
ProxyPass /testappli4 http://testhost4:8080/
LoadModule rewrite_module modules/mod_rewrite.so
RewriteEngine On
RewriteCond %{HTTP_HOST} ^cluster\.domain\.com [NC]
RewriteRule ^/$ /myapp/MyCount [PT]

RewriteCond %{HTTP_HOST} ^cluster\.domain\.org [NC]
RewriteRule ^/(.*)$ balancer://mycluster/myapp/\$1 [L,PT]

RewriteCond %{HTTP_HOST} ^cluster\.domain\.net [NC]
RewriteRule ^/test/(.*)$ balancer://mycluster/myapp/\$1 [L,P]

RewriteCond %{HTTP_HOST} ^cluster\.domain\.info [NC]
RewriteRule ^/?([^/.]+)/(.*)$ balancer://mycluster/test/\$2?partnerpath=/\$1 [P,QSA]

UseAlias 1
MaxContext 150
MaxHost 130
Maxnode 40
Maxjgroupsid 40
EnableOptions
EOF
echo "s/@IP@/${IP}/" > sed.cmd
echo "s/@ADVIP@/${ADVIP}/" >> sed.cmd
echo "s/@SUBIP@/${SUBIP}/" >> sed.cmd
sed -f sed.cmd "$file.new" > "$file.new.1"

# replace httpd.conf by the new file.
mv "$file" "$file.`date +%y%m%d.%H%M%S`.1.org"
mv "$file.new.1" "$file"

# restore the execute permissions.
chmod a+x "$BASELOC/${BASEHTTPDSBIN}"/*
chmod a+x "$BASELOC/${BASEHTTPD}"/bin/*
case $BUILD_TAG in
   *windows*)
     # The service is run as Administrators/SYSTEM
     chown -R Administrators "$BASELOC/httpd-2.2"
     chgrp -R SYSTEM "$BASELOC/httpd-2.2"
     ;;
esac
