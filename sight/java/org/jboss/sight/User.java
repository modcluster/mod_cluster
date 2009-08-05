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

import java.util.Iterator;

/**
 * User Information.
 * <br />
 * @author Mladen Turk
 *
 */

public final class User
{
    private long                    INSTANCE;
    private static native User[]    users0()
                                        throws OutOfMemoryError;
    private static native User[]    who0()
                                        throws OutOfMemoryError;
    private static native void      free0(long instance);
    private static native long      getuser0(User thiz, long sid)
                                        throws OutOfMemoryError;

    private User(int dummy, long instance)
    {
        INSTANCE = instance;
    }

    private User()
    {
        INSTANCE = 0;
    }

    public static User fromId(long userId)
        throws NullPointerException, OutOfMemoryError
    {
        User user = new User();
        user.INSTANCE = getuser0(user, userId);
        if (user.INSTANCE == 0) {
            throw new NullPointerException();
        }
        else
            return user;
    }

    public User(long userId)
        throws OutOfMemoryError
    {
        INSTANCE = getuser0(this, userId);
    }

    /**
     * Object finalize callback.
     * Called by the garbage collector on an object when garbage
     * collection determines that there are no more references to the object.
     */
    protected final void finalize()
    {
        if (INSTANCE != 0)
            free0(INSTANCE);
        INSTANCE = 0;
    }

    /**
     * String that specifies the name of the user account.
     */
    public String       Name;

    /**
     * String that contains the full name of the user. This string can be
     * a null string.
     */
    public String       FullName;

    /**
     * String that contains a comment associated with the user. This string can
     * be a null string.
     */
    public String       Comment;

    /**
     * Specifies the identifier (pid_t or PSID) of the user.
     */
    public long         Id;

    /**
     * String that contains a comment users Home directory.
     */
    public String       Home;

   /**
     * Get the list of all users
     * @return Array of Users.
     */
    public static UserIterator getUsers()
        throws OutOfMemoryError
    {
        return new UserIterator(users0());
    }

   /**
     * Get the list of all users currently logged on.
     * @return Array of Users.
     */
    public static UserIterator getLoggedUsers()
        throws OutOfMemoryError
    {
        return new UserIterator(who0());
    }

}
