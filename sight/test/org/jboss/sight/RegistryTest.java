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
import java.util.EnumSet;

import junit.framework.Assert;
import junit.framework.TestCase;


/**
 * Registry Test.
 *
 */
public class RegistryTest extends TestCase
{

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(RegistryTest.class);
    }

    protected void setUp()
        throws Exception
    {
        Library.initialize(null);
    }

    protected void tearDown()
        throws Exception
    {
        Library.shutdown();
    }

    public void testRegistryValueType()
        throws Exception
    {
        // Test some common registry keys
        for (int i = 0; i < 100; i++) {
            Registry registry = new Registry(HKEY.LOCAL_MACHINE,
                                             "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion",
                                             EnumSet.of(KeyAccessRights.READ));
            RegistryValueType valueType = registry.getValueType( "ProductId");
            assertEquals(RegistryValueType.SZ, valueType);
        }
        for (int i = 0; i < 100; i++) {
            Registry registry = new Registry(HKEY.LOCAL_MACHINE,
                                             "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion",
                                             EnumSet.of(KeyAccessRights.READ));
            RegistryValueType valueType = registry.getValueType( "InstallDate");
            assertEquals(RegistryValueType.DWORD, valueType);
        }
        for (int i = 0; i < 100; i++) {
            Registry registry = new Registry(HKEY.LOCAL_MACHINE,
                                             "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion",
                                             EnumSet.of(KeyAccessRights.READ));
            RegistryValueType valueType = registry.getValueType( "DigitalProductId");
            assertEquals(RegistryValueType.BINARY, valueType);
        }
    }

    public void testRegistryCreate()
        throws Exception
    {
        // Our test key
        String base = "SOFTWARE\\Red Hat\\Sight\\RegistryTest";
        for (int i = 0; i < 100; i++) {
            Registry registry = new Registry(HKEY.LOCAL_MACHINE,
                                             base,
                                             EnumSet.of(KeyAccessRights.READ, KeyAccessRights.WRITE));
            registry.setValue("Integer", i);
            Registry subkey = new Registry(registry, "Subkey" + i);
            subkey.setValue("String", "Some value " + i);
        }
        // Now test values
        for (int i = 0; i < 100; i++) {
            Registry registry = new Registry(HKEY.LOCAL_MACHINE,
                                             base,
                                             EnumSet.of(KeyAccessRights.READ));
            RegistryValueType valueType = registry.getValueType("Integer");
            assertEquals(valueType, RegistryValueType.DWORD);
            int value = registry.getIntegerValue("Integer");
            assertEquals(99, value);
        }
    }

    public void testRegistryEnum()
        throws Exception
    {
        // Our test key
        String base = "SOFTWARE\\Red Hat\\Sight\\RegistryTest";
        Registry registry = new Registry(HKEY.LOCAL_MACHINE,
                                         base,
                                         EnumSet.of(KeyAccessRights.READ));
        // Enumerate all keys
        String keys[] = registry.enumKeys();
        assertEquals(100, keys.length);
        for (int i = 0; i < 100; i++) {
            Registry subkey = new Registry(HKEY.LOCAL_MACHINE,
                                           base + "\\" + keys[i],
                                           EnumSet.of(KeyAccessRights.READ));
            RegistryValueType valueType = subkey.getValueType("String");
            assertEquals(valueType, RegistryValueType.SZ);
            String value =  subkey.getStringValue("String");
        }

    }

    public void testRegistryDelete()
        throws Exception
    {
        // Our test key
        String base = "SOFTWARE\\Red Hat\\Sight\\RegistryTest";
        int rc;
        rc = Registry.deleteKey(HKEY.LOCAL_MACHINE, base, true);
        // This should fail because key is not empty
        assertEquals(720145, rc);
        rc = Registry.deleteKey(HKEY.LOCAL_MACHINE, base, false);
        assertEquals(Error.APR_SUCCESS, rc);
        // Cleanup Red Hat if not empty
        Registry.deleteKey(HKEY.LOCAL_MACHINE, "SOFTWARE\\Red Hat\\Sight", true);
        // Cleanup Red Hat but only if not empty
        Registry.deleteKey(HKEY.LOCAL_MACHINE, "SOFTWARE\\Red Hat", true);
    }

}
