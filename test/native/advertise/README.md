# Build on Linux

## Prerequisites
 * apr-devel
 * apr
 * cmake
 * make, gcc, the usual toolchain...

## Build
    $ ls
    advertise
    $ mkdir advertise-build
    cd advertise-build/
    $ cmake ../advertise -G "Unix Makefiles"
    $ make

## Run
    $ ./advertise -h
    UDP Multicast Advertize Usage and Defaults:
        --udpaddress    -a      UDP Multicast address to send datagrams to. Value: 224.0.1.105
        --udpport       -p      UDP Multicast port. Value: 23364
        --nicaddress    -n      IP address of the NIC to bound to. Value: NULL
        --help  -h      show help

# Build on Windows

## Prerequisites
 * MS VisualStudio 2014 Community/Express
 * cmake
 * apr-devel (.lib)
 * apr (.dll)
 * aforementioned apr dependencies come with any httpd-devel build; one could create .lib from .dll [with this MS CV tool](https://adrianhenke.wordpress.com/2008/12/05/create-lib-file-from-dll/)

## Build (full output)
    C:\Users\karm\advertise-build
    λ cmake ..\advertise -G "NMake Makefiles" -DAPR_LIBRARY=C:\Users\karm\WORKSPACE\httpd-devel\lib\libapr-1.lib -DAPR_INCLUDE_DIR=C:\Users\karm\WORKSPACE\httpd-devel\include\httpd\
    -- The C compiler identification is MSVC 19.0.23506.0
    -- The CXX compiler identification is MSVC 19.0.23506.0
    -- Check for working C compiler: C:/Program Files (x86)/Microsoft Visual Studio 14.0/VC/bin/cl.exe
    -- Check for working C compiler: C:/Program Files (x86)/Microsoft Visual Studio 14.0/VC/bin/cl.exe -- works
    -- Detecting C compiler ABI info
    -- Detecting C compiler ABI info - done
    -- Check for working CXX compiler: C:/Program Files (x86)/Microsoft Visual Studio 14.0/VC/bin/cl.exe
    -- Check for working CXX compiler: C:/Program Files (x86)/Microsoft Visual Studio 14.0/VC/bin/cl.exe -- works
    -- Detecting CXX compiler ABI info
    -- Detecting CXX compiler ABI info - done
    -- Detecting CXX compile features
    -- Detecting CXX compile features - done
    -- Found APR: C:/Users/karm/WORKSPACE/httpd-devel/lib/libapr-1.lib
    -- Configuring done
    -- Generating done
    -- Build files have been written to: C:/Users/karm/advertise-build
    λ nmake
    <SNIP>

The advertise.exe expects to have the libapr-1.dll close by:

    λ cp C:\Users\karm\WORKSPACE\httpd-devel\lib\libapr-1.dll .

## Run
    C:\Users\karm\advertise-build
    λ advertise.exe -h
    UDP Multicast Advertize Usage and Defaults:
            --udpaddress    -a      UDP Multicast address to send datagrams to. Value: 224.0.1.105
            --udpport       -p      UDP Multicast port. Value: 23364
            --nicaddress    -n      IP address of the NIC to bound to. Value: NULL
            --help  -h      show help

