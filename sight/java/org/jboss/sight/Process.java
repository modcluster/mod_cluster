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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Process
 *
 * @author Mladen Turk
 *
 */

public class Process extends NativeObject
{

    private static native int   alloc0(Object thiz, long instance, int pid);
    private static native int   create0(long instance);
    private static native int   getpid0();
    private static native int[] getpids0();
    private static native int   term0(int pid, int signum);

    private static native int   pid0(long instance);
    private static native int   ioset0(long instance, int in, int out, int err);
    private static native int   cmdset0(long instance, int cmd);
    private static native int   sdetach0(long instance, boolean on);
    private static native int   dirset0(long instance, String dir);
    private static native int   uset0(long instance, String username, String password);
    private static native int   gset0(long instance, String Group);
    private static native int   exitval0(long instance);
    private static native int   exitwhy0(long instance);
    private static native int   wait0(long instance, int waithow);
    private static native int   wait1(long instance, int waithow, long timeout);
    private static native int   wait2(long instance, IProgressNotificationCallback progress, long timeout);
    private static native int   kill0(long instance, int sig);
    private static native int   exec0(long instance, String progname, String [] args, String [] env)
                                    throws NullPointerException, OperatingSystemException;
    private static native int   ciset0(long instance, long in, long parent);
    private static native int   coset0(long instance, long in, long parent);
    private static native int   ceset0(long instance, long in, long parent);
    private static native long  getios0(long instance, int which)
                                    throws NullPointerException, OperatingSystemException;
    private static native int   setiostm0(long instance, long timeout, int which);

    private static native void  notes0(long instance, int how);
    private static native int   rlimit0(long instance, int what, int soft, int hard)
                                    throws NullPointerException;

    private File redirIs    = null;
    private File redirOs    = null;
    private File redirEs    = null;

    /** Maximum number of arguments for create process call */
    public static final int MAX_ARGS_SIZE           = 1024;
    /** Maximum number of environment variables for create process call */
    public static final int MAX_ENV_SIZE            = 1024;

    private boolean allocated = false;

    private void setState(int state)
    {
        State = ProcessState.valueOf(state);
    }

    public Process()
    {
        super(0);
        Id = -1;
        create0(INSTANCE);
    }

    public Process(Pool pool)
    {
        super(pool.POOL);
        Id = -1;
        create0(INSTANCE);
    }

    public Process(int pid)
    {
        super(0);
        Id = pid;
        if (alloc0(this, INSTANCE, Id) == Error.APR_SUCCESS)
            allocated = true;
    }

    protected Process(long pool, int pid)
    {
        super(pool);
        Id = pid;
        if (alloc0(this, INSTANCE, Id) == Error.APR_SUCCESS)
            allocated = true;
    }

    protected void onDestroy()
    {
        // Nothing
    }

    /**
     * Retrieves the process identifier of the calling process.
     * @deprecated
     */
    @Deprecated public static int getCurrentProcessId()
    {
        return getpid0();
    }

    /**
     * Get the list of all process runing on the system
     * @return Array of Processes.
     * @deprecated
     */
    @Deprecated public static int[] getProcessIdList()
    {
        return getpids0();
    }

    /**
     * Get the list of all process runing on the system
     * @return Array of Processes.
     */
    public static ProcessIterator getProcesses()
    {
        return new ProcessIterator(getpids0());
    }

    /**
     * Refresh the Process.
     */
    public int refresh()
    {
        if (allocated) {
            // Only allocated processes can be refreshed.
            clear();
            State = ProcessState.DEAD;
            return alloc0(this, INSTANCE, Id);
        }
        else
            return 0;
    }

    /**
     * Process identifier.
     */
    public int          Id;

    /**
     * Process identifier of the process that created this
     * process (its parent process).
     */
    public int          ParentId;

    /**
     * Fully-qualified path of the
     * process executable.
     */
    public String       Name;

    /**
     * Base name of the specified module.
     */
    public String       BaseName;

    /**
     * Command-line arguments for this process.
     */
    public String[]     Arguments;

    /**
     * Environment variables for this process.
     */
    public String[]     Environment;

    /**
     * Number of execution threads started by the process.
     */
    public int          ThreadCount;

    public long         ReadOperationCount;

    public long         WriteOperationCount;

    public long         OtherOperationCount;

    public long         ReadTransferCount;

    public long         WriteTransferCount;

    public long         OtherTransferCount;

    public long         CreateTime;

    public long         ExitTime;

    public long         InKernelTime;

    public long         InUserTime;
    
    /**
     * Process execution state
     */
    public ProcessState State;
    
    /**
     * User id that owns the process
     */
    public long         UserId;
    /**
     * Group id that owns the process
     */
    public long         GroupId;

    /**
     * Current working directory.
     */
    public String       CurrentWorkingDirectory;

    /**
     * Get process memory usage.
     */
    public Memory getMemory()
    {
        return new Memory(POOL, Id);
    }

    /**
     * Terminate a process.
     * @param pid The id of the process to terminate.
     * @param sig How to kill the process.
     */
    public static int kill(int pid, int sig)
    {
        return term0(pid, sig);
    }

    /**
     * Terminate a process.
     * @param sig How to kill the process.
     * @deprecated
     */
    @Deprecated public int kill(int sig)
    {
        return kill0(INSTANCE, sig);
    }

    /**
     * Terminate a process.
     * @param sig How to kill the process.
     */
    public int kill(Signal sig)
    {
        return kill0(INSTANCE, sig.valueOf());
    }

    /**
     * Determine if any of stdin, stdout, or stderr should be linked to pipes
     * when starting a child process. Valid values for each pipe are:
     * <PRE>
     * NO_PIPE          -- Do not redirect the specific stream
     * FULL_BLOCK       -- Make read/write blocking.
     * FULL_NONBLOCK    -- Make read/write non blocking.
     * PARENT_BLOCK
     * CHILD_BLOCK
     * </PRE>
     * @param in Should stdin be a pipe back to the parent?
     * @param out Should stdout be a pipe back to the parent?
     * @param err Should stderr be a pipe back to the parent?
     */
    public int setIoMode(PipeIoMode in, PipeIoMode out, PipeIoMode err)
    {
        return ioset0(INSTANCE, in.valueOf(), out.valueOf(), err.valueOf());
    }

    /**
     * Set the Resource Utilization limits when starting a new process.
     * @param what Which limit to set, one of:
     * <PRE>
     *      CPU
     *      MEM
     *      NPROC
     *      NOFILE
     * </PRE>
     * @param soft Value to set the soft limit to.
     * @param hard Value to set the hard limit to.
     */
    public int setResourceLimit(ResourceLimit what, int soft, int hard)
        throws NullPointerException
    {
        return rlimit0(INSTANCE, what.valueOf(), soft, hard);
    }

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
    public int setCommandType(int cmd)
    {
        return cmdset0(INSTANCE, cmd);
    }

    /**
     * Set what type of command the child process will call.
     * @param cmd The type of command.  One of:
     * <PRE>
     * SHELLCMD     --  Anything that the shell can handle
     * PROGRAM      --  Executable program   (default)
     * PROGRAM_ENV  --  Executable program, copy environment
     * PROGRAM_PATH --  Executable program on PATH, copy env
     * </PRE>
     */
    public int setCommandType(CommandType cmd)
    {
        return cmdset0(INSTANCE, cmd.valueOf());
    }

    /**
     * Determine if the child should start in detached state.
     * @param on Should the child start in detached state?  Default is no.
     */
    public int setDetached(boolean on)
    {
        return sdetach0(INSTANCE, on);
    }

     /**
     * Detach the current process from the controlling terminal.
     * @param daemonize set to non-zero if the process should daemonize
     *                  and become a background process, else it will
     *                  stay in the foreground.
     */
    public static native int detach(int daemonize);

    /**
     * Set which directory the child process should start executing in.
     * @param dir Which dir to start in.  By default, this is the same dir as
     *            the parent currently resides in, when the createprocess call
     *            is made.
     */
    public int setWorkingDirectory(String dir)
    {
        return dirset0(INSTANCE, dir);
    }

    /**
     * Set the username used for running process
     * @param username The username used
     * @param password User password if needed. Password is needed on WIN32
     *                 or any other platform having
     *                 APR_PROCATTR_USER_SET_REQUIRES_PASSWORD set.
     */
    public int setUser(String username, String password)
    {
        return uset0(INSTANCE, username, password);
    }

    /**
     * Set the group used for running process
     * @param groupname The group name used
     */
    public int setGroup(String groupname)
    {
        return gset0(INSTANCE, groupname);
    }

    /**
     * The returned exit status of the child, if a child process
     * dies, or the signal that caused the child to die.
     * On platforms that don't support obtaining this information,
     * the status parameter will be returned as APR_ENOTIMPL.
     */
    public int getExitCode()
    {
        return exitval0(INSTANCE);
    }

    /**
    * Why the child died, the bitwise or of:
    * <PRE>
    * EXIT         -- process terminated normally
    * SIGNAL       -- process was killed by a signal
    * SIGNAL_CORE  -- process was killed by a signal, and
    *                 generated a core dump.
    * </PRE>
    */
    public ExitReason getExitReason()
    {
        return ExitReason.valueOf(exitwhy0(INSTANCE));
    }

    /**
     * Wait for a child process to die
     * @param waithow How should we wait.  One of:
     * <PRE>
     * WAIT   -- block until the child process dies.
     * NOWAIT -- return immediately regardless of if the
     *               child is dead or not.
     * </PRE>
     * @return The childs status is in the return code to this process.  It is one of:
     * <PRE>
     * APR_CHILD_DONE     -- child is no longer running.
     * APR_CHILD_NOTDONE  -- child is still running.
     * </PRE>
     */
    public int wait(WaitHow waithow)
    {
        return wait0(INSTANCE, waithow.valueOf());
    }

    /**
     * Wait for a child process to die.
     * Causes the current thread to wait, if necessary, until the process
     * represented by this Process object has terminated.
     * <BR/>
     * This method returns
     * immediately if the subprocess has already terminated. If the
     * subprocess has not yet terminated, the calling thread will be
     * blocked until the subprocess exits.
     * @return The childs status is in the return code to this process.
     *         It is one of:
     * <PRE>
     *      APR_CHILD_DONE     -- child is no longer running.
     *      APR_CHILD_NOTDONE  -- child is still running.
     * </PRE>
     */
    public int waitFor()
    {
        return wait0(INSTANCE, 0);
    }

    /**
     * Wait for a child process to die
     * Causes the current thread to wait, if necessary, until the process
     * represented by this Process object has terminated or timeout expires.
     * <BR/>
     * This method returns
     * immediately if the subprocess has already terminated. If the
     * subprocess has not yet terminated, the calling thread will be
     * blocked until the subprocess exits.
     * @param timeout How long should we wait. In case the timeout is
     * negative it will block until the child process dies.
     * In case the timeout is zero waitFor will return immediately with
     * the current status.
     * @return The childs status is in the return code to this process.
     *         It is one of:
     * <PRE>
     *      APR_CHILD_DONE     -- child is no longer running.
     *      APR_CHILD_NOTDONE  -- child is still running.
     * </PRE>
     */
    public int waitFor(long timeout)
    {
        return wait1(INSTANCE, 0, timeout);
    }

    /**
     * Wait for a child process to die
     * Causes the current thread to wait, if necessary, until the process
     * represented by this Process object has terminated or timeout expires.
     * <BR/>
     * This method returns
     * immediately if the subprocess has already terminated. If the
     * subprocess has not yet terminated, the calling thread will be
     * blocked until the subprocess exits.
     * @param timeout How long should we wait. In case the timeout is
     * negative it will block until the child process dies.
     * In case the timeout is zero waitFor will return immediately with
     * the current status.
     * @return The childs status is in the return code to this process.
     *         It is one of:
     * <PRE>
     *      APR_CHILD_DONE     -- child is no longer running.
     *      APR_CHILD_NOTDONE  -- child is still running.
     * </PRE>
     */
    public int waitFor(IProgressNotificationCallback progress, long timeout)
    {
        return wait2(INSTANCE, progress, timeout);
    }

    /**
     * Wait for a child process to die
     * Causes the current thread to wait, if necessary, until the process
     * represented by this Process object has terminated or Proggress
     * notification callback returns negative value.
     * <BR/>
     * This method returns
     * immediately if the subprocess has already terminated. If the
     * subprocess has not yet terminated, the calling thread will be
     * blocked until the subprocess exits.
     * @param progress Progress notification callback.
     * @return The childs status is in the return code to this process.
     *         It is one of:
     * <PRE>
     *      APR_CHILD_DONE     -- child is no longer running.
     *      APR_CHILD_NOTDONE  -- child is still running.
     * </PRE>
     */
    public int waitFor(IProgressNotificationCallback progress)
    {
        return wait2(INSTANCE, progress, 0);
    }


    /**
     * Set the child_in and/or parent_in values to existing apr_file_t values.
     * <br />
     * This is NOT a required initializer function. This is
     * useful if you have already opened a pipe (or multiple files)
     * that you wish to use, perhaps persistently across multiple
     * process invocations - such as a log file. You can save some
     * extra function calls by not creating your own pipe since this
     * creates one in the process space for you.
     * @param in apr_file_t value to use as child_in. Must be a valid file.
     * @param parent apr_file_t value to use as parent_in. Must be a valid file.
     */
    public int setChildIn(File in, File parent)
    {
        return ciset0(INSTANCE, in.INSTANCE, parent.INSTANCE);
    }

    /**
     * Set the child_out and parent_out values to existing apr_file_t values.
     * <br />
     * This is NOT a required initializer function. This is
     * useful if you have already opened a pipe (or multiple files)
     * that you wish to use, perhaps persistently across multiple
     * process invocations - such as a log file.
     * @param out apr_file_t value to use as child_out. Must be a valid file.
     * @param parent apr_file_t value to use as parent_out. Must be a valid file.
     */
    public int setChildOut(File out, File parent)
    {
        return coset0(INSTANCE, out.INSTANCE, parent.INSTANCE);
    }

    /**
     * Set the child_err and parent_err values to existing apr_file_t values.
     * <br />
     * This is NOT a required initializer function. This is
     * useful if you have already opened a pipe (or multiple files)
     * that you wish to use, perhaps persistently across multiple
     * process invocations - such as a log file.
     * @param err apr_file_t value to use as child_err. Must be a valid file.
     * @param parent apr_file_t value to use as parent_err. Must be a valid file.
     */
    public int setChildErr(File err, File parent)
    {
        return ceset0(INSTANCE, err.INSTANCE, parent.INSTANCE);
    }

    /**
     * Create a new process and execute a new program within that process.
     * This function returns without waiting for the new process to terminate;
     * use apr_proc_wait for that.
     * @param progname The program to run
     * @param args The arguments to pass to the new program. The first
     *             one should be the program name.
     *             This should be array of strings smaller then
     *             <code>MAX_ENV_SIZE</code>.
     * @param env The new environment table for the new process.
     *            This should be array of strings smaller then
     *            <code>MAX_ENV_SIZE</code>.<BR/>
     *            This argument is ignored for <code>APR_PROGRAM_ENV</code>,
     *            <code>APR_PROGRAM_PATH</code>, and
     *            <code>APR_SHELLCMD_ENV</code> types of commands.
     * @return APR Status code.
     */
    public int exec(String progname, String [] args, String [] env)
        throws NullPointerException, OperatingSystemException
    {
        int rv = exec0(INSTANCE, progname, args, env);
        if (rv == Error.APR_SUCCESS) {
            Id      = pid0(INSTANCE);
            redirIs = new File(POOL, getios0(INSTANCE, 0));
            redirOs = new File(POOL, getios0(INSTANCE, 1));
            redirEs = new File(POOL, getios0(INSTANCE, 2));
        }
        return rv;
    }

    /**
     * Set timeout for process STDOUT stream.
     * The stream must have CHILD_BLOCK IoMode
     * @param timeout Timeout in miliseconds.
     * @return APR Status code.
     */
    public int setStdOutTimeout(long timeout)
    {
        return setiostm0(INSTANCE, timeout, 1);
    }

    /**
     * Set timeout for process STDERR stream.
     * The stream must have CHILD_BLOCK IoMode
     * @param timeout Timeout in miliseconds.
     * @return APR Status code.
     */
    public int setStdErrTimeout(long timeout)
    {
        return setiostm0(INSTANCE, timeout, 2);
    }

    public InputStream getStdOut()
    {
        return redirOs.getInputStream();
    }

    public InputStream getStdErr()
    {
        return redirEs.getInputStream();
    }

    public OutputStream getStdIn()
    {
        return redirIs.getOutputStream();
    }

    /**
     * Register a process to be killed when a pool dies.
     * @param how How to kill the process, one of:
     * <PRE>
     *         KILL_NEVER         -- process is never sent any signals
     *         KILL_ALWAYS        -- process is sent SIGKILL on apr_pool_t cleanup
     *         KILL_AFTER_TIMEOUT -- SIGTERM, wait 3 seconds, SIGKILL
     *         JUST_WAIT          -- wait forever for the process to complete
     *         KILL_ONLY_ONCE     -- send SIGTERM and then wait
     * </PRE>
     */
    public void setOnDestroy(KillConditions how)
    {
        notes0(INSTANCE, how.valueOf());
    }

    /**
     * Get the list of all modules loaded in this process
     * @return Array of Modules.
     */
    public ModuleIterator getModules()
        throws OperatingSystemException
    {
        return new ModuleIterator(Module.getModules(Id));
    }

    /**
     * Get the current process
     * @return Current process.
     */
    public static Process self()
    {
        return new Process(getpid0());
    }


}
