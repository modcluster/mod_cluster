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
 * Set what type of command the child process will call.
 * @param cmd The type of command.  One of:
 * <PRE>
 * APR_SHELLCMD     --  Anything that the shell can handle
 * APR_PROGRAM      --  Executable program   (default)
 * APR_PROGRAM_ENV  --  Executable program, copy environment
 * APR_PROGRAM_PATH --  Executable program on PATH, copy env
 * </PRE>
 */
public enum CommandType
{
    /** Use the shell to invoke the program */
    SHELLCMD(       0),
    /** Invoke the program directly, no copied env */
    PROGRAM(        1),
    /** Invoke the program, replicating our environment */
    PROGRAM_ENV(    2),
    /** Find program on PATH, use our environment */
    PROGRAM_PATH(   3),
    /** Use the shell to invoke the program,
     *   replicating our environment
     */
    SHELLCMD_ENV(   4);


    private int value;
    private CommandType(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static CommandType valueOf(int value)
    {
        for (CommandType e : values()) {
            if (e.value == value)
                return e;
        }
        throw new IllegalArgumentException("Invalid initializer: " + value);
    }

}
