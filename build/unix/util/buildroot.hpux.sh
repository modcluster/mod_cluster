#!/bin/sh
# Copyright(c) 2007 Red Hat Middleware, LLC,
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
# Build a chrootable environment.
# $1 : Directory where the chroot will takes place.
tools="/bin/bash /etc/passwd /etc/group /etc/hosts /etc/netconfig /etc/resolv.conf /usr/lib/dld.sl /usr/lib/hpux32/uld.so /usr/lib/hpux32/dld.so /usr/lib/hpux32/libm.so.1 /usr/lib/hpux32/libpthread.so.1 /usr/lib/hpux32/libnss_dns.so.1 /usr/lib/libm.2 /usr/lib/libpthread.1 /usr/lib/libnss_dns.1"
depfiles="/bin/sh /usr/bin/test /usr/bin/dirname /usr/bin/basename /usr/bin/uname /usr/bin/grep /usr/bin/sh /usr/bin/expr /usr/bin/false /usr/bin/true"
# tools are the tools we need for the tests.
# depfiles are files the packages needed (to be generated).
files="$tools $depfiles"

# Those are dependencies (to be generated).
toolpackages=""
deppackages="" #Need something?
packages="$toolpackages $deppackages"

# Copy library need by one file
# $1 : Directory where to put the libraries.
# $2 : The executable file to process.
Processlibs()
{
  for lib in `ldd $2 | awk 'NF==3 { print $3 } ' | sort -u`
  do
    Processfile $1 $lib
  done
}

#
# Copy one file
# $1 : Directory where to put the libraries.
# $2 : The executable file to process.
Processfile()
{
   dir=`dirname $2`
   mkdir -p $1/$dir
   cp $2 $1/$dir
}

#
# Copy all the files of a package.
# The package listing tool is /usr/sbin/swlist
# $1 : Directory where to put the libraries.
# $2 : name of the package
Processpackage()
{
  for file in `/usr/sbin/swlist -l file $2 | grep -v "^#"`
  do
    Processfile $1 $file
  done
}

#
# Create a device in the chrootable directory
# $1 : Directory where to create the node
# $2 : The node name in /dev
Createdev()
{
  major=`ls -lL /dev/$2 | awk ' { print $5 } ' | sed 's:,::'`
  minor=`ls -lL /dev/$2 | awk ' { print $6 } '`
  echo "/usr/sbin/mknod $1/$2 c $major $minor"
  sudo /usr/sbin/mknod $1/$2 c $major $minor
}

# Use tmp for mknod
root=$1

for file in `echo $files`
do
  Processlibs $root $file
  Processfile $root $file
done

for package in `echo $packages`
do
  Processpackage $root $package
done

# create proc
mkdir -p $root/proc

# Create needed devices
devices="ip kmem null random urandom rawip tcp tcp6 zero tcp tcp6 tlclts tlcotsod tlcots upd upd6"
mkdir -p $root/dev
sudo chown root $root/dev

for dev in `echo $devices`
do
  Createdev $root/dev $dev
done

# create /tmp
mkdir $root/tmp
chmod a+rwx $root/tmp

# Tricky part.
mkdir -p $root/usr/sbin
mkdir -p $root/var/spool/sockets/pwgr
sudo cp -rp /var/spool/pwgr $root/var/spool/
sudo cp -p /usr/sbin/pwgrd $root/usr/sbin/pwgrd-jfclere
