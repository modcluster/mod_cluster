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
 * Posix signals
 */
public enum Signal
{

    /** Unknown signal  */
    UNKNOWN(    0),
    /** Hangup (POSIX).  */
    SIGHUP(     1),
    /** Interrupt (ANSI).  */
    SIGINT(     2),
    /** Quit (POSIX).  */
    SIGQUIT(    3),
    /** Illegal instruction (ANSI).  */
    SIGILL(     4),
    /** Trace trap (POSIX).  */
    SIGTRAP(    5),
    /** Abort (ANSI).  */
    SIGABRT(    6),
    /** IOT trap (4.2 BSD).  */
    SIGIOT(     6),
    /** BUS error (4.2 BSD).  */
    SIGBUS(     7),
    /** Floating-point exception (ANSI).  */
    SIGFPE(     8),
    /** Kill, unblockable (POSIX).  */
    SIGKILL(    9),
    /** User-defined signal 1 (POSIX).  */
    SIGUSR1(    10),
    /** Segmentation violation (ANSI).  */
    SIGSEGV(    11),
    /** User-defined signal 2 (POSIX).  */
    SIGUSR2(    12),
    /** Broken pipe (POSIX).  */
    SIGPIPE(    13),
    /** Alarm clock (POSIX).  */
    SIGALRM(    14),
    /** Termination (ANSI).  */
    SIGTERM(    15),
    /** Stack fault.  */
    SIGSTKFLT(  16),
    /** Child status has changed (POSIX).  */
    SIGCHLD(    17),
    /** Continue (POSIX).  */
    SIGCONT(    18),
    /** Stop, unblockable (POSIX).  */
    SIGSTOP(    19),
    /** Keyboard stop (POSIX).  */
    SIGTSTP(    20),
    /** Background read from tty (POSIX).  */
    SIGTTIN(    21),
    /** Background write to tty (POSIX).  */
    SIGTTOU(    22),
    /** Urgent condition on socket (4.2 BSD).  */
    SIGURG(     23),
    /** CPU limit exceeded (4.2 BSD).  */
    SIGXCPU(    24),
    /** File size limit exceeded (4.2 BSD).  */
    SIGXFSZ(    25),
    /** Virtual alarm clock (4.2 BSD).  */
    SIGVTALRM(  26),
    /** Profiling alarm clock (4.2 BSD).  */
    SIGPROF(    27),
    /** Window size change (4.3 BSD, Sun).  */
    SIGWINCH(   28),
    /** I/O now possible (4.2 BSD).  */
    SIGIO(      29),
    /** Power failure restart (System V).  */
    SIGPWR(     30),
    /** Bad system call.  */
    SIGSYS(     31);


    private int value;
    private Signal(int v)
    {
        value = v;
    }

    public int valueOf()
    {
        return value;
    }

    public static Signal valueOf(int value)
    {
        for (Signal e : values()) {
            if (e.value == value)
                return e;
        }
        return UNKNOWN;
    }

}
