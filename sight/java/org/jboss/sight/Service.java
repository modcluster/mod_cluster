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

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Properties;

/**
 * Service
 *
 * @author Mladen Turk
 *
 */

public final class Service extends NativeObject
{

    private static native int       open0(Object thiz, long instance,
                                          long scm, String name, int access)
                                        throws OutOfMemoryError, OperatingSystemException;
    private static native int       ctrl0(long instance, int cmd);
    private static native int       stats0(Object thiz, long instance);
    private static native int       wait0(Object thiz, long instance, long timeout, int state);
    private static native int       wait1(Object thiz, long instance,
                                          IProgressNotificationCallback progress,
                                          long timeout, int state);

    private static Properties serviceProperties = null;
    private static boolean    servicePropertiesLoaded = false;

    protected static void loadResources()
    {
        if (serviceProperties == null) {
            serviceProperties = new Properties();
            if (OS.IS_LINUX) {
                try {
                    InputStream is = LibraryLoader.class.getResourceAsStream
                        ("/org/jboss/sight/platform/linux/Services.properties");
                    serviceProperties.load(is);
                    is.close();
                }
                catch (Throwable t) {
                    // Nothing
                }
            }
            else if (OS.IS_SOLARIS) {
                try {
                    InputStream is = LibraryLoader.class.getResourceAsStream
                        ("/org/jboss/sight/platform/solaris/Services.properties");
                    serviceProperties.load(is);
                    is.close();
                }
                catch (Throwable t) {
                    // Nothing
                }
            }
        }
    }

    private static void setupKnownService(Service service)
    {
        String pserviceName = serviceProperties.getProperty(service.Name + ".2", service.Name);
        service.DisplayName = serviceProperties.getProperty(pserviceName + ".0", service.DisplayName);
        service.Description = serviceProperties.getProperty(pserviceName + ".1");
    }

    private Service()
    {
        super(0);
    }

    private Service(Pool pool)
    {
        super(pool.POOL);
    }

    private void setState(int state)
    {
        State = ServiceState.valueOf(state);
    }

    protected Service(ServiceControlManager scm, String name)
        throws OutOfMemoryError, OperatingSystemException, IllegalArgumentException
    {
        super(scm.POOL);
        int e = open0(this, INSTANCE, scm.INSTANCE,
                      name, GenericAccessRights.READ.valueOf());
        if (e != Error.APR_SUCCESS) {
            throw new OperatingSystemException(Error.getError(e));
        }
        Name = name;
        if (OS.IS_LINUX || OS.IS_SOLARIS) {
            setupKnownService(this);
        }
    }

    protected Service(ServiceControlManager scm, String name,
                      EnumSet<GenericAccessRights> access)
        throws OutOfMemoryError, OperatingSystemException, IllegalArgumentException
    {
        super(scm.POOL);
        int e = open0(this, INSTANCE, scm.INSTANCE,
                      name, GenericAccessRights.bitmapOf(access));
        if (e != Error.APR_SUCCESS) {
            throw new OperatingSystemException(Error.getError(e));
        }
        Name = name;
        setupKnownService(this);
    }

    /**
     * Refresh the Service.
     */
    public int refresh()
    {
        return stats0(this, INSTANCE);
    }

    protected void onDestroy()
    {
        // Nothing
    }

    /**
     * Current state of the service
     */
    public ServiceState        State;

    /**
     * String that specifies the name of the service.
     * The maximum string length is 256 characters.
     * The service control manager database preserves the case
     * of the characters, but service name comparisons are always
     * case insensitive. Forward-slash (/) and backslash (\) are
     * invalid service name characters.
     */
    public String               Name;

    /**
     * String that contains the fully qualified path to the service
     * binary file. The path can also include arguments for an auto-start
     * service. These arguments are passed to the service entry point
     * (typically the main function).
     */
    public String               BinaryPathName;

    /**
     * Array of names of services or load ordering groups that must start
     * before this service. If the array is null or if it points
     * to an empty string, the service has no dependencies.
     * Dependency on a service means that this service can only run
     * if the service it depends on is running.
     */
    public String[]             Dependencies;

    /**
     * If the service type is SERVICE_WIN32_OWN_PROCESS or
     * SERVICE_WIN32_SHARE_PROCESS, this is the name of the
     * account that the service process will be logged on
     * as when it runs. This name can be of the form
     * "DomainName\Username". If the account belongs to the
     * built-in domain, the name can be of the form ".\Username".
     * The name can also be "LocalSystem" if the process is running
     * under the LocalSystem account.
     * If the service type is SERVICE_KERNEL_DRIVER or
     * SERVICE_FILE_SYSTEM_DRIVER,
     * this name is the driver object name
     * (that is, \FileSystem\Rdr or \Driver\Xns)
     * which the input and output (I/O) system uses to load
     * the device driver. If this member is NULL, the driver
     * is to be run with a default
     * object name created by the I/O system, based on the service name.
     */
    public String               ServiceStartName;

    /**
     * String that specifies the display name to be used by service
     * control programs to identify the service. This string has a
     * maximum length of 256 characters. The name is case-preserved
     * in the service control manager. Display name comparisons are
     * always case-insensitive.
     */
    public String               DisplayName;

    /**
     * String that specifies the description of the service.
     * The service description must not exceed the size of a
     * registry value of type REG_SZ.
     */
    public String               Description;

    /**
     * String that names the load ordering group of which
     * this service is a member. Specify null or an empty
     * string if the service does not belong to a group. <BR/>
     * The startup program uses load ordering groups
     * to load groups of services in a specified order with respect to the other groups
     */
    public String               LoadOrderGroup;

    /**
     * Type of service.
     */
    public ServiceType          ServiceType;

    /**
     * When to start the service.
     */
    public ServiceStartType     ServiceStartType;

    /**
     * Severity of the error, and action taken, if this
     * service fails to start.
     */
    public ServiceErrorControl  ServiceErrorControl;

    /**
     * Error code that the service uses to report an error that
     * occurs when it is starting or stopping. To return an error
     * code specific to the service, the service must set this
     * value to ERROR_SERVICE_SPECIFIC_ERROR to indicate that the
     * ServiceSpecificExitCode member contains the error code.
     * The service should set this value to NO_ERROR when it is
     * running and when it terminates normally.
    */
    public int                  ExitCode;

    /**
     * Service-specific error code that the service returns when
     * an error occurs while the service is starting or stopping.
     * This value is ignored unless the dwWin32ExitCode member is
     * set to ERROR_SERVICE_SPECIFIC_ERROR.
     */
    public int                  ServiceSpecificExitCode;

    /**
     * Process identifier of the service.
     */
    public int                  ProcessId;

    public void control(ServiceControl cmd)
        throws OperatingSystemException
    {
        int rc = ctrl0(INSTANCE, cmd.valueOf());
        if (rc != Error.APR_SUCCESS)
            throw new OperatingSystemException(Error.getError(rc));
    }

    public void waitFor(ServiceState state)
        throws OperatingSystemException
    {
        int rc = wait0(this, INSTANCE, 0, state.valueOf());
        if (rc != Error.APR_SUCCESS) {
            throw new OperatingSystemException(Error.getError(rc));
        }
    }

    public void waitFor(ServiceState state, long timeout)
        throws OperatingSystemException, InterruptedException
    {
        int rc = wait0(this, INSTANCE, timeout, state.valueOf());
        if (rc != Error.APR_SUCCESS) {
            if (Status.APR_STATUS_IS_TIMEUP(rc)) {
                throw new InterruptedException("Timeout ellapsed");
            }
            else {
                throw new OperatingSystemException(Error.getError(rc));
            }
        }
    }

    public void waitFor(ServiceState state, IProgressNotificationCallback callback)
        throws OperatingSystemException
    {
        int rc = wait1(this, INSTANCE, callback, 0, state.valueOf());
        if (rc != Error.APR_SUCCESS) {
            throw new OperatingSystemException(Error.getError(rc));
        }
    }

    public void waitFor(ServiceState state, IProgressNotificationCallback callback,
                        long timeout)
        throws OperatingSystemException, InterruptedException
    {
        int rc = wait1(this, INSTANCE, callback, timeout, state.valueOf());
        if (rc != Error.APR_SUCCESS) {
            if (Status.APR_STATUS_IS_TIMEUP(rc)) {
                throw new InterruptedException("Timeout ellapsed");
            }
            else {
                throw new OperatingSystemException(Error.getError(rc));
            }
        }
    }

}
