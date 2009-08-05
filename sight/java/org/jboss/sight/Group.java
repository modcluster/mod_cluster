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
 * Group Information.
 * <br />
 * @author Mladen Turk
 *
 */
public final class Group
{
    private long                    INSTANCE;
    private static native Group[]   lgroups0()
                                        throws OutOfMemoryError;
    private static native Group[]   ggroups0()
                                        throws OutOfMemoryError;
    private static native void      free0(long instance);
    private static native long      getlgrp0(Group thiz, long sid)
                                        throws OutOfMemoryError;

    private Group(int dummy, long instance)
    {
        INSTANCE = instance;
    }

    private Group()
    {
        INSTANCE = 0;
    }

    public static Group fromId(long groupId)
        throws NullPointerException, OutOfMemoryError
    {
        Group group = new Group();
        group.INSTANCE = getlgrp0(group, groupId);
        if (group.INSTANCE == 0) {
            throw new NullPointerException();
        }
        else
            return group;
    }

    public Group(long groupId)
        throws OutOfMemoryError
    {
        INSTANCE = getlgrp0(this, groupId);
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
     * String that specifies the name of the group.
     */
    public String       Name;

    /**
     * String that contains a comment associated with the group.
     * This string can be a null string.
     */
    public String       Comment;

    /**
     * Specifies the identifier (gid_t or PSID) of the group.
     */
    public long         Id;

    /**
     * Set to true if the group is Local system group.
     */
    public boolean      IsLocal;

   /**
     * Get the list of all groups
     * @return Array of Users.
     */
    public static GroupIterator getGroups()
        throws OutOfMemoryError
    {
        return new GroupIterator(lgroups0());
    }

}
