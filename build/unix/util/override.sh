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

# Override a command using the environment.
# $1 script $2..n  (parameters).

basedir=`dirname $1`
cmd=`basename $1 .sh`
if [ -x  ${basedir}/${cmd}.${BUILD_SYS}.${BUILD_CPU}.sh ]; then
  run=${cmd}.${BUILD_SYS}.${BUILD_CPU}.sh
elif [ -x ${basedir}/${cmd}.${BUILD_SYS}.sh ]; then
  run=${cmd}.${BUILD_SYS}.sh
else
  run=${cmd}.sh
fi
shift
echo "Running ${basedir}/$run $@"
${basedir}/$run $@
ret=$?
if [ ${ret} -eq 2 ]; then
  echo "$run not supported on this platform"
  exit 0
elif [ ${ret} -ne 0 ]; then
  echo "$run returned ${ret}"
  exit 1
fi
