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
 */
package org.jboss.sight;

/**
 * Error codes
 * <br/>
 * <PRE>
 * <b>APR ERROR VALUES</b>
 * APR_ENOSTAT      APR was unable to perform a stat on the file
 * APR_ENOPOOL      APR was not provided a pool with which to allocate memory
 * APR_EBADDATE     APR was given an invalid date
 * APR_EINVALSOCK   APR was given an invalid socket
 * APR_ENOPROC      APR was not given a process structure
 * APR_ENOTIME      APR was not given a time structure
 * APR_ENODIR       APR was not given a directory structure
 * APR_ENOLOCK      APR was not given a lock structure
 * APR_ENOPOLL      APR was not given a poll structure
 * APR_ENOSOCKET    APR was not given a socket
 * APR_ENOTHREAD    APR was not given a thread structure
 * APR_ENOTHDKEY    APR was not given a thread key structure
 * APR_ENOSHMAVAIL  There is no more shared memory available
 * APR_EDSOOPEN     APR was unable to open the dso object.  For more
 *                  information call apr_dso_error().
 * APR_EGENERAL     General failure (specific information not available)
 * APR_EBADIP       The specified IP address is invalid
 * APR_EBADMASK     The specified netmask is invalid
 * APR_ESYMNOTFOUND Could not find the requested symbol
 * </PRE>
 * <br/>
 * <PRE>
 * <b>APR STATUS VALUES</b>
 * APR_INCHILD        Program is currently executing in the child
 * APR_INPARENT       Program is currently executing in the parent
 * APR_DETACH         The thread is detached
 * APR_NOTDETACH      The thread is not detached
 * APR_CHILD_DONE     The child has finished executing
 * APR_CHILD_NOTDONE  The child has not finished executing
 * APR_TIMEUP         The operation did not finish before the timeout
 * APR_INCOMPLETE     The operation was incomplete although some processing
 *                    was performed and the results are partially valid
 * APR_BADCH          Getopt found an option not in the option string
 * APR_BADARG         Getopt found an option that is missing an argument
 *                    and an argument was specified in the option string
 * APR_EOF            APR has encountered the end of the file
 * APR_NOTFOUND       APR was unable to find the socket in the poll structure
 * APR_ANONYMOUS      APR is using anonymous shared memory
 * APR_FILEBASED      APR is using a file name as the key to the shared memory
 * APR_KEYBASED       APR is using a shared key as the key to the shared memory
 * APR_EINIT          Ininitalizer value.  If no option has been found, but
 *                    the status variable requires a value, this should be used
 * APR_ENOTIMPL       The APR function has not been implemented on this
 *                    platform, either because nobody has gotten to it yet,
 *                    or the function is impossible on this platform.
 * APR_EMISMATCH      Two passwords do not match.
 * APR_EBUSY          The given lock was busy.
 * </PRE>
 * <br/>
 *
 * @author Mladen Turk
 *
 */
public final class Error {


    /**
     * APR_OS_START_ERROR is where the APR specific error values start.
     */
     public static final int APR_OS_START_ERROR   = 20000;

    /**
     * APR_OS_ERRSPACE_SIZE is the maximum number of errors you can fit
     *    into one of the error/status ranges below -- except for
     *    APR_OS_START_USERERR, which see.
     */
     public static final int APR_OS_ERRSPACE_SIZE = 50000;

    /**
     * APR_OS_START_STATUS is where the APR specific status codes start.
     */
     public static final int APR_OS_START_STATUS  = (APR_OS_START_ERROR + APR_OS_ERRSPACE_SIZE);

    /**
     * APR_OS_START_USERERR are reserved for applications that use APR that
     *     layer their own error codes along with APR's.  Note that the
     *     error immediately following this one is set ten times farther
     *     away than usual, so that users of apr have a lot of room in
     *     which to declare custom error codes.
     */
    public static final int APR_OS_START_USERERR  = (APR_OS_START_STATUS + APR_OS_ERRSPACE_SIZE);

    /**
     * APR_OS_START_USEERR is obsolete, defined for compatibility only.
     * Use APR_OS_START_USERERR instead.
     */
    public static final int APR_OS_START_USEERR    = APR_OS_START_USERERR;

    /**
     * APR_OS_START_CANONERR is where APR versions of errno values are defined
     *     on systems which don't have the corresponding errno.
     */
    public static final int APR_OS_START_CANONERR  = (APR_OS_START_USERERR + (APR_OS_ERRSPACE_SIZE * 10));

    /**
     * APR_OS_START_EAIERR folds EAI_ error codes from getaddrinfo() into
     *     apr_status_t values.
     */
    public static final int APR_OS_START_EAIERR  = (APR_OS_START_CANONERR + APR_OS_ERRSPACE_SIZE);

    /**
     * APR_OS_START_SYSERR folds platform-specific system error values into
     *     apr_status_t values.
     */
    public static final int APR_OS_START_SYSERR  = (APR_OS_START_EAIERR + APR_OS_ERRSPACE_SIZE);

    /** no error. */
    public static final int APR_SUCCESS = 0;

    public static final int APR_ENOSTAT       = (APR_OS_START_ERROR + 1);
    public static final int APR_ENOPOOL       = (APR_OS_START_ERROR + 2);
    public static final int APR_EBADDATE      = (APR_OS_START_ERROR + 4);
    public static final int APR_EINVALSOCK    = (APR_OS_START_ERROR + 5);
    public static final int APR_ENOPROC       = (APR_OS_START_ERROR + 6);
    public static final int APR_ENOTIME       = (APR_OS_START_ERROR + 7);
    public static final int APR_ENODIR        = (APR_OS_START_ERROR + 8);
    public static final int APR_ENOLOCK       = (APR_OS_START_ERROR + 9);
    public static final int APR_ENOPOLL       = (APR_OS_START_ERROR + 10);
    public static final int APR_ENOSOCKET     = (APR_OS_START_ERROR + 11);
    public static final int APR_ENOTHREAD     = (APR_OS_START_ERROR + 12);
    public static final int APR_ENOTHDKEY     = (APR_OS_START_ERROR + 13);
    public static final int APR_EGENERAL      = (APR_OS_START_ERROR + 14);
    public static final int APR_ENOSHMAVAIL   = (APR_OS_START_ERROR + 15);
    public static final int APR_EBADIP        = (APR_OS_START_ERROR + 16);
    public static final int APR_EBADMASK      = (APR_OS_START_ERROR + 17);
    public static final int APR_EDSOOPEN      = (APR_OS_START_ERROR + 19);
    public static final int APR_EABSOLUTE     = (APR_OS_START_ERROR + 20);
    public static final int APR_ERELATIVE     = (APR_OS_START_ERROR + 21);
    public static final int APR_EINCOMPLETE   = (APR_OS_START_ERROR + 22);
    public static final int APR_EABOVEROOT    = (APR_OS_START_ERROR + 23);
    public static final int APR_EBADPATH      = (APR_OS_START_ERROR + 24);
    public static final int APR_EPATHWILD     = (APR_OS_START_ERROR + 25);
    public static final int APR_ESYMNOTFOUND  = (APR_OS_START_ERROR + 26);
    public static final int APR_EPROC_UNKNOWN = (APR_OS_START_ERROR + 27);
    public static final int APR_ENOTENOUGHENTROPY = (APR_OS_START_ERROR + 28);

    public static final int APR_INCHILD       = (APR_OS_START_STATUS + 1);
    public static final int APR_INPARENT      = (APR_OS_START_STATUS + 2);
    public static final int APR_DETACH        = (APR_OS_START_STATUS + 3);
    public static final int APR_NOTDETACH     = (APR_OS_START_STATUS + 4);
    public static final int APR_CHILD_DONE    = (APR_OS_START_STATUS + 5);
    public static final int APR_CHILD_NOTDONE = (APR_OS_START_STATUS + 6);
    public static final int APR_TIMEUP        = (APR_OS_START_STATUS + 7);
    public static final int APR_INCOMPLETE    = (APR_OS_START_STATUS + 8);
    public static final int APR_BADCH         = (APR_OS_START_STATUS + 12);
    public static final int APR_BADARG        = (APR_OS_START_STATUS + 13);
    public static final int APR_EOF           = (APR_OS_START_STATUS + 14);
    public static final int APR_NOTFOUND      = (APR_OS_START_STATUS + 15);
    public static final int APR_ANONYMOUS     = (APR_OS_START_STATUS + 19);
    public static final int APR_FILEBASED     = (APR_OS_START_STATUS + 20);
    public static final int APR_KEYBASED      = (APR_OS_START_STATUS + 21);
    public static final int APR_EINIT         = (APR_OS_START_STATUS + 22);
    public static final int APR_ENOTIMPL      = (APR_OS_START_STATUS + 23);
    public static final int APR_EMISMATCH     = (APR_OS_START_STATUS + 24);
    public static final int APR_EBUSY         = (APR_OS_START_STATUS + 25);

    public static final int TIMEUP            = (APR_OS_START_USERERR + 1);
    public static final int EAGAIN            = (APR_OS_START_USERERR + 2);
    public static final int EINTR             = (APR_OS_START_USERERR + 3);
    public static final int EINPROGRESS       = (APR_OS_START_USERERR + 4);
    public static final int ETIMEDOUT         = (APR_OS_START_USERERR + 5);

    /**
     * Get the last platform error.
     * @return apr_status_t the last platform error, folded into apr_status_t, on most platforms
     * This retrieves errno, or calls a GetLastError() style function, and
     *      folds it with APR_FROM_OS_ERROR.  Some platforms (such as OS2) have no
     *      such mechanism, so this call may be unsupported.  Do NOT use this
     *      call for socket errors from socket, send, recv etc!
     */
    public static native int getOsError();

    /**
     * Get the last platform socket error.
     * @return the last socket error, folded into apr_status_t, on all platforms
     * This retrieves errno or calls a GetLastSocketError() style function,
     *      and folds it with APR_FROM_OS_ERROR.
     */
    public static native int getNetworkError();

    /**
     * Return a human readable string describing the specified error.
     * @param statcode The error code the get a string for.
     * @return The error string.
    */
    public static native String getError(int statcode);

}
