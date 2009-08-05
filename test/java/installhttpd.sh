#!/bin/sh
#
# Script to install httpd in $HOME and configure it for mod_cluster tests.
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

HOME=`pwd`
export HOME
files="opt/jboss/httpd/sbin/apachectl opt/jboss/httpd/httpd/conf/httpd.conf /opt/jboss/httpd/sbin/envvars"
for FILE in `echo $files`
do
  file=${HOME}/$FILE
  echo "$file"
  cp -p $file $file.new
  sed "s:opt/jboss/httpd:${HOME}/opt/jboss/httpd:" $file > $file.new
  mv $file $file.`date +%y%m%d.%H%M%S`.org
  mv $file.new $file
done
# Arrange apachectl
file=$HOME/opt/jboss/httpd/sbin/apachectl
cp -p $file $file.new
echo "s:\$HTTPD -k \$ARGV:\$HTTPD -k \$ARGV -d ${HOME}/opt/jboss/httpd/httpd:" > sed.cmd
sed -f sed.cmd $file > $file.new
mv $file $file.`date +%y%m%d.%H%M%S`.1.org
mv $file.new $file

# Arrange httpd.conf
file=$HOME/opt/jboss/httpd/httpd/conf/httpd.conf
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
