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
tools="/bin/bash /lib/ld-linux.so.2 /usr/bin/strace /etc/passwd /etc/group /etc/hosts"
depfiles="/bin/sh /usr/bin/test /usr/bin/dirname /bin/basename /bin/uname /bin/grep"
# tools are the tools we need for the tests.
# depfiles are files the packages needed (to be generated).
files="$tools $depfiles"

# Those are dependencies (to be generated).
toolpackages="shadow-utils"
deppackages="glibc sqlite expat e2fsprogs-libs zlib"
packages="$toolpackages $deppackages"

# Copy library need by one file
# $1 : Directory where to put the libraries.
# $2 : The executable file to process.
Processlibs()
{
  for lib in `sudo -b /usr/bin/ldd $2 | awk 'NF==4 { print $3 } ' | sort -u`
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
   sudo -b cp $2 $1/$dir
}

#
# Copy all the files of a package.
# $1 : Directory where to put the libraries.
# $2 : name of the package
Processpackage()
{
  for file in `rpm -ql $2`
  do
    Processfile $1 $file
  done
}

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

# mount proc
mkdir -p $root/proc
sudo -b mount -t proc proc $root/proc

# Create needed device
mkdir -p $root/dev
sudo -b mknod $root/dev/null c 1 3
sudo -b mknod $root/dev/random c 1 8
