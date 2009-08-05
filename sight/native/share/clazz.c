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

#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"

int sight_load_class(JNIEnv *e, JAVA_C_ID *clazz)
{
    jobject c;
    char an[SIGHT_SBUFFER_SIZ];

    if (clazz->i != NULL) {
        return 0;
    }
    if ((*e)->EnsureLocalCapacity(e, 3) < 0)
        goto failed;
    c = (jobject)(*e)->FindClass(e, clazz->n);
    if ((*e)->ExceptionCheck(e) || c == NULL) {
        goto failed;
    }
    clazz->i = (jclass)(*e)->NewGlobalRef(e, c);
    if ((*e)->ExceptionCheck(e) || clazz->i == NULL) {
        clazz->i = NULL;
        goto failed;
    }
    (*e)->DeleteLocalRef(e, c);

    /* Init class array */
    sprintf(an, "L%s;", clazz->n);
    c = (jobject)(*e)->FindClass(e, an);
    if ((*e)->ExceptionCheck(e) || c == NULL) {
        goto failed;
    }
    clazz->a = (jclass)(*e)->NewGlobalRef(e, c);
    if ((*e)->ExceptionCheck(e) || clazz->a == NULL) {
        clazz->a = NULL;
        goto failed;
    }
    return 0;

failed:
    if (clazz->i != NULL) {
        (*e)->DeleteGlobalRef(e, clazz->i);
        clazz->i = NULL;
    }
    return -1;

}

void sight_unload_class(JNIEnv *e, JAVA_C_ID *clazz)
{
    if (clazz->i != NULL) {
        (*e)->DeleteGlobalRef(e, clazz->i);
        clazz->i = NULL;
    }
    if (clazz->a != NULL) {
        (*e)->DeleteGlobalRef(e, clazz->a);
        clazz->a = NULL;
    }
}

SIGHT_CLASS_LDEC(Cpu);
SIGHT_CLASS_LDEC(CpuStatistic);
SIGHT_CLASS_LDEC(FileInfo);
SIGHT_CLASS_LDEC(Group);
SIGHT_CLASS_LDEC(Memory);
SIGHT_CLASS_LDEC(NativeObject);
SIGHT_CLASS_LDEC(Process);
SIGHT_CLASS_LDEC(User);
SIGHT_CLASS_LDEC(Service);
SIGHT_CLASS_LDEC(Module);
SIGHT_CLASS_LDEC(Mutex);
SIGHT_CLASS_LDEC(Network);
SIGHT_CLASS_LDEC(NetworkAdapter);
SIGHT_CLASS_LDEC(NetworkAddress);
SIGHT_CLASS_LDEC(Volume);
SIGHT_CLASS_LDEC(TcpConnection);
SIGHT_CLASS_LDEC(TcpStatistics);
SIGHT_CLASS_LDEC(UdpConnection);
SIGHT_CLASS_LDEC(UdpStatistics);

#ifdef WIN32
SIGHT_CLASS_LDEC(Registry);
SIGHT_CLASS_LDEC(VARIANT);
#endif

int sight_load_classes(JNIEnv *_E)
{
    SIGHT_CLASS_LCAL(Cpu);
    SIGHT_CLASS_LCAL(CpuStatistic);
    SIGHT_CLASS_LCAL(FileInfo);
    SIGHT_CLASS_LCAL(Group);
    SIGHT_CLASS_LCAL(Memory);
    SIGHT_CLASS_LCAL(NativeObject);
    SIGHT_CLASS_LCAL(Process);
    SIGHT_CLASS_LCAL(User);
    SIGHT_CLASS_LCAL(Service);
    SIGHT_CLASS_LCAL(Module);
    SIGHT_CLASS_LCAL(Mutex);
    SIGHT_CLASS_LCAL(Network);
    SIGHT_CLASS_LCAL(NetworkAdapter);
    SIGHT_CLASS_LCAL(NetworkAddress);
    SIGHT_CLASS_LCAL(Volume);
    SIGHT_CLASS_LCAL(TcpConnection);
    SIGHT_CLASS_LCAL(TcpStatistics);
    SIGHT_CLASS_LCAL(UdpConnection);
    SIGHT_CLASS_LCAL(UdpStatistics);
#ifdef WIN32
    SIGHT_CLASS_LCAL(Registry);
    SIGHT_CLASS_LCAL(VARIANT);
#endif
    return 0;
}

void sight_unload_classes(JNIEnv *_E)
{
    SIGHT_CLASS_UCAL(Cpu);
    SIGHT_CLASS_UCAL(CpuStatistic);
    SIGHT_CLASS_UCAL(FileInfo);
    SIGHT_CLASS_UCAL(Group);
    SIGHT_CLASS_UCAL(Memory);
    SIGHT_CLASS_UCAL(NativeObject);
    SIGHT_CLASS_UCAL(Process);
    SIGHT_CLASS_UCAL(User);
    SIGHT_CLASS_UCAL(Service);
    SIGHT_CLASS_UCAL(Module);
    SIGHT_CLASS_UCAL(Mutex);
    SIGHT_CLASS_UCAL(Network);
    SIGHT_CLASS_UCAL(NetworkAdapter);
    SIGHT_CLASS_UCAL(NetworkAddress);
    SIGHT_CLASS_UCAL(Volume);
    SIGHT_CLASS_UCAL(TcpConnection);
    SIGHT_CLASS_UCAL(TcpStatistics);
    SIGHT_CLASS_UCAL(UdpConnection);
    SIGHT_CLASS_UCAL(UdpStatistics);
#ifdef WIN32
    SIGHT_CLASS_UCAL(Registry);
    SIGHT_CLASS_UCAL(VARIANT);
#endif

}
