#!/bin/sh
#
# Script to install httpd in $BASELOC and configure it for mod_cluster tests.
# $1: IP address to listen (both for normal requests and cluster management ones.
# $2: IP address for the advertise
# $3: sub network to accept cluster management requests.
IP=localhost
ADVIP=232.0.0.2
SUBIP=all
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
      ;;
esac
#PACKAGE=rhel-httpd-2.2.8-1.el5s2
PACKAGE=mod_cluster-1.0.0.dev

# Something like (note don't use ssl for the moment.
# http://hudson.qa.jboss.com/hudson/view/Native/job/mod_cluster-linux-x86_64/lastSuccessfulBuild/artifact/jbossnative/build/unix/output/rhel-httpd-2.2.8-1.el5s2-linux2-x64-ssl.tar.gz
# http://hudson.qa.jboss.com/hudson/view/Native/job/mod_cluster-solaris-x86/lastSuccessfulBuild/artifact/jbossnative/build/unix/output/mod_cluster-1.0.0.dev-solaris-x86.tar.gz
TARBALL=http://hudson.qa.jboss.com/hudson/view/Native/job/${BASE}/lastSuccessfulBuild/artifact/jbossnative/build/unix/output/${PACKAGE}-${BUILD_SYS}-${BUILD_CPU}.${EXT}
BASELOC=`ant base | grep echo | awk '{ print $2 }'`
rm -f ${PACKAGE}-${BUILD_SYS}-${BUILD_CPU}.${EXT}
wget ${TARBALL}
TARBALL=`pwd`/${PACKAGE}-${BUILD_SYS}-${BUILD_CPU}.${EXT}
export BASELOC
echo "Base is: $BASELOC !!!"
# Clean previous install
rm -rf $BASELOC/opt/jboss
case ${EXT} in
  tar.gz)
    (cd $BASELOC
     gzip -dc ${TARBALL} | tar xvf -
    )
    ;;
  *)
    (cd $BASELOC
     unzip ${TARBALL}
    )
    ;;
esac
#
# Arrange the installed files
# 
files="opt/jboss/httpd/sbin/apachectl opt/jboss/httpd/httpd/conf/httpd.conf opt/jboss/httpd/sbin/envvars"
for FILE in `echo $files`
do
  file=${BASELOC}/$FILE
  echo "$file"
  cp -p $file $file.new
  sed "s:opt/jboss/httpd:${BASELOC}/opt/jboss/httpd:" $file > $file.new
  mv $file $file.`date +%y%m%d.%H%M%S`.org
  mv $file.new $file
done
# Arrange apachectl
file=$BASELOC/opt/jboss/httpd/sbin/apachectl
cp -p $file $file.new
echo "s:\$HTTPD -k \$ARGV:\$HTTPD -k \$ARGV -d ${BASELOC}/opt/jboss/httpd/httpd:" > sed.cmd
sed -f sed.cmd $file > $file.new
mv $file $file.`date +%y%m%d.%H%M%S`.1.org
mv $file.new $file

# Arrange httpd.conf
file=$BASELOC/opt/jboss/httpd/httpd/conf/httpd.conf
cp -p $file $file.new
echo "s/Listen 80/Listen @IP@:8000/" > sed.cmd
echo "s/LoadModule proxy_balancer/#LoadModule proxy_balancer/" >> sed.cmd
sed -f sed.cmd $file > $file.new
cat >> $file.new <<EOF
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
     AdvertiseGroup @ADVIP@:23364
     </VirtualHost>
</IfModule>
EOF
echo "s/@IP@/${IP}/" > sed.cmd
echo "s/@ADVIP@/${ADVIP}/" >> sed.cmd
echo "s/@SUBIP@/${SUBIP}/" >> sed.cmd
sed -f sed.cmd $file.new > $file.new.1

# replace httpd.conf by the new file.
mv $file $file.`date +%y%m%d.%H%M%S`.1.org
mv $file.new.1 $file

# restore the execute permissions.
chmod a+x $BASELOC/opt/jboss/httpd/sbin/*
chmod a+x $BASELOC/opt/jboss/httpd/bin/*
