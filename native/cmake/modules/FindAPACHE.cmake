# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#  APACHE_FOUND         - System has APACHE
#  APACHE_INCLUDE_DIR   - The APACHE include directory
#
#  APACHE_LOCATION      - setting this enables search for apache libraries / headers in this location

#
# Include directories
#
find_path(APACHE_INCLUDE_DIR
        NAMES httpd.h
        PATH_SUFFIXES httpd apache apache2
)

if(NOT DEFINED APACHE_MODULE_DIR)
    find_program(APXS_BIN NAMES apxs apxs2
                 PATH_SUFFIXES httpd apache apache2
    )

    if(APXS_BIN)
        EXEC_PROGRAM(${APXS_BIN}
                ARGS -q LIBEXECDIR
                OUTPUT_VARIABLE APACHE_MODULE_DIR )
    endif(APXS_BIN)
endif(NOT DEFINED APACHE_MODULE_DIR)

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(APACHE DEFAULT_MSG APACHE_INCLUDE_DIR )
mark_as_advanced(APACHE_INCLUDE_DIR)
