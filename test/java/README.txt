The test directory contains to kind of tests:
1 - small test using httpclient that allows to read directly information from httpd (TestHttpClient).
2 - small test suite:
    to run it:
    mvn test
    to run a single test:
    mvn -Dtest=mytest test where mytest is for example Test_Chunk_JBWEB_117
3 - JBW listener:
    to test the JBW cluster listener use -Dcluster=false for example:
    mvn -Dtest=Test_Chunk_JBWEB_117 -Dcluster=false test
4 - httpd is installed and started via ant 
    ant httpd
    to stop it:
    ant stophttpd

NOTE the httpd should have something like the following in httpd.conf
<IfModule manager_module>
   Listen jfcpc:6666
   <VirtualHost jfcpc:6666>
    <Directory />
      Order deny,allow
      Deny from all
      Allow from 10.33.144
    </Directory>

   KeepAliveTimeout 300
   MaxKeepAliveRequests 0
   AdvertiseFrequency 5
   EnableMCPMReceive
   </VirtualHost>
</IfModule>
(replace jfpc by your hostname or your IP address)
(replace 10.33.144 by the subnet you want to allow)
