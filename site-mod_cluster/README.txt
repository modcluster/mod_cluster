Here are the scripts to build the mod_cluster site.
Basically the downloads and the corresponding page.
The scripts need to be edited: to change the version for example.
1 - create a directory for the version.
2 - run files.sh
3 - send to download
    scp -rp dir mod_cluster@filemgmt.jboss.org:/downloads_htdocs/mod_cluster
4 - gen.downloads.sh > toto.xml
5 - import it in magnolia.
6 - generate the docs
    cd ../docs; mvn install
7 - copy it
    cd ../docs/userguide/target/docbook/publish/en-US
    scp -rp html mod_cluster@filemgmt.jboss.org:/docs_htdocs/mod_cluster/1.x.0
    scp -rp html_single mod_cluster@filemgmt.jboss.org:/docs_htdocs/mod_cluster/1.x.0
    scp -rp pdf mod_cluster@filemgmt.jboss.org:/docs_htdocs/mod_cluster/1.x.0
