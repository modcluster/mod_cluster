APACHE_BASE = /home/jbento/modclusterdemo/opt/jboss/httpd/
APACHE_INC  = /home/jbento/modclusterdemo/opt/jboss/httpd/include/httpd

Advertise: Advertise.c
	cc -c -I$(APACHE_INC) Advertise.c
	cc -o Advertise Advertise.o -L$(APACHE_BASE)/lib -lapr-1
