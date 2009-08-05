The test directory contains to kind of tests:
1 - small test using httpclient that allows to read directly information from httpd (TestHttpClient).
2 - small test suite:
    to run it:
    ant
    to run a single test:
    ant one -Dtest=test where test is for example Test_Chunk_JBWEB_117
    to run just the test (without httpd installation)
    ant tests
3 - JBW listener:
    to test the JBW cluster listener use -Dcluster=false for example:
    ant one -Dtest=Test_Chunk_JBWEB_117 -Dcluster=false
4 - To run the test with an httpd already installed and running:
    ant extra

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
   </VirtualHost>
</IfModule>
(replace jfpc by your hostname or your IP address)
(replace 10.33.144 by the subnet you want to allow)
