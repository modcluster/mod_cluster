# mod_cluster native modules
mod_cluster consists of four modules that are built separately and comprise different functionality.
All modules depend on 

* Apache HTTP Server devel
* APR devel
* APR-Util devel

Furthermore, mod_proxy_cluster depends on mod_proxy. It means that one needs mod_proxy symbols at linking time and mod_proxy module must be loaded prior to mod_proxy_cluster module at runtime. It happens automatically on Linux and might need an explicit path to mod_proxy.lib on Windows.

# Compilation on Linux
## Dependencies
* cmake 2.8+
* gcc, make (your build tool chain of choice cmake can generate targets for)
* apr-util, apr-util-devel
* apr, apr-devel
* httpd, httpd-devel

## Build
In the mod_cluster/native directory:

    $ mkdir build
    $ cd build/
    $ cmake ../ -G "Unix Makefiles"
    -- Found APR: /usr/lib64/libapr-1.so
    -- Found APRUTIL: /usr/lib64/libaprutil-1.so
    -- Found APACHE: /usr/include/httpd  
    -- Configuring done
    -- Generating done
    -- Build files have been written to: /opt/mod_cluster-cmake/build
    $ make
    $ ls modules/
        mod_advertise.so  mod_cluster_slotmem.so  mod_manager.so  mod_proxy_cluster.so

The "build" directory is arbitrary. One might note that CMake looks up your APR, APR-Util and httpd automatically.
If you wish to enforce a particular httpd or APR, you could provide cmake with additional command line arguments: 

    -DAPR_LIBRARY=... 
    -DAPR_INCLUDE_DIR=... 
    -DAPACHE_INCLUDE_DIR=... 
    -DAPRUTIL_LIBRARY=... 
    -DAPRUTIL_INCLUDE_DIR=... 
    -DAPACHE_LIBRARY=... 

# Compilation on Windows
## Dependencies
* cmake 2.8+
* Microsoft Visual Studio 14.0
 * including Windows SDK (10+ or 8.1+)
* Apache HTTP Server windows distribution, including:
 * libapr-1.lib
 * libaprutil-1.lib
 * libhttpd.lib
 * mod_proxy.lib
 * include files (devel headers)

In this document we concern ourselves with x64 compilation.
One could compile for x86 target by using a 32bit version of Apache HTTP Server distribution and by adjusting following commands on self evident places, e.g. 64/32, X64/X86. 

## Missing a particular .lib file?
Some Apache HTTP Server windows distributions might not contain, for instance, mod_proxy.lib.
One could create such file with the following procedure. Please note we expect your PATH to contain Visual Studio tools, e.g., in the order:

    C:\Program Files (x86)\CMake\bin
    C:\Program Files (x86)\Microsoft Visual Studio 14.0\VC\bin\amd64
    C:\Program Files (x86)\Microsoft Visual Studio 14.0\VC
    C:\Program Files (x86)\Microsoft Visual Studio 14.0\VC\bin

 1. Export symbols from mod_proxy.so file:


```
    λ dumpbin /exports /library C:\Users\karm\WORKSPACE\Apache24\modules\mod_proxy.so> C:\Users\karm\WORKSPACE\Apache24\modules\mod_proxy.def
```

 1. Edit the mod_proxy.def so as it contains word EXPORTS on the first line, followed with symbol names on each new line, e.g.:


```
    EXPORTS
    ap_proxy_acquire_connection
    ap_proxy_backend_broke
    ap_proxy_c2hex
    ap_proxy_canon_netloc
    ...
```

 1. Create the desired .lib file for your target architecture:


```
    λ lib /def:C:\Users\karm\WORKSPACE\Apache24\modules\mod_proxy.def /OUT:C:\Users\karm\WORKSPACE\Apache24\modules\mod_proxy.lib /MACHINE:X64 /NAME:mod_proxy.so
```


## Build
In the mod_cluster/native directory, having the aforementioned PATH set, type:

    λ mkdir build
    λ cd build
    λ vcvars64.bat
    λ cmake ..\ -G "NMake Makefiles" -DAPR_LIBRARY=C:\Users\karm\WORKSPACE\Apache24\lib\libapr-1.lib -DAPR_INCLUDE_DIR=C:\Users\karm\WORKSPACE\Apache24\include\ -DAPACHE_INCLUDE_DIR=C:\Users\karm\WORKSPACE\\Apache24\include\ -DAPRUTIL_LIBRARY=C:\Users\karm\WORKSPACE\Apache24\lib\libaprutil-1.lib -DAPRUTIL_INCLUDE_DIR=C:\Users\karm\WORKSPACE\Apache24\include\ -DAPACHE_LIBRARY=C:\Users\karm\WORKSPACE\Apache24\lib\libhttpd.lib -DPROXY_LIBRARY=C:\Users\karm\WORKSPACE\Apache24\modules\mod_proxy.lib
    -- The C compiler identification is MSVC 19.0.23506.0
    -- The CXX compiler identification is MSVC 19.0.23506.0
    -- Check for working C compiler: C:/Program Files (x86)/Microsoft Visual Studio 14.0/VC/bin/amd64/cl.exe
    -- Check for working C compiler: C:/Program Files (x86)/Microsoft Visual Studio 14.0/VC/bin/amd64/cl.exe -- works
    -- Detecting C compiler ABI info
    -- Detecting C compiler ABI info - done
    -- Check for working CXX compiler: C:/Program Files (x86)/Microsoft Visual Studio 14.0/VC/bin/amd64/cl.exe
    -- Check for working CXX compiler: C:/Program Files (x86)/Microsoft Visual Studio 14.0/VC/bin/amd64/cl.exe -- works
    -- Detecting CXX compiler ABI info
    -- Detecting CXX compiler ABI info - done
    -- Detecting CXX compile features
    -- Detecting CXX compile features - done
    -- Found APR: C:/Users/karm/WORKSPACE/Apache24/lib/libapr-1.lib
    -- Found APRUTIL: C:/Users/karm/WORKSPACE/Apache24/lib/libaprutil-1.lib
    -- Found APACHE: C:/Users/karm/WORKSPACE/Apache24/include
    -- Configuring done
    -- Generating done
    -- Build files have been written to: C:/Users/karm/WORKSPACE/mod_cluster-native/build
    
    λ  nmake
        <SNIP>
    λ dir modules
        mod_advertise.exp
        mod_advertise.ilk
        mod_advertise.lib
        mod_advertise.pdb
        mod_advertise.so
        mod_cluster_slotmem.exp
        mod_cluster_slotmem.ilk
        mod_cluster_slotmem.lib
        mod_cluster_slotmem.pdb
        mod_cluster_slotmem.so
        mod_manager.exp
        mod_manager.ilk
        mod_manager.lib
        mod_manager.pdb
        mod_manager.so
        mod_proxy_cluster.exp
        mod_proxy_cluster.ilk
        mod_proxy_cluster.lib
        mod_proxy_cluster.pdb
        mod_proxy_cluster.so

EOF