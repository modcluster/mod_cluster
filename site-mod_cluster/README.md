mod_cluster site
================
Here are the scripts to build the mod_cluster site.
Basically the downloads and the corresponding page.
The scripts need to be edited: to change the version for example.

Steps
-----
 * change VERSION in constants.sh
 * run files_downloader.sh
 * run the rsync command described at the end of files_downloader.sh
 * run gen.downloads.sh
 * import the resulting XML into Magnolia
 * enable it in navigation...
 * generate the docs

    cd ../docs; mvn install
    copy it
    cd ../docs/userguide/target/docbook/publish/en-US
    scp -rp html mod_cluster@filemgmt.jboss.org:/docs_htdocs/mod_cluster/1.x.0
    scp -rp html_single mod_cluster@filemgmt.jboss.org:/docs_htdocs/mod_cluster/1.x.0
    scp -rp pdf mod_cluster@filemgmt.jboss.org:/docs_htdocs/mod_cluster/1.x.0
