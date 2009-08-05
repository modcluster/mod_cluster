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
 * @author Mladen Turk
 *
 */

#ifndef SIGHT_PRIVATE_H
#define SIGHT_PRIVATE_H

#include "apr.h"
#include "apr_general.h"
#include <jni.h>
#include <limits.h>

#if !defined(PRODUCT_UNDEFINED)
/* Vista Platform SDK definitions */

typedef enum
_TCP_TABLE_CLASS {
    TCP_TABLE_BASIC_LISTENER,
    TCP_TABLE_BASIC_CONNECTIONS,
    TCP_TABLE_BASIC_ALL,
    TCP_TABLE_OWNER_PID_LISTENER,
    TCP_TABLE_OWNER_PID_CONNECTIONS,
    TCP_TABLE_OWNER_PID_ALL,
    TCP_TABLE_OWNER_MODULE_LISTENER,
    TCP_TABLE_OWNER_MODULE_CONNECTIONS,
    TCP_TABLE_OWNER_MODULE_ALL
} TCP_TABLE_CLASS, *PTCP_TABLE_CLASS;

typedef enum
_UDP_TABLE_CLASS {
    UDP_TABLE_BASIC,
    UDP_TABLE_OWNER_PID,
    UDP_TABLE_OWNER_MODULE
} UDP_TABLE_CLASS, *PUDP_TABLE_CLASS;

#endif /* PRODUCT_UNDEFINED */

#define SIGHT_PLATFORM_CLASS_PATH   SIGHT_CLASS_PATH "platform/windows/"

#define SIGHT_PLATFORM_DECLARE(RT, CL, FN)  \
    JNIEXPORT RT JNICALL Java_org_jboss_sight_platform_windows_##CL##_##FN

typedef enum {
    SYSDLL_KERNEL32 = 0,    // kernel32 From WinBase.h
    SYSDLL_NTDLL    = 1,    // ntdll    From our real kernel
    SYSDLL_USER32   = 2,    // user32   From WinUser.h
    SYSDLL_IPHLPAPI = 3,    // iphlpapi From Iphlpapi.h
    SYSDLL_JVM      = 4,    // jvm      From our own jvm.dll
    SYSDLL_defined  = 5     // must define as last idx_ + 1
} sight_dlltoken_e;

/* Copied from http://source.winehq.org/source/include/winternl.h */

typedef struct _CURDIR
{
    UNICODE_STRING DosPath;
    PVOID Handle;
} CURDIR, *PCURDIR;

typedef struct RTL_DRIVE_LETTER_CURDIR
{
    USHORT              Flags;
    USHORT              Length;
    ULONG               TimeStamp;
    UNICODE_STRING      DosPath;
} RTL_DRIVE_LETTER_CURDIR, *PRTL_DRIVE_LETTER_CURDIR;

typedef struct tagRTL_BITMAP {
    ULONG  SizeOfBitMap; /* Number of bits in the bitmap */
    PULONG Buffer; /* Bitmap data, assumed sized to a DWORD boundary */
} RTL_BITMAP, *PRTL_BITMAP;

typedef struct _RTL_USER_PROCESS_PARAMETERS
{
    ULONG               AllocationSize;
    ULONG               Size;
    ULONG               Flags;
    ULONG               DebugFlags;
    HANDLE              ConsoleHandle;
    ULONG               ConsoleFlags;
    HANDLE              hStdInput;
    HANDLE              hStdOutput;
    HANDLE              hStdError;
    CURDIR              CurrentDirectory;
    UNICODE_STRING      DllPath;
    UNICODE_STRING      ImagePathName;
    UNICODE_STRING      CommandLine;
    PWSTR               Environment;
    ULONG               dwX;
    ULONG               dwY;
    ULONG               dwXSize;
    ULONG               dwYSize;
    ULONG               dwXCountChars;
    ULONG               dwYCountChars;
    ULONG               dwFillAttribute;
    ULONG               dwFlags;
    ULONG               wShowWindow;
    UNICODE_STRING      WindowTitle;
    UNICODE_STRING      Desktop;
    UNICODE_STRING      ShellInfo;
    UNICODE_STRING      RuntimeInfo;
    RTL_DRIVE_LETTER_CURDIR DLCurrentDirectory[0x20];
} RTL_USER_PROCESS_PARAMETERS, *PRTL_USER_PROCESS_PARAMETERS;

typedef struct _PEB_LDR_DATA
{
    ULONG               Length;
    BOOLEAN             Initialized;
    PVOID               SsHandle;
    LIST_ENTRY          InLoadOrderModuleList;
    LIST_ENTRY          InMemoryOrderModuleList;
    LIST_ENTRY          InInitializationOrderModuleList;
} PEB_LDR_DATA, *PPEB_LDR_DATA;

/***********************************************************************
 * PEB data structure (472 bytes)
 */
typedef struct _SIGHT_PEB
{
    BOOLEAN                      InheritedAddressSpace;             /*  00 */
    BOOLEAN                      ReadImageFileExecOptions;          /*  01 */
    BOOLEAN                      BeingDebugged;                     /*  02 */
    BOOLEAN                      SpareBool;                         /*  03 */
    HANDLE                       Mutant;                            /*  04 */
    HMODULE                      ImageBaseAddress;                  /*  08 */
    PPEB_LDR_DATA                LdrData;                           /*  0c */
    RTL_USER_PROCESS_PARAMETERS *ProcessParameters;                 /*  10 */
    PVOID                        SubSystemData;                     /*  14 */
    HANDLE                       ProcessHeap;                       /*  18 */
    PRTL_CRITICAL_SECTION        FastPebLock;                       /*  1c */
    PVOID /*PPEBLOCKROUTINE*/    FastPebLockRoutine;                /*  20 */
    PVOID /*PPEBLOCKROUTINE*/    FastPebUnlockRoutine;              /*  24 */
    ULONG                        EnvironmentUpdateCount;            /*  28 */
    PVOID                        KernelCallbackTable;               /*  2c */
    PVOID                        EventLogSection;                   /*  30 */
    PVOID                        EventLog;                          /*  34 */
    PVOID /*PPEB_FREE_BLOCK*/    FreeList;                          /*  38 */
    ULONG                        TlsExpansionCounter;               /*  3c */
    PRTL_BITMAP                  TlsBitmap;                         /*  40 */
    ULONG                        TlsBitmapBits[2];                  /*  44 */
    PVOID                        ReadOnlySharedMemoryBase;          /*  4c */
    PVOID                        ReadOnlySharedMemoryHeap;          /*  50 */
    PVOID                       *ReadOnlyStaticServerData;          /*  54 */
    PVOID                        AnsiCodePageData;                  /*  58 */
    PVOID                        OemCodePageData;                   /*  5c */
    PVOID                        UnicodeCaseTableData;              /*  60 */
    ULONG                        NumberOfProcessors;                /*  64 */
    ULONG                        NtGlobalFlag;                      /*  68 */
    BYTE                         Spare2[4];                         /*  6c */
    LARGE_INTEGER                CriticalSectionTimeout;            /*  70 */
    ULONG                        HeapSegmentReserve;                /*  78 */
    ULONG                        HeapSegmentCommit;                 /*  7c */
    ULONG                        HeapDeCommitTotalFreeThreshold;    /*  80 */
    ULONG                        HeapDeCommitFreeBlockThreshold;    /*  84 */
    ULONG                        NumberOfHeaps;                     /*  88 */
    ULONG                        MaximumNumberOfHeaps;              /*  8c */
    PVOID                       *ProcessHeaps;                      /*  90 */
    PVOID                        GdiSharedHandleTable;              /*  94 */
    PVOID                        ProcessStarterHelper;              /*  98 */
    PVOID                        GdiDCAttributeList;                /*  9c */
    PVOID                        LoaderLock;                        /*  a0 */
    ULONG                        OSMajorVersion;                    /*  a4 */
    ULONG                        OSMinorVersion;                    /*  a8 */
    ULONG                        OSBuildNumber;                     /*  ac */
    ULONG                        OSPlatformId;                      /*  b0 */
    ULONG                        ImageSubSystem;                    /*  b4 */
    ULONG                        ImageSubSystemMajorVersion;        /*  b8 */
    ULONG                        ImageSubSystemMinorVersion;        /*  bc */
    ULONG                        ImageProcessAffinityMask;          /*  c0 */
    ULONG                        GdiHandleBuffer[34];               /*  c4 */
    ULONG                        PostProcessInitRoutine;            /* 14c */
    PRTL_BITMAP                  TlsExpansionBitmap;                /* 150 */
    ULONG                        TlsExpansionBitmapBits[32];        /* 154 */
    ULONG                        SessionId;                         /* 1d4 */
} SIGHT_PEB, *PSIGHT_PEB;

#ifdef __cplusplus
extern "C" {
#endif

extern LPSYSTEM_INFO           sight_osinf;
extern LPOSVERSIONINFOEXA      sight_osver;
extern UINT64                  sight_vmem;

FARPROC sight_load_dll_func(sight_dlltoken_e, const char *, int);

/* The sight_load_dll_func call WILL return def if the function cannot be loaded */

#define SIGHT_DECLARE_LATE_DLL_FUNC(lib, rettype, def,                      \
                                    calltype, fn, ord, args, names)         \
    typedef rettype (calltype *sight_late_fpt_##fn) args;                   \
    static sight_late_fpt_##fn sight_late_pfn_##fn = NULL;                  \
    static APR_INLINE rettype sight_late_##fn args                          \
    {   if (!sight_late_pfn_##fn)                                           \
            sight_late_pfn_##fn = (sight_late_fpt_##fn)                     \
                                      sight_load_dll_func(lib, #fn, ord);   \
        if (sight_late_pfn_##fn)                                            \
            return (*(sight_late_pfn_##fn)) names;                          \
        else return def; } //

#define SIGHT_DECLARE_LATE_DLL_CALL(lib, rettype,                           \
                                    calltype, fn, fnx, ord, args, names)    \
    typedef rettype (calltype *sight_late_fpt_##fn) args;                   \
    static sight_late_fpt_##fn sight_late_pfn_##fn = NULL;                  \
    static APR_INLINE rettype sight_late_##fn args                          \
    {   if (!sight_late_pfn_##fn)                                           \
            sight_late_pfn_##fn = (sight_late_fpt_##fn)                     \
                                      sight_load_dll_func(lib, #fn, ord);   \
        if (!sight_late_pfn_##fn)                                           \
            sight_late_pfn_##fn = (sight_late_fpt_##fn)                     \
                                      sight_load_dll_func(lib, #fnx, ord);  \
        if (sight_late_pfn_##fn)                                            \
            (*(sight_late_pfn_##fn)) names;                                 \
    } //


#ifdef SIGHT_WANT_LATE_DLL

SIGHT_DECLARE_LATE_DLL_FUNC(SYSDLL_KERNEL32, BOOL, FALSE,
                            WINAPI, GetSystemTimes, 0, (
    OUT LPFILETIME lpIdleTime,
    OUT LPFILETIME lpKernelTime,
    OUT LPFILETIME lpUserTime),
    (lpIdleTime, lpKernelTime, lpUserTime));
#undef  GetSystemTimes
#define GetSystemTimes sight_late_GetSystemTimes

SIGHT_DECLARE_LATE_DLL_FUNC(SYSDLL_KERNEL32, BOOL, FALSE,
                            WINAPI, IsWow64Process, 0, (
    IN HANDLE hProcess,
    OUT PBOOL Wow64Process),
    (hProcess, Wow64Process));
#undef  IsWow64Process
#define IsWow64Process sight_late_IsWow64Process

SIGHT_DECLARE_LATE_DLL_FUNC(SYSDLL_NTDLL, NTSTATUS, 1,
                            WINAPI, NtQuerySystemInformation, 0, (
    IN SYSTEM_INFORMATION_CLASS SystemInformationClass,
    OUT PVOID SystemInformation,
    IN ULONG SystemInformationLength,
    OUT PULONG ReturnLength),
    (SystemInformationClass, SystemInformation, SystemInformationLength, ReturnLength));
#undef  NtQuerySystemInformation
#define NtQuerySystemInformation sight_late_NtQuerySystemInformation

SIGHT_DECLARE_LATE_DLL_FUNC(SYSDLL_NTDLL, NTSTATUS, 1,
                            WINAPI, NtQueryInformationProcess, 0, (
    IN HANDLE ProcessHandle,
    IN PROCESSINFOCLASS ProcessInformationClass,
    OUT PVOID ProcessInformation,
    IN ULONG ProcessInformationLength,
    OUT PULONG ReturnLength),
    (ProcessHandle, ProcessInformationClass, ProcessInformation, ProcessInformationLength, ReturnLength));
#undef  NtQueryInformationProcess
#define NtQueryInformationProcess sight_late_NtQueryInformationProcess

SIGHT_DECLARE_LATE_DLL_FUNC(SYSDLL_KERNEL32, BOOL, FALSE,
                            WINAPI, GetPerformanceInfo, 0, (
    PPERFORMACE_INFORMATION pPerformanceInformation,
    DWORD cb),
    (pPerformanceInformation, cb));
#undef  GetPerformanceInfo
#define GetPerformanceInfo sight_late_GetPerformanceInfo

SIGHT_DECLARE_LATE_DLL_FUNC(SYSDLL_IPHLPAPI, DWORD, ERROR_INVALID_FUNCTION,
                            WINAPI, GetExtendedTcpTable, 0, (
    PVOID pTcpTable,
    PDWORD pdwSize,
    BOOL bOrder,
    ULONG ulAf,
    TCP_TABLE_CLASS TableClass,
    ULONG Reserved),
    (pTcpTable, pdwSize, bOrder, ulAf, TableClass, Reserved));
#undef  GetExtendedTcpTable
#define GetExtendedTcpTable sight_late_GetExtendedTcpTable

SIGHT_DECLARE_LATE_DLL_FUNC(SYSDLL_IPHLPAPI, DWORD, ERROR_INVALID_FUNCTION,
                            WINAPI, GetExtendedUdpTable, 0, (
    PVOID pUdpTable,
    PDWORD pdwSize,
    BOOL bOrder,
    ULONG ulAf,
    UDP_TABLE_CLASS TableClass,
    ULONG Reserved),
    (pUdpTable, pdwSize, bOrder, ulAf, TableClass, Reserved));
#undef  GetExtendedUdpTable
#define GetExtendedUdpTable sight_late_GetExtendedUdpTable

SIGHT_DECLARE_LATE_DLL_CALL(SYSDLL_JVM, void, JNICALL,
                            JVM_DumpAllStacks,
                            _JVM_DumpAllStacks@8, 0, (
    JNIEnv *lpEnv,
    jclass jClazz),
    (lpEnv, jClazz));
#undef  JVM_DumpAllStacks
#define JVM_DumpAllStacks sight_late_JVM_DumpAllStacks

#undef SIGHT_WANT_LATE_DLL
#endif

#define IS_INVALID_HANDLE(h) (((h) == NULL || (h) == INVALID_HANDLE_VALUE))
#define SIGHT_REGS_CPU  "HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\"
#define SIGHT_REGS_CPU0 SIGHT_REGS_CPU "0\\"

LPWSTR  sight_trimw(LPWSTR);
void    sight_trimwc(LPWSTR, LPCWSTR);
int     sight_iswlast(LPCWSTR, int);

char        *sight_registry_get_lpstr(HKEY, const char *,
                                      char *, apr_size_t);
apr_status_t sight_registry_get_int32(HKEY, const char *, apr_int32_t *);
apr_status_t sight_registry_get_int64(HKEY, const char *, apr_int64_t *);

#if 0
char        *sight_registry_pstrdup(HKEY, const char *, apr_pool_t *);
sight_str_t  sight_registry_pstrdupw(HKEY, LPCWSTR, apr_pool_t *);
char        *sight_registry_strdup(HKEY, const char*);
sight_arr_t  sight_registry_get_arrw(HKEY, LPCWSTR, apr_pool_t *);
#endif

jlong filetime_to_ms(FILETIME *);
jlong winftime_to_ms(FILETIME *);
jlong largeint_to_ms(LARGE_INTEGER *);
jlong litime_to_ms(LARGE_INTEGER *);

apr_status_t sight_uid_get(HANDLE, apr_uid_t *, apr_gid_t *, apr_pool_t *);

/* WMI API */
void        *alloc_variant();
void         free_variant(void *);
void        *wmi_intialize(JNIEnv *, const jchar *, jsize);
void         wmi_terminate(void *);
int          wmi_query(void *, const jchar *, jsize, const jchar *, jsize);
int          wmi_query_next(void *);
int          wmi_query_skip(void *, int);
int          wmi_query_reset(void *);

void        *wmi_query_get(JNIEnv *, void *, const jchar *, jsize);
jlong        wmi_query_getJ(void *, const jchar *, jsize);
jboolean     wmi_query_getZ(void *, const jchar *, jsize);
jdouble      wmi_query_getD(void *, const jchar *, jsize);
jstring      wmi_query_getS(JNIEnv *, void *, const jchar *, jsize);

PSID         sight_get_sid(LPCWSTR, PSID_NAME_USE);
SID_NAME_USE get_sid_name(LPWSTR, size_t, PSID);

#ifdef __cplusplus
}
#endif

#endif /* SIGHT_PRIVATE_H */
