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
tools="/bin/bash /etc/passwd /etc/group /etc/hosts /etc/netconfig /etc/resolv.conf /etc/inet/ipnodes /usr/lib/ld.so.1"
depfiles="/bin/sh /usr/bin/test /usr/bin/dirname /usr/bin/basename /usr/bin/uname /usr/bin/grep /usr/bin/sh /usr/bin/expr /usr/bin/false /usr/bin/true"
# tools are the tools we need for the tests.
# depfiles are files the packages needed (to be generated).
files="$tools $depfiles"

# Those are dependencies (to be generated).
toolpackages=""
# SUNWlibC is for java.
case `/bin/uname -p` in
  sparc)
    deppackages="SUNWlibms SUNWcsl SUNWzlib SUNWlibC"
    ;;
  i386)
    deppackages="SUNWlibms SUNWcsl SUNWzlib SUNWlibC SUNWcslr"
    ;;
esac
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
# $1 : Directory where to put the libraries.
# $2 : name of the package
Processpackage()
{
  for file in `/usr/sbin/pkgchk -l $2 | grep Pathname: | awk ' { print $2 } '`
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
  major=`ls -lL /dev/$2 | awk ' { print $5 } ' | sed 's:,::'`:w
  minor=`ls -lL /dev/$2 | awk ' { print $6 } '`
  sudo mknod $1/$2 c $major $minor
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
devices="ip kmem null random rawip tcp tcp6 ticlts ticots ticotsord udp udp6 zero"
mkdir -p $root/dev
sudo chown root $root/dev

for dev in `echo $devices`
do
  Createdev $root/dev $dev
done

# create /tmp
mkdir $root/tmp
chmod a+rwx $root/tmp

# mount proc
mkdir -p $root/proc
sudo /usr/sbin/mount -F proc proc $root/proc
