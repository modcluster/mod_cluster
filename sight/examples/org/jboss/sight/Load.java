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
import org.jboss.sight.platform.windows.*;

import java.util.Date;
import java.util.Random;
import java.util.EnumSet;


/** Library load example
 *
 * @author Mladen Turk
 */

public class Load {


    public Load()
    {
        try {
            Cpu cpu = new Cpu();
            System.out.println("OS sysname      : " + OS.getSysname());
            System.out.println("OS release      : " + OS.getRelease());
            System.out.println("OS machine      : " + OS.getMachine());
            System.out.println("OS version      : " + OS.getVersion());
            System.out.println("OS nodename     : " + OS.getNodename());
            System.out.println("OS isWOW64      : " + OS.IS_WOW64);

            System.out.println("CPU number      : " + cpu.NumberOfProcessors);
            System.out.println("CPU family      : " + cpu.Family);
            System.out.println("CPU model       : " + cpu.Model);
            System.out.println("CPU stepping    : " + cpu.Stepping);
            System.out.println("CPU name        : " + cpu.Name);
            System.out.println("CPU vendor      : " + cpu.Vendor);
            System.out.println("CPU MHz         : " + cpu.MHz);
            System.out.println("CPU bogomips    : " + cpu.Bogomips);
            System.out.println("CPU idle        : " + cpu.IdleLoad);

            Memory mem = new Memory();

            System.out.println("Total RAM       : " + mem.Physical);
            System.out.println("Avail RAM       : " + mem.AvailPhysical);
            System.out.println("Total Swap      : " + mem.Swap);
            System.out.println("Avail Swap      : " + mem.AvailSwap);
            System.out.println("Load            : " + mem.Load);
            System.out.println("Kernel          : " + mem.Kernel);
            System.out.println("Page faults     : " + mem.PageFaults);

            Cpu stat[] = new Cpu[cpu.NumberOfProcessors + 1];
            for (int i = 0; i < cpu.NumberOfProcessors; i++) {
                stat[i] = new Cpu(i);
                System.out.println("CPU [" + i + "]  user   : " + stat[i].UserTime);
                System.out.println("CPU [" + i + "]  idle   : " + stat[i].IdleLoad);
                System.out.println("CPU [" + i + "]  load   : " + stat[i].UserLoad);
                System.out.println("CPU [" + i + "]  sytem  : " + stat[i].SystemLoad);
            }
            Thread.sleep(500);
            /* Do some CPU tasks */
            int sum = 0;
            for (int i = 0; i < 100000; i++) {
                Random rnd = new Random(System.currentTimeMillis());
                for (int j = 0; j < 1000; j++)
                    sum += rnd.nextInt();
            }
            Thread.sleep(500);
            System.out.println("Second run .... " + sum);
            Date bd = new Date(cpu.BootTime);
            System.out.println("CPU [A]  boot      : " + bd);
            System.out.println("CPU [A]  processes : " + cpu.Processes);
            System.out.println("CPU [A]  running   : " + cpu.RunningProcesses);
            System.out.println("CPU [A]  user      : " + cpu.UserTime);
            System.out.println("CPU [A]  idle      : " + cpu.IdleLoad);
            System.out.println("CPU [A]  load      : " + cpu.UserLoad);
            System.out.println("CPU [A]  system    : " + cpu.SystemLoad);

            for (int i = 0; i < cpu.NumberOfProcessors; i++) {
                stat[i].refresh();
                System.out.println("CPU [" + i + "]  user      : " + stat[i].UserTime);
                System.out.println("CPU [" + i + "]  idle      : " + stat[i].IdleLoad);
                System.out.println("CPU [" + i + "]  load      : " + stat[i].UserLoad);
                System.out.println("CPU [" + i + "]  system    : " + stat[i].SystemLoad);
            }
            Process proc = Process.self();
            System.out.println("Process ID         : " + proc.Id);

            System.out.println("************ PROCESS environment ************");
            String[] env = proc.Environment;
            if (env != null) {
            System.out.println("Process ENV lenght : " + env.length);
            for (int i = 0; i <  env.length; i++) {
                System.out.println("ENV: " + env[i]);

            }
            }
            for (int i = 0; i < cpu.NumberOfProcessors; i++) {
//                stat[i].destroy();
                stat[i] = null;
            }

            System.out.println("GC 1");
            System.gc();
            Thread.sleep(1000);
            System.out.println("GC 2");
            System.gc();
            Thread.sleep(1000);


            System.out.println("************ PROCESS arguments ************");
            String[] cmd = proc.Arguments;
            System.out.println("Process CMD lenght : " + cmd.length);
            for (int i = 0; i <  cmd.length; i++) {
                System.out.println("CMD: " + cmd[i]);

            }

            System.out.println("************ PROCESS loaded modules ************");
            for (Module mod : proc.getModules()) {
                System.out.println("Module: " + mod.Size + "\t\t" + mod.Name);
            }


            mem.refresh();
            System.out.println("************ SYSTEM ************");
            System.out.println("Total RAM       : " + mem.Physical);
            System.out.println("Avail RAM       : " + mem.AvailPhysical);
            System.out.println("Total Swap      : " + mem.Swap);
            System.out.println("Avail Swap      : " + mem.AvailSwap);
            System.out.println("Pagesize        : " + mem.Pagesize);
            System.out.println("Load            : " + mem.Load);
            System.out.println("Kernel          : " + mem.Kernel);
            System.out.println("Page faults     : " + mem.PageFaults);
            System.out.println("Max virtual     : " + mem.MaxVirtual);

            Memory mself = proc.getMemory();

            System.out.println("************ PROCESS ************");
            System.out.println("Total RAM       : " + mself.Physical);
            System.out.println("Avail RAM       : " + mself.AvailPhysical);
            System.out.println("Total Swap      : " + mself.Swap);
            System.out.println("Avail Swap      : " + mself.AvailSwap);
            System.out.println("Pagesize        : " + mself.Pagesize);
            System.out.println("Load            : " + mself.Load);
            System.out.println("Kernel          : " + mself.Kernel);
            System.out.println("Page faults     : " + mself.PageFaults);
            System.out.println("Max virtual     : " + mself.MaxVirtual);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String [] args) {
        try {
            Library.initialize("");
            Load ld = new Load();
            System.out.println("1");
            System.gc();
            Thread.sleep(1000);
            System.out.println("2");
            System.gc();
            Thread.sleep(1000);
            System.out.println("3");
            Library.shutdown();
            Thread.sleep(1000);
            System.out.println("4");
            System.gc();
            Thread.sleep(1000);
            System.out.println("5");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 }
