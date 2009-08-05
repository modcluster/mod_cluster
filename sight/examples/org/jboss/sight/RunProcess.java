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
import java.util.EnumSet;

/** Library run process example
 *
 * @author Mladen Turk
 */

public class RunProcess {


  private class ProcessWorker extends Thread
    {

        private Process  proc;

        public ProcessWorker(Process proc)
        {
            this.proc = proc;
        }

        public void run() {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                // Skip
            } finally {
                try {
                    System.out.println("Destroying proc");
                    proc.destroy();
                    System.out.println("Destroyed  proc");

                } catch (Exception x) {
                    // Skip
                    x.printStackTrace();
                }
            }

        }


    }


    private class Progress implements IProgressNotificationCallback
    {
        public Progress()
        {

        }

        public int progress(int tick)
        {
            System.out.print(".");
            // tick is number of invocations for this callback
            //
            // Sleep 100 ms and check again
            return 100;
        }
    }

    Process p = null;
    public RunProcess()
    {
        try {
            // Create a Process holder
            p = new Process();
            // Set up pipe modes
            p.setIoMode(PipeIoMode.FULL_BLOCK, PipeIoMode.CHILD_BLOCK, PipeIoMode.CHILD_BLOCK);
            p.setCommandType(CommandType.SHELLCMD);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
    public void execute(String cmd, String [] args)
    {
        try {
            p.exec(cmd, args, null);
            p.setStdOutTimeout(1000);
            p.setStdErrTimeout(1000);
            InputStream is = p.getStdOut();
            int ch;
            // Read from process STDOUT
            try {
                while ((ch = is.read()) > 0) {
                    System.out.write(ch);
                }
            } catch (IOException x) {
                // Timeout thrown after on second
                // because cmd.exe waits for the input
                System.out.println();
                System.out.println(x.toString());
            }
            System.out.println();
            System.out.println("Waiting for process to die ...");
            ProcessWorker pw = new ProcessWorker(p);
            pw.start();
            Progress pc = new Progress();
            int w = p.waitFor(pc, 3000);
            System.out.println();
            if (w == Error.APR_CHILD_NOTDONE) {
                // Refresh the process
                // This is noop for created processes
                p.refresh();                
                p.kill(Signal.SIGKILL);
                w = p.waitFor();
            }
            System.out.println();
            System.out.println("Exited stat: " + w +
                               " why: " + p.getExitReason() +
                               " returned " + p.getExitCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String [] args) {
        try {
            Library.initialize("");

            FileInfo fi = new FileInfo("C:\\WINDOWS",
                                       EnumSet.of(FileInfoFlags.NORM,
                                                  FileInfoFlags.NAME));
            System.out.println("File info " + fi.Type);
            User u1 = new User(fi.UserId);
            Group g1 = new Group(fi.GroupId);
            System.out.println("File info User(" + u1.Name +
                               ") Group (" + g1.Name + ")");

            RunProcess proc = new RunProcess();
            String [] a = new String[3];
            a[0] = "cmd";
            a[1] = "/K";
            a[2] = "dir C:\\";

            proc.execute(a[0], a );
            Library.shutdown();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 }
