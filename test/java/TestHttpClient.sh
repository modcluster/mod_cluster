#
# Copyright 2008 Red Hat Middleware, LLC.
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software distributed
# under the License is distributed on an "AS IS" BASIS,i
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
CLASSES=$HOME/java/commons-httpclient-3.1/commons-httpclient-3.1.jar
javac -classpath $CLASSES *Method.java
javac -classpath $CLASSES:. TestHttpClient.java
CLASSES=$CLASSES:$HOME/java/commons-logging-1.0.4/commons-logging.jar:$HOME/java/commons-codec-1.3/commons-codec-1.3.jar:.

HTTPD=localhost
JVMROUTE=node1
HOST=localhost
SCHEME=http
SECURITY="-Djavax.net.ssl.trustStore=$HOME/.keystore -Djavax.net.ssl.keyStore=$HOME/CERTS/demoCA/test.p12 -Djavax.net.ssl.keyStorePassword=changeit -Djavax.net.ssl.keyStoreType=\"PKCS12\""


# Send a CONFIG command.
java ${SECURITY} -classpath $CLASSES -DJVMRoute=${JVMROUTE} -DHost=${HOST} -DJVMRoute=${JVMROUTE} -DHost=${HOST} TestHttpClient ${SCHEME}://${HTTPD}:6666/test_bla/ CONFIG

java ${SECURITY} -classpath $CLASSES -DJVMRoute=${JVMROUTE} -DHost=${HOST} TestHttpClient ${SCHEME}://${HTTPD}:6666/test_bla/ DUMP

# Send a ENABLE for /myapp
java ${SECURITY} -classpath $CLASSES -DJVMRoute=${JVMROUTE} -DHost=${HOST} TestHttpClient ${SCHEME}://${HTTPD}:6666/test_bla/ ENABLE

java ${SECURITY} -classpath $CLASSES -DJVMRoute=${JVMROUTE} -DHost=${HOST} TestHttpClient ${SCHEME}://${HTTPD}:6666/test_bla/ DUMP

# Send a REMOVE for /hisapp
java ${SECURITY} -classpath $CLASSES -DJVMRoute=${JVMROUTE} -DHost=${HOST} TestHttpClient ${SCHEME}://${HTTPD}:6666/test_bla/ REMOVE

java ${SECURITY} -classpath $CLASSES -DJVMRoute=${JVMROUTE} -DHost=${HOST} TestHttpClient ${SCHEME}://${HTTPD}:6666/test_bla/ DUMP

java ${SECURITY} -classpath $CLASSES -DJVMRoute=${JVMROUTE} -DHost=${HOST} TestHttpClient ${SCHEME}://${HTTPD}:6666/test_bla/ STATUS

#java ${SECURITY} -classpath $CLASSES -DJVMRoute=${JVMROUTE} -DHost=${HOST} TestHttpClient ${SCHEME}://${HTTPD}:6666/test_bla/ INFO
