# Some XML food for Magnolia...
FIRSTNODE=""
read -d '' FIRSTNODE <<"EOF"
<sv:node sv:name="@DOWNLOADS@" xmlns:jcr="http://www.jcp.org/jcr/1.0" xmlns:mgnl="http://www.magnolia.info/jcr/mgnl"
  xmlns:mix="http://www.jcp.org/jcr/mix/1.0" xmlns:rep="internal" xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:fn="http://www.w3.org/2005/xpath-functions"
      xmlns:_pre="http://jboss.org" xmlns:xs="http://www.w3.org/2001/XMLSchema"
        xmlns:fn_old="http://www.w3.org/2004/10/xpath-functions" xmlns:sv="http://www.jcp.org/jcr/sv/1.0" xmlns:jcrfn="http://www.jcp.org/jcr/xpath-functions/1.0">
EOF
export FIRSTNODE

ITEMDOWNLOAD_1=""
read -d '' ITEMDOWNLOAD_1 <<"EOF"
  <sv:property sv:name="jcr:primaryType" sv:type="Name">
    <sv:value>mgnl:content</sv:value>
  </sv:property>
  <sv:property sv:name="jcr:mixinTypes" sv:type="Name">
    <sv:value>mix:lockable</sv:value>
  </sv:property>
  <sv:property sv:name="enableRSS" sv:type="String">
    <sv:value>false</sv:value>
  </sv:property>
  <sv:property sv:name="hideInNav" sv:type="String">
    <sv:value>true</sv:value>
  </sv:property>
  <sv:property sv:name="menubar" sv:type="String">
    <sv:value>false</sv:value>
  </sv:property>
  <sv:property sv:name="metaCache" sv:type="String">
    <sv:value>disable</sv:value>
  </sv:property>
  <sv:property sv:name="metaExpire" sv:type="String">
    <sv:value>false</sv:value>
  </sv:property>
  <sv:property sv:name="metaRobots" sv:type="String">
    <sv:value>all</sv:value>
  </sv:property>
  <sv:property sv:name="redirectWindow" sv:type="String">
    <sv:value>false</sv:value>
  </sv:property>
  <sv:property sv:name="title" sv:type="String">
    <sv:value>@VERSION@ Downloads</sv:value>
  </sv:property>
  <sv:property sv:name="useTitle" sv:type="String">
    <sv:value>true</sv:value>
  </sv:property>
  <sv:node sv:name="MetaData">
    <sv:property sv:name="jcr:primaryType" sv:type="Name">
      <sv:value>mgnl:metaData</sv:value>
    </sv:property>
    <sv:property sv:name="mgnl:activated" sv:type="Boolean">
      <sv:value>false</sv:value>
    </sv:property>
    <sv:property sv:name="mgnl:activatorid" sv:type="String">
      <sv:value>superuser</sv:value>
    </sv:property>
    <sv:property sv:name="mgnl:authorid" sv:type="String">
      <sv:value>mod_cluster</sv:value>
    </sv:property>
    <sv:property sv:name="mgnl:template" sv:type="String">
      <sv:value>jbossorgProjectSubPage</sv:value>
    </sv:property>
  </sv:node>
  <sv:node sv:name="orgLayoutHeader">
    <sv:property sv:name="jcr:primaryType" sv:type="Name">
      <sv:value>mgnl:contentNode</sv:value>
    </sv:property>
    <sv:property sv:name="jcr:mixinTypes" sv:type="Name">
      <sv:value>mix:lockable</sv:value>
    </sv:property>
    <sv:property sv:name="headerInheritParagraphsDirection" sv:type="String">
      <sv:value>rootToCurrent</sv:value>
    </sv:property>
    <sv:property sv:name="showHeaderInheritParagraphs" sv:type="String">
      <sv:value>true</sv:value>
    </sv:property>
    <sv:property sv:name="showHeaderInheritParagraphsPos" sv:type="String">
      <sv:value>below</sv:value>
    </sv:property>
    <sv:node sv:name="MetaData">
      <sv:property sv:name="jcr:primaryType" sv:type="Name">
        <sv:value>mgnl:metaData</sv:value>
      </sv:property>
      <sv:property sv:name="mgnl:activated" sv:type="Boolean">
        <sv:value>false</sv:value>
      </sv:property>
      <sv:property sv:name="mgnl:activatorid" sv:type="String">
        <sv:value>superuser</sv:value>
      </sv:property>
      <sv:property sv:name="mgnl:authorid" sv:type="String">
        <sv:value>superuser</sv:value>
      </sv:property>
      <sv:property sv:name="mgnl:template" sv:type="String">
        <sv:value>orgLayoutHeader</sv:value>
      </sv:property>
    </sv:node>
  </sv:node>
  <sv:node sv:name="orgLayoutBody">
    <sv:property sv:name="jcr:primaryType" sv:type="Name">
      <sv:value>mgnl:contentNode</sv:value>
    </sv:property>
    <sv:property sv:name="jcr:mixinTypes" sv:type="Name">
      <sv:value>mix:lockable</sv:value>
    </sv:property>
    <sv:property sv:name="browserTitelBreadcrumb" sv:type="String">
      <sv:value>true</sv:value>
    </sv:property>
    <sv:property sv:name="ignoreInherit" sv:type="String">
      <sv:value>false</sv:value>
    </sv:property>
    <sv:property sv:name="showLeftColumn" sv:type="String">
      <sv:value>false</sv:value>
    </sv:property>
    <sv:property sv:name="showRightColumn" sv:type="String">
      <sv:value>false</sv:value>
    </sv:property>
    <sv:node sv:name="MetaData">
      <sv:property sv:name="jcr:primaryType" sv:type="Name">
        <sv:value>mgnl:metaData</sv:value>
      </sv:property>
      <sv:property sv:name="mgnl:activated" sv:type="Boolean">
        <sv:value>false</sv:value>
      </sv:property>
      <sv:property sv:name="mgnl:activatorid" sv:type="String">
        <sv:value>superuser</sv:value>
      </sv:property>
      <sv:property sv:name="mgnl:authorid" sv:type="String">
        <sv:value>superuser</sv:value>
      </sv:property>
      <sv:property sv:name="mgnl:template" sv:type="String">
        <sv:value>orgLayoutBody</sv:value>
      </sv:property>
    </sv:node>
  </sv:node>
  <sv:node sv:name="mainColumnParagraphs">
    <sv:property sv:name="jcr:primaryType" sv:type="Name">
      <sv:value>mgnl:contentNode</sv:value>
    </sv:property>
    <sv:property sv:name="jcr:mixinTypes" sv:type="Name">
      <sv:value>mix:lockable</sv:value>
    </sv:property>
    <sv:node sv:name="MetaData">
      <sv:property sv:name="jcr:primaryType" sv:type="Name">
        <sv:value>mgnl:metaData</sv:value>
      </sv:property>
      <sv:property sv:name="mgnl:activated" sv:type="Boolean">
        <sv:value>false</sv:value>
      </sv:property>
      <sv:property sv:name="mgnl:activatorid" sv:type="String">
        <sv:value>superuser</sv:value>
      </sv:property>
      <sv:property sv:name="mgnl:authorid" sv:type="String">
        <sv:value>mod_cluster</sv:value>
      </sv:property>
    </sv:node>
    <sv:node sv:name="0">
      <sv:property sv:name="jcr:primaryType" sv:type="Name">
        <sv:value>mgnl:contentNode</sv:value>
      </sv:property>
      <sv:property sv:name="jcr:mixinTypes" sv:type="Name">
        <sv:value>mix:lockable</sv:value>
      </sv:property>
      <sv:property sv:name="popup" sv:type="String">
        <sv:value>true</sv:value>
      </sv:property>
      <sv:property sv:name="showDescription" sv:type="String">
        <sv:value>true</sv:value>
      </sv:property>
      <sv:property sv:name="showLicense" sv:type="String">
        <sv:value>true</sv:value>
      </sv:property>
      <sv:property sv:name="showReleaseDate" sv:type="String">
        <sv:value>true</sv:value>
      </sv:property>
      <sv:property sv:name="showSize" sv:type="String">
        <sv:value>true</sv:value>
      </sv:property>
      <sv:property sv:name="tableDesc" sv:type="String">
        <sv:value>mod_cluster native modules for your Apache HTTP Server and java bundles for your JBoss AS or Tomcat.</sv:value>
      </sv:property>
      <sv:property sv:name="tableTitle" sv:type="String">
        <sv:value>mod_cluster</sv:value>
      </sv:property>
EOF
export ITEMDOWNLOAD_1

ITEMDOWNLOAD_2=""
read -d '' ITEMDOWNLOAD_2 <<"EOF"
      <sv:node sv:name="MetaData">
        <sv:property sv:name="jcr:primaryType" sv:type="Name">
          <sv:value>mgnl:metaData</sv:value>
        </sv:property>
        <sv:property sv:name="mgnl:activated" sv:type="Boolean">
          <sv:value>false</sv:value>
        </sv:property>
        <sv:property sv:name="mgnl:activatorid" sv:type="String">
          <sv:value>superuser</sv:value>
        </sv:property>
        <sv:property sv:name="mgnl:authorid" sv:type="String">
          <sv:value>mod_cluster</sv:value>
        </sv:property>
        <sv:property sv:name="mgnl:template" sv:type="String">
          <sv:value>downloads</sv:value>
        </sv:property>
      </sv:node>
    </sv:node>
  </sv:node>
</sv:node>
EOF
export ITEMDOWNLOAD_2