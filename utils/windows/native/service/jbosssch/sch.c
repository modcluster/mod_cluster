/*
 *  JBOSSSCH - JBoss Service helper
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
 */

/*
 * Ignore most warnings (back down to /W3) for poorly constructed headers
 */
#if defined(_MSC_VER) && _MSC_VER >= 1200
#pragma warning(push, 3)
#endif

/* disable or reduce the frequency of...
 *   C4057: indirection to slightly different base types
 *   C4075: slight indirection changes (unsigned short* vs short[])
 *   C4100: unreferenced formal parameter
 *   C4127: conditional expression is constant
 *   C4163: '_rotl64' : not available as an intrinsic function
 *   C4201: nonstandard extension nameless struct/unions
 *   C4244: int to char/short - precision loss
 *   C4514: unreferenced inline function removed
 */
#pragma warning(disable: 4100 4127 4163 4201 4514; once: 4057 4075 4244)

/* Ignore Microsoft's interpretation of secure development
 * and the POSIX string handling API
 */
#if defined(_MSC_VER) && _MSC_VER >= 1400
#ifndef _CRT_SECURE_NO_DEPRECATE
#define _CRT_SECURE_NO_DEPRECATE
#endif
#pragma warning(disable: 4996)
#endif

#ifndef _WINDOWS_
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#ifndef _WIN32_WINNT
/* Restrict the server to a subset of Windows 2000 header files by default
 */
#define _WIN32_WINNT 0x0500
#endif
#endif

#include <windows.h>
#include <strsafe.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>
#include <process.h>

#include <jni.h>

#pragma warning(disable: 4996)

#define JBSCH_CLASS_PATH     "org/jboss/windows/SignalHelper"
#define JBSCH_LOG_DOMAIN     "JBossServiceHelper"
#define JBSCH_LOG_KEY        "SYSTEM\\CurrentControlSet\\Services\\EventLog\\Application"
#define JBSCH_ENV            "JSERVICE_NAME"
#define UNREFERENCED(P)      (P) = (P)
#define UNREFERENCED_STDARGS e = e; o = o
#define IS_INVALID_HANDLE(h) (((h) == NULL || (h) == INVALID_HANDLE_VALUE))

typedef void (JNICALL *PFN_JVM_DUMPALLSTACKS) (JNIEnv *e, jclass c);

#ifdef WIN32
#define LLT(X) (X)
#else
#define LLT(X) ((long)(X))
#endif
#define P2J(P)          ((jlong)LLT(P))
#define J2P(P, T)       ((T)LLT((jlong)P))
#define J2S(V)          c##V

/* On stack buffer size */
#define JBSCH_BUFFER_SZ   8192
#define JBSCH_SIGNAL_SZ   200
#define JBSCH_STDARGS     JNIEnv *e, jobject o

#define JBSCH_ALLOC_CSTRING(V)     \
    const char *c##V = V ? (const char *)((*e)->GetStringUTFChars(e, V, 0)) : NULL

#define JBSCH_FREE_CSTRING(V)      \
    if (c##V) (*e)->ReleaseStringUTFChars(e, V, c##V)

#define JBSCH_IMPLEMENT_CALL(RT, FN)    \
    JNIEXPORT RT JNICALL Java_org_jboss_windows_SignalHelper_##FN

#define JBSCH_IMPLEMENT_METHOD(RT, FN)  \
    static RT method_##FN

#define JBSCH_GETNET_METHOD(FN)  method_##FN

#define JBSCH_THROW_EXCEPTION(M)         \
            ThrowException(e, "java/lang/Exception", (M))

#define JBSCH_TO_JSTRING(V)   (*e)->NewStringUTF((e), (V))
#define JBSCH_BEGIN_MACRO     if (1) {
#define JBSCH_END_MACRO       } else (void)(0)

#define JBSCH_LOAD_CLASS(E, C, N, R)                    \
    JBSCH_BEGIN_MACRO                                   \
        jclass _##C = (*(E))->FindClass((E), N);        \
        if (_##C == NULL) {                             \
            (*(E))->ExceptionClear((E));                \
            return R;                                   \
        }                                               \
        C = (*(E))->NewGlobalRef((E), _##C);            \
        (*(E))->DeleteLocalRef((E), _##C);              \
    JBSCH_END_MACRO

#define JBSCH_UNLOAD_CLASS(E, C)                        \
        (*(E))->DeleteGlobalRef((E), (C))

#define JBSCH_IS_NULL(E, O)                             \
        ((*(E))->IsSameObject((E), (O), NULL) == JNI_TRUE)

#define JBSCH_GET_METHOD(E, C, M, N, S, R)              \
    JBSCH_BEGIN_MACRO                                   \
        M = (*(E))->GetStaticMethodID((E), C, N, S);    \
        if (M == NULL) {                                \
            return R;                                   \
        }                                               \
    JBSCH_END_MACRO

#define JBSCH_DECLARE(type)         __declspec(dllexport) type __stdcall
#define JBSCH_DECLARE_NONSTD(type)  __declspec(dllexport) type
#define JBSCH_DECLARE_DATA          __declspec(dllexport)

#define LOG_MSG_EMERG                    0xC0000001L
#define LOG_MSG_ERROR                    0xC0000002L
#define LOG_MSG_NOTICE                   0x80000003L
#define LOG_MSG_WARN                     0x80000004L
#define LOG_MSG_INFO                     0x40000005L
#define LOG_MSG_DEBUG                    0x00000006L

#define JBSCH_LOG_EMERG  1
#define JBSCH_LOG_ERROR  2
#define JBSCH_LOG_NOTICE 3
#define JBSCH_LOG_WARN   4
#define JBSCH_LOG_INFO   5
#define JBSCH_LOG_DEBUG  6

static CRITICAL_SECTION dll_critical_section;   /* dll's critical section */
static HINSTANCE        dll_instance = NULL;
static char             dll_file_name[MAX_PATH+1];
static JavaVM           *jbsch_global_vm = NULL;
static char             jbsch_service_name[JBSCH_SIGNAL_SZ + 1];
static BOOL             log_initialized = FALSE;
static BOOL             service_mode = FALSE;

static HANDLE           jbsch_monitor_thread = NULL;
static DWORD            jbsch_monitor_threadid;

#define JBSCH_NUM_EVENTS 10

static HANDLE           jbsch_ctrl_events[JBSCH_NUM_EVENTS];
static HMODULE          jvm_library = NULL;

#define JVM_DUMPALLSTACKSA      "_JVM_DumpAllStacks@8"
#define JVM_DUMPALLSTACKSC      "JVM_DumpAllStacks"
static  PFN_JVM_DUMPALLSTACKS   pfn_JVM_DumpAllStacks = NULL;

static jclass    jWrapper_class;
static jmethodID jWrapper_onSignal;

BOOL APIENTRY DllMain(HINSTANCE hInstance,
                      DWORD  dwReasonForCall,
                      LPVOID lpReserved)
{
    int i;
    switch (dwReasonForCall) {
        /**
         *  The DLL is loading due to process
         *  initialization or a call to LoadLibrary.
         */
        case DLL_PROCESS_ATTACH:
            InitializeCriticalSection(&dll_critical_section);
            dll_instance = hInstance;
            GetModuleFileNameA(hInstance, dll_file_name, MAX_PATH);
            for (i = 0; i < JBSCH_NUM_EVENTS; i++) {
                jbsch_ctrl_events[i] = NULL;
            }
            jvm_library = GetModuleHandleA("jvm.dll");
            if (!IS_INVALID_HANDLE(jvm_library)) {
                pfn_JVM_DumpAllStacks = (PFN_JVM_DUMPALLSTACKS)
                        GetProcAddress(jvm_library, JVM_DUMPALLSTACKSA);
                if (pfn_JVM_DumpAllStacks == NULL) {
                    pfn_JVM_DumpAllStacks = (PFN_JVM_DUMPALLSTACKS)
                        GetProcAddress(jvm_library, JVM_DUMPALLSTACKSC);
                }
            }
        break;
        /**
         *  The attached process creates a new thread.
         */
        case DLL_THREAD_ATTACH:
        break;

        /**
         *  The thread of the attached process terminates.
         */
        case DLL_THREAD_DETACH:
        break;

        /**
         *  DLL unload due to process termination
         *  or FreeLibrary.
         */
        case DLL_PROCESS_DETACH:
            /* Make sure the library is always terminated */
            if (!IS_INVALID_HANDLE(jbsch_ctrl_events[0])) {
                SetEvent(jbsch_ctrl_events[0]);
                if (!IS_INVALID_HANDLE(jbsch_monitor_thread)) {
                    WaitForSingleObject(jbsch_monitor_thread, INFINITE);
                    CloseHandle(jbsch_monitor_thread);
                    jbsch_monitor_thread = NULL;
                }
            }
            for (i = 0; i < JBSCH_NUM_EVENTS; i++) {
                if (!IS_INVALID_HANDLE(jbsch_ctrl_events[i])) {
                    CloseHandle(jbsch_ctrl_events[i]);
                    jbsch_ctrl_events[i] = NULL;
                }
            }
            if (!IS_INVALID_HANDLE(jbsch_monitor_thread)) {
                CloseHandle(jbsch_monitor_thread);
                jbsch_monitor_thread = NULL;
            }
            DeleteCriticalSection(&dll_critical_section);
        break;

        default:
        break;
    }

    return TRUE;
    UNREFERENCED_PARAMETER(lpReserved);
}

/* To share the semaphores with other processes, we need a NULL ACL
 * Code from MS KB Q106387
 */
static PSECURITY_ATTRIBUTES GetNullACL()
{
    PSECURITY_DESCRIPTOR pSD;
    PSECURITY_ATTRIBUTES sa;

    sa  = (PSECURITY_ATTRIBUTES)LocalAlloc(LPTR, sizeof(SECURITY_ATTRIBUTES));
    if (sa == NULL)
        return NULL;
    sa->nLength = sizeof(SECURITY_ATTRIBUTES);

    pSD = (PSECURITY_DESCRIPTOR)LocalAlloc(LPTR, SECURITY_DESCRIPTOR_MIN_LENGTH);
    if (pSD == NULL) {
        LocalFree(sa);
        return NULL;
    }
    sa->lpSecurityDescriptor = pSD;

    SetLastError(0);
    if (!InitializeSecurityDescriptor(pSD, SECURITY_DESCRIPTOR_REVISION)
        || GetLastError()) {
        LocalFree(pSD);
        LocalFree(sa);
        return NULL;
    }
    if (!SetSecurityDescriptorDacl(pSD, TRUE, (PACL) NULL, FALSE)
        || GetLastError()) {
        LocalFree(pSD );
        LocalFree(sa);
        return NULL;
    }

    sa->bInheritHandle = FALSE;
    return sa;
}


static void CleanNullACL(PSECURITY_ATTRIBUTES sa)
{
    if (sa) {
        LocalFree(sa->lpSecurityDescriptor);
        LocalFree(sa);
    }
}

static BOOL InitSyslogSource()
{
    HKEY  key;
    if (RegCreateKeyA(HKEY_LOCAL_MACHINE, JBSCH_LOG_KEY "\\" JBSCH_LOG_DOMAIN,
                      &key) == ERROR_SUCCESS) {
        DWORD typesSupported = EVENTLOG_ERROR_TYPE | EVENTLOG_WARNING_TYPE |
                               EVENTLOG_INFORMATION_TYPE;
        RegSetValueExA(key, "EventMessageFile", 0, REG_SZ,
                       (LPBYTE)&dll_file_name[0], lstrlenA(dll_file_name) + 1);
        RegSetValueExA(key, "TypesSupported", 0, REG_DWORD,
                       (LPBYTE)&typesSupported, sizeof(DWORD));
        RegCloseKey(key);
        log_initialized = TRUE;
        return TRUE;
    }
    return FALSE;
}

static void SyslogMessage(int level, const char *msg, const char *desc)
{
    DWORD id = LOG_MSG_DEBUG;
    WORD  il = EVENTLOG_SUCCESS;
    WORD  ne = desc ? 2 : 1;
    HANDLE  source;
    const char *messages[2];

    if (!log_initialized) {
        EnterCriticalSection(&dll_critical_section);
        if (!log_initialized) {
            if (!InitSyslogSource()) {
                LeaveCriticalSection(&dll_critical_section);
                return;
            }
        }
        LeaveCriticalSection(&dll_critical_section);
    }

    switch (level) {
        case JBSCH_LOG_EMERG:
            id = LOG_MSG_EMERG;
            il = EVENTLOG_ERROR_TYPE;
        break;
        case JBSCH_LOG_ERROR:
            id = LOG_MSG_ERROR;
            il = EVENTLOG_ERROR_TYPE;
        break;
        case JBSCH_LOG_NOTICE:
            id = LOG_MSG_NOTICE;
            il = EVENTLOG_WARNING_TYPE;
        break;
        case JBSCH_LOG_WARN:
            id = LOG_MSG_WARN;
            il = EVENTLOG_WARNING_TYPE;
        break;
        case JBSCH_LOG_INFO:
            id = LOG_MSG_INFO;
            il = EVENTLOG_INFORMATION_TYPE;
        break;
    }

    messages[0] = msg;
    messages[1] = desc;
    source = RegisterEventSourceA(NULL, JBSCH_LOG_DOMAIN);
    if (source != NULL) {
        ReportEventA(source, il,
                     0,
                     id,
                     NULL,
                     ne,
                     0,
                     messages, NULL);
        DeregisterEventSource(source);
    }
}

static void SyslogError(int level, DWORD err, const char *desc)
{
    void *buf;

    if (!FormatMessageA(FORMAT_MESSAGE_ALLOCATE_BUFFER |
                        FORMAT_MESSAGE_FROM_SYSTEM |
                        FORMAT_MESSAGE_IGNORE_INSERTS,
                        NULL,
                        err,
                        MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
                        (LPTSTR)&buf,
                        0,
                        NULL)) {
        char sb[JBSCH_SIGNAL_SZ + 1];
        StringCbPrintf(sb, JBSCH_SIGNAL_SZ,
                       "Unknown error: 0x%08X\n", err);
        SyslogMessage(level, sb, desc);
    }
    else {
        SyslogMessage(level, (const char *)buf, desc);
        LocalFree(buf);
    }
}

/*
 * Convenience function to help throw a class
 */
static void ThrowException(JNIEnv *env, const char *cls, const char *msg)
{
    jclass javaClass;

    javaClass = (*env)->FindClass(env, cls);
    if (javaClass == NULL) {
        char buf[JBSCH_BUFFER_SZ];
        StringCbPrintfA(buf, JBSCH_BUFFER_SZ,
                        "Cannot find %s class.", cls);
        SyslogMessage(JBSCH_LOG_ERROR, buf, msg);
        return;
    }
    (*env)->ThrowNew(env, javaClass, msg);
    (*env)->DeleteLocalRef(env, javaClass);
}

static volatile int is_initialized = 0;

/*
 * Called by the JVM when JBOSSSCH is loaded
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
    JNIEnv *env;
    UNREFERENCED(reserved);
    jbsch_global_vm = vm;
    is_initialized  = 0;

    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_4)) {
        return JNI_ERR;
    }
    JBSCH_LOAD_CLASS(env, jWrapper_class, JBSCH_CLASS_PATH, JNI_ERR);

    JBSCH_GET_METHOD(env, jWrapper_class, jWrapper_onSignal,
                     "onSignal", "(I)Z", JNI_ERR);
    return JNI_VERSION_1_4;
}

/*
 * Called by the JVM before the JBOSSSCH is unloaded
 */
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved)
{
    JNIEnv *env;
    UNREFERENCED(reserved);

    if (is_initialized) {
        /* TODO: See if we need to support the native
         * library load/unload.
         */
        is_initialized  = 0;

    }
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_2)) {
        return;
    }
    JBSCH_UNLOAD_CLASS(env, jWrapper_class);

}

static void DumpAllStacks(JNIEnv *env)
{
    if (pfn_JVM_DumpAllStacks) {
        (*pfn_JVM_DumpAllStacks)(env, NULL);
    }
}

static void CallOnSignal(int signal)
{
    JNIEnv *env = NULL;
    int is_attached = 0;

    if ((*jbsch_global_vm)->GetEnv(jbsch_global_vm, (void **)&env,
                                   JNI_VERSION_1_4) == JNI_EDETACHED) {
        (*jbsch_global_vm)->AttachCurrentThread(jbsch_global_vm,
                                                (void**)&env, NULL);
        is_attached = 1;
    }
    if (env) {
        jboolean s = (*env)->CallStaticBooleanMethod(env,
                                                     jWrapper_class,
                                                     jWrapper_onSignal,
                                                     signal);
        if (s && signal == 3) {
            DumpAllStacks(env);
        }
        if (is_attached) {
            (*jbsch_global_vm)->DetachCurrentThread(jbsch_global_vm);
        }
    }
}

static DWORD WINAPI SignalThreadProc(LPVOID lpParam)
{
    int i;
    char buf[MAX_PATH+1];

    for (;;) {
        DWORD e = WaitForMultipleObjects(JBSCH_NUM_EVENTS,
                                         jbsch_ctrl_events,
                                         FALSE,
                                         INFINITE);
        if (e == WAIT_TIMEOUT) {
            /* Should never happen */
            SyslogError(JBSCH_LOG_WARN, GetLastError(),
                        "Event timeout signaled");
        }
        else if (e >= WAIT_ABANDONED_0) {
            e = e - WAIT_ABANDONED_0;
            StringCbPrintfA(buf, MAX_PATH,
                            "Event %d abandoned.", e);
        }
        else {
            e = e - WAIT_OBJECT_0;
            /* JBSCH_TERMINATE_EVENT
             * Stop monitoring events
             */
            if (e == 0) {
                SyslogMessage(JBSCH_LOG_INFO,
                              "Terminate event signaled", NULL);
                break;
            }
            if (e < JBSCH_NUM_EVENTS) {
                CallOnSignal(e);
            }
            else {
                SyslogError(JBSCH_LOG_WARN, GetLastError(),
                "Unknown event signaled");
            }
        }
    }
    for (i = 0; i < JBSCH_NUM_EVENTS; i++) {
        if (!IS_INVALID_HANDLE(jbsch_ctrl_events[i])) {
            CloseHandle(jbsch_ctrl_events[i]);
            jbsch_ctrl_events[i] = NULL;
        }
    }
    ExitThread(0);
    return 0;
}

/* Console control handler
 *
 */
BOOL WINAPI console_handler(DWORD dwCtrlType)
{
    switch (dwCtrlType) {
        case CTRL_C_EVENT:
#ifdef _DEBUG
            SyslogMessage(JBSCH_LOG_DEBUG,
                          "Console CTRL+C event signaled",
                          NULL);
#endif
            CallOnSignal(1);
            return FALSE;
        case CTRL_CLOSE_EVENT:
#ifdef _DEBUG
            SyslogMessage(JBSCH_LOG_DEBUG,
                          "Console CLOSE event signaled",
                          NULL);
#endif
            CallOnSignal(2);
            return TRUE;
        case CTRL_BREAK_EVENT:
#ifdef _DEBUG
            SyslogMessage(JBSCH_LOG_DEBUG,
                          "Console CTRL+BREAK event signaled",
                          NULL);
#endif
            CallOnSignal(3);
            return TRUE;
        case CTRL_SHUTDOWN_EVENT:
#ifdef _DEBUG
            SyslogMessage(JBSCH_LOG_DEBUG,
                          "Console SHUTDOWN event signaled",
                          NULL);
#endif
            CallOnSignal(4);
            return FALSE;
        case CTRL_LOGOFF_EVENT:
#ifdef _DEBUG
            SyslogMessage(JBSCH_LOG_DEBUG,
                          "Console LOGOFF event signaled",
                          NULL);
#endif
            CallOnSignal(5);
            return TRUE;
        break;

   }
   return FALSE;
}

static void rtrim(char *str)
{
    int i = lstrlenA(str) - 1;
    while ((str[i] == ' ') && (i >= 0))
        str[i--] = '\0';
}

JBSCH_IMPLEMENT_CALL(jboolean, init)(JBSCH_STDARGS, jstring name)
{
    int i;
    char c;
    PSECURITY_ATTRIBUTES sa = NULL;
    JBSCH_ALLOC_CSTRING(name);
    jboolean rv = JNI_FALSE;

    if (is_initialized++) {
        /* Already initialized */
        SyslogMessage(JBSCH_LOG_WARN, "Library already initialized", NULL);
        JBSCH_FREE_CSTRING(name);
        return JNI_TRUE;
    }
    if (J2S(name)) {
        StringCbCopyA(jbsch_service_name, JBSCH_SIGNAL_SZ, J2S(name));
    }
    else {
        DWORD sz = GetEnvironmentVariableA(JBSCH_ENV,
                                           jbsch_service_name,
                                           JBSCH_SIGNAL_SZ);
        if (sz > JBSCH_SIGNAL_SZ) {
            JBSCH_THROW_EXCEPTION(JBSCH_ENV
                                  " Environment variable too large.");
            goto cleanup;
        }
        else if (sz == 0) {
#ifdef _DEBUG
            SyslogMessage(JBSCH_LOG_DEBUG,
                          "Using PID instead " JBSCH_ENV
                          " environment variable",
                          NULL);
#endif
            StringCbPrintfA(jbsch_service_name,
                            JBSCH_SIGNAL_SZ,
                            "%d", _getpid());
        }
    }
    rtrim(jbsch_service_name);
    /* Check if we are in the service mode */
    if (GetEnvironmentVariableA("JSERVICE_PPID", &c, 0) > 0) {
        service_mode = TRUE;
    }
    sa = GetNullACL();
    jbsch_ctrl_events[0] = CreateEvent(sa, FALSE, FALSE, NULL);
    if (IS_INVALID_HANDLE(jbsch_ctrl_events[0])) {
        SyslogError(JBSCH_LOG_ERROR, GetLastError(), "Terminate Event");
        JBSCH_THROW_EXCEPTION("Error creating TERMINATE event object");
        goto cleanup;
    }

    for (i = 1; i < JBSCH_NUM_EVENTS; i++) {
        char sb[JBSCH_SIGNAL_SZ+1];
        StringCbPrintfA(sb, JBSCH_SIGNAL_SZ, "Global\\PSIGNUM_%s_%d",
                        jbsch_service_name, i);
        jbsch_ctrl_events[i] = CreateEventA(sa, FALSE, FALSE, sb);
        if (IS_INVALID_HANDLE(jbsch_ctrl_events[i])) {
            SyslogError(JBSCH_LOG_ERROR, GetLastError(), sb);
            JBSCH_THROW_EXCEPTION("Error creating event object");
            goto cleanup;
        }
    }

    jbsch_monitor_thread = CreateThread(NULL, 0, SignalThreadProc,
                                        NULL, 0, &jbsch_monitor_threadid);
    if (IS_INVALID_HANDLE(jbsch_monitor_thread)) {
        SyslogError(JBSCH_LOG_ERROR, GetLastError(), "Event Thread");
        JBSCH_THROW_EXCEPTION("Error creating event thread");
        goto cleanup;
    }
    if (service_mode) {
        /* Allocate console so that events gets processed */
        AllocConsole();
        /* Set console handler to capture CTRL events */
        SetConsoleCtrlHandler((PHANDLER_ROUTINE)console_handler, TRUE);
    }
    rv = JNI_TRUE;

cleanup:
    if (rv == JNI_FALSE) {
        for (i = 0; i < JBSCH_NUM_EVENTS; i++) {
            if (!IS_INVALID_HANDLE(jbsch_ctrl_events[i])) {
                CloseHandle(jbsch_ctrl_events[i]);
                jbsch_ctrl_events[i] = NULL;
            }
        }
    }
    CleanNullACL(sa);
    JBSCH_FREE_CSTRING(name);
    return rv;
}

JBSCH_IMPLEMENT_CALL(void, term)(JBSCH_STDARGS)
{
    int i;

    is_initialized--;
    if (is_initialized) {
        return;
    }
    if (service_mode) {
        FreeConsole();
    }

    if (IS_INVALID_HANDLE(jbsch_ctrl_events[0])) {
        JBSCH_THROW_EXCEPTION("Invalid TERMINATE event object");
        goto cleanup;
    }

    SetEvent(jbsch_ctrl_events[0]);
    if (!IS_INVALID_HANDLE(jbsch_monitor_thread)) {
        WaitForSingleObject(jbsch_monitor_thread, INFINITE);
    }

cleanup:
    for (i = 0; i < JBSCH_NUM_EVENTS; i++) {
        if (!IS_INVALID_HANDLE(jbsch_ctrl_events[i])) {
            CloseHandle(jbsch_ctrl_events[i]);
            jbsch_ctrl_events[i] = NULL;
        }
    }
    if (!IS_INVALID_HANDLE(jbsch_monitor_thread)) {
        CloseHandle(jbsch_monitor_thread);
        jbsch_monitor_thread = NULL;
    }
}
