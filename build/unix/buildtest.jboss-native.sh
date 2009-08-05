#!/bin/sh
# Copyright(c) 2008 Red Hat Middleware, LLC,
# and individual contributors as indicated by the @authors tag.
# See the copyright.txt in the distribution for a
# full listing of individual contributors.
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2 of the License, or (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library in the file COPYING.LIB;
# if not, write to the Free Software Foundation, Inc.,
# 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
#
# @author Jean-Frederic Clere
#
# $1 : Base directory for the test. 
# $2 : Directory where the package was built.
# $3 : Directory where downloaded packages are stored.
# $4 : Name or IP to use for the tests.

base=$1
root=$2
build_cache_dir=$3
IPLOCAL=$4

# Copy java
if [ ! -d ${base}/${root}/${JAVA_HOME} ]; then
  mkdir -p ${base}/${root}/`dirname ${JAVA_HOME}`
  cp -rp $JAVA_HOME ${base}/${root}/${JAVA_HOME}
fi

# Install jbossas
URL=http://surfnet.dl.sourceforge.net/sourceforge/jboss/jboss-4.2.0.GA.zip
FILE=`basename $URL`
mkdir -p $build_cache_dir
if [ ! -f ${build_cache_dir}/$FILE ]; then
  (cd ${build_cache_dir}
  wget --tries=0 --retry-connrefused $URL
  )
fi
mkdir ${base}/${root}/jbossas
(cd ${base}/${root}/jbossas
unzip ${build_cache_dir}/${FILE}
)
JBOSSDIR=`ls ${base}/${root}/jbossas`

# Copy the package to test.
cp -rp ${root}/bin ${base}/${root}/jbossas/${JBOSSDIR}

# Add the right JAVA_HOME
echo "JAVA_HOME=$JAVA_HOME" >> ${base}/${root}/jbossas/${JBOSSDIR}/bin/run.conf
echo "export JAVA_HOME" >> ${base}/${root}/jbossas/${JBOSSDIR}/bin/run.conf

# Add LD_LIBRARY_PATH
echo "LD_LIBRARY_PATH=/jbossas/${JBOSSDIR}/bin/native" >> ${base}/${root}/jbossas/${JBOSSDIR}/bin/run.conf
echo "export LD_LIBRARY_PATH" >> ${base}/${root}/jbossas/${JBOSSDIR}/bin/run.conf

# Create our shutdown wrapper.
echo "#!/bin/sh" > ${base}/${root}/jbossas/${JBOSSDIR}/bin/jshutdown.sh
echo "DIRNAME=\`dirname \$0\`" >> ${base}/${root}/jbossas/${JBOSSDIR}/bin/jshutdown.sh
echo ". \$DIRNAME/run.conf"  >> ${base}/${root}/jbossas/${JBOSSDIR}/bin/jshutdown.sh
echo "\$DIRNAME/shutdown.sh -S -s $IPLOCAL" >> ${base}/${root}/jbossas/${JBOSSDIR}/bin/jshutdown.sh
chmod a+x ${base}/${root}/jbossas/${JBOSSDIR}/bin/jshutdown.sh

# start the jboss
sudo /usr/sbin/chroot ${base}/${root} jbossas/${JBOSSDIR}/bin/run.sh -b ${IPLOCAL} > ${base}/${root}/jboss.out.txt &
sleep 60

grep "APR capabilities:" ${base}/${root}/jboss.out.txt
if [ $? -ne 0 ]; then
  sudo /usr/sbin/chroot ${base}/${root} jbossas/${JBOSSDIR}/bin/jshutdown.sh
  echo "Failed APR not loaded"
  exit 1
fi

curl -v http://${IPLOCAL}:8080/ | grep "Welcome to JBoss" 2>/dev/null 
if [ $? -ne 0 ]; then
  sudo /usr/sbin/chroot ${base}/${root} jbossas/${JBOSSDIR}/bin/jshutdown.sh
  echo "Failed Connection doesn't work"
  exit 1
fi

# Stop the jboss
sudo /usr/sbin/chroot ${base}/${root} jbossas/${JBOSSDIR}/bin/jshutdown.sh
