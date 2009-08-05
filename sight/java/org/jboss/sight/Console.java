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
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import java.io.EOFException;
import java.io.IOException;

/**
 * System Console wrapper
 *
 * @author Mladen Turk
 *
 */
public final class Console
{
    private static final String msgReleased  = "Console was already released";
    private static final Semaphore available = new Semaphore(1, true);
    private static final ReentrantLock lock  = new ReentrantLock(true);
    private long INSTANCE = 0L;

    private static native long      alloc0()
                                        throws OutOfMemoryError;
    private static native void      open0(long instance);
    private static native void      close0(long instance);
    private static native void      echo0(long instance, boolean on);
    private static native void      kill3();
    private static native void      stitle0(String title);
    private static native String    gtitle0();
    private static native String    gets0(long instance)
                                        throws IOException;
    private static native int       getc0(long instance)
                                        throws IOException;
    private static native void      putc0(long instance, int ch)
                                        throws IOException;
    private static native void      flush0(long instance)
                                        throws IOException;
    private static native void      puts0(long instance, String str)
                                        throws IOException;

    private static native int       attach0(int pid);
    private static native int       denable0(long instance);
    private static native void      ddisable0(long instance);

    protected static final int CR   = 13;
    protected static final int LF   = 10;

    /**
     * Attach to current desktop.
     * This flag is usable only on Windows for service
     * applications
     */
    public static boolean  ATTACH_TO_DESKTOP = false;

    private void close(int err)
        throws OperatingSystemException
    {
        if (err != Error.APR_SUCCESS) {
            close0(INSTANCE);
            INSTANCE = 0;
            throw new OperatingSystemException(Error.getError(err));
        }
    }

    private Console()
        throws OutOfMemoryError, OperatingSystemException
    {
        INSTANCE = alloc0();
        if (ATTACH_TO_DESKTOP) {
            close(denable0(INSTANCE));
        }
        open0(INSTANCE);
    }

    private Console(int pid)
        throws OutOfMemoryError, OperatingSystemException
    {
        INSTANCE = alloc0();
        if (ATTACH_TO_DESKTOP) {
            close(denable0(INSTANCE));
        }
        close(attach0(pid));
        open0(INSTANCE);
    }

    /**
     * Object finalize callback.
     * Called by the garbage collector on an object when garbage
     * collection determines that there are no more references to the object.
     */
    protected final void finalize()
    {
        if (INSTANCE != 0) {
            close0(INSTANCE);
            INSTANCE = 0;
            try {
                available.release();
            }
            catch (Exception e) {
                // Toss any error
            }
        }
    }

    public static Console acquire()
        throws InterruptedException, OutOfMemoryError, OperatingSystemException
    {
        Console con = null;
        available.acquire();
        try {
            con = new Console();
        }
        finally {
            if (con == null)
                available.release();
        }
        return con;
    }

    public static Console acquire(int pid)
        throws InterruptedException, OutOfMemoryError, OperatingSystemException
    {
        Console con = null;
        available.acquire();
        try {
            con = new Console(pid);
        }
        finally {
            if (con == null)
                available.release();
        }
        return con;
    }

    /**
     * Prints a stack trace of all threads to the standard error stream.
     * This method is used only for debugging.
     */
    public void dumpAllStacks()
        throws IOException
    {
        if (INSTANCE == 0)
            throw new EOFException(msgReleased);
        lock.lock();
        try {
            kill3();
        }
        finally {
            lock.unlock();
        }
    }

    public void release()
        throws IOException
    {
        if (INSTANCE != 0) {
            close0(INSTANCE);
            INSTANCE = 0;
            available.release();
        }
        else
            throw new EOFException(msgReleased);
    }

    /**
     * Sets the echo mode for the Console.
     * @param on If <code>true</code> the echo will be
     *           enabled.
     */
    public void setEcho(boolean on)
        throws IOException
    {
        if (INSTANCE == 0)
            throw new EOFException(msgReleased);
        lock.lock();
        try {
            echo0(INSTANCE, on);
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Sets the title for the current console window.
     * @param title String that contains the string to be
     *              displayed in the title bar of the console window.
     */
    public void setTitle(String title)
        throws IOException
    {
        if (INSTANCE == 0)
            throw new EOFException(msgReleased);
        lock.lock();
        try {
            stitle0(title);
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves the title for the current console window.
     * @return Current console window title
     */
    public String getTitle()
        throws IOException
    {
        if (INSTANCE == 0)
            throw new EOFException(msgReleased);
        lock.lock();
        try {
            return gtitle0();
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Read a line from the console untill EOL
     * @return Readed line
     */
    public String readln()
        throws IOException
    {
        if (INSTANCE == 0)
            throw new EOFException(msgReleased);
        lock.lock();
        try {
            return gets0(INSTANCE);
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Read a single character from the Console
     * @return Readed char
     */
    public int read()
        throws IOException
    {
        if (INSTANCE == 0)
            throw new EOFException(msgReleased);
        lock.lock();
        try {
            return getc0(INSTANCE);
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Write a single character to the Console
     * @param ch A character to write.
     */
    public void write(int ch)
        throws IOException
    {
        if (INSTANCE == 0)
            throw new EOFException(msgReleased);
        lock.lock();
        try {
            putc0(INSTANCE, ch);
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Print a string. If the argument is null then the string
     * "null" is printed. Otherwise, the string's characters
     * are converted into bytes according to the platform's default
     * character encoding, and these bytes are written in exactly
     * the manner of the write(int) method.
     * @param str The String to be printed.
     */
    public void print(String str)
        throws IOException
    {
        if (INSTANCE == 0)
            throw new EOFException(msgReleased);
        lock.lock();
        try {
            if (str != null)
                puts0(INSTANCE, str);
            else
                puts0(INSTANCE, "null");
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Terminate the current line by writing the line separator string.
     */
    public void println()
        throws IOException
    {
        if (INSTANCE == 0)
            throw new EOFException(msgReleased);
        lock.lock();
        try {
            if (OS.IS_WINDOWS)
                putc0(INSTANCE, CR);
            putc0(INSTANCE, LF);
            flush0(INSTANCE);
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Print a String and then terminate the line. This method behaves as
     * though it invokes <code>print(String)</code> and then
     * <code>println()</code>.
     * @param str The String to be printed.
     */
    public void println(String str)
        throws IOException
    {
        if (INSTANCE == 0)
            throw new EOFException(msgReleased);
        lock.lock();
        try {
            puts0(INSTANCE, str);
            if (OS.IS_WINDOWS)
                putc0(INSTANCE, CR);
            putc0(INSTANCE, LF);
            flush0(INSTANCE);
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Flush Console output buffer
     */
    public void flush()
        throws IOException
    {
        if (INSTANCE == 0)
            throw new EOFException(msgReleased);
        lock.lock();
        try {
            flush0(INSTANCE);
        }
        finally {
            lock.unlock();
        }
    }

}
