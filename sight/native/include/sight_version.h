/*
 *  SIGHT - System information gathering hybrid tool
 *
 *  Copyright(c) 2007 Red Hat Middleware, LLC,
 *  and individual contributors as indicated by the @authors tag.
 *  See the copyright.txt in the distribution for a
 *  full listing of individual contributors. 
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library in the file COPYING.LIB;
 *  if not, write to the Free Software Foundation, Inc.,
 *  59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
 *
 * @author Mladen Turk
 *
 */

#ifndef SIGHT_VERSION_H
#define SIGHT_VERSION_H

#include "apr_version.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @file sight_version.h
 * @brief
 *
 * SIGHT Version
 *
 * There are several different mechanisms for accessing the version. There
 * is a string form, and a set of numbers; in addition, there are constants
 * which can be compiled into your application, and you can query the library
 * being used for its actual version.
 *
 * Note that it is possible for an application to detect that it has been
 * compiled against a different version of SIGHT by use of the compile-time
 * constants and the use of the run-time query function.
 *
 * SIGHT version numbering follows the guidelines specified in:
 *
 *     http://apr.apache.org/versioning.html
 */

/* The numeric compile-time version constants. These constants are the
 * authoritative version numbers for SIGHT.
 */

/** major version
 * Major API changes that could cause compatibility problems for older
 * programs such as structure size changes.  No binary compatibility is
 * possible across a change in the major version.
 */
#define SIGHT_MAJOR_VERSION     1

/**
 * Minor API changes that do not cause binary compatibility problems.
 * Should be reset to 0 when upgrading SIGHT_MAJOR_VERSION
 */
#define SIGHT_MINOR_VERSION     0

/** patch level */
#define SIGHT_PATCH_VERSION     2

/**
 *  This symbol is defined for internal, "development" copies of SIGHT.
 *  This symbol will be #undef'd for releases.
 */
#undef SIGHT_IS_DEV_VERSION


/** The formatted string of APU's version */
#define SIGHT_VERSION_STRING \
     APR_STRINGIFY(SIGHT_MAJOR_VERSION) "."\
     APR_STRINGIFY(SIGHT_MINOR_VERSION) "."\
     APR_STRINGIFY(SIGHT_PATCH_VERSION)\
     SIGHT_IS_DEV_STRING

/** Internal: string form of the "is dev" flag */
#ifdef SIGHT_IS_DEV_VERSION
#define SIGHT_IS_DEV_STRING "-dev"
#else
#define SIGHT_IS_DEV_STRING ""
#endif

#ifdef __cplusplus
}
#endif

#endif /* SIGHT_VERSION_H */
