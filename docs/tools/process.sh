sed 's:*::' list > $$.list
while read a
do
  text=`echo $a | awk --field-separator " - " ' { print $2 } '`
  bug=`echo $a | awk --field-separator " - " ' { print $1 } ' | sed "s:]::" | sed "s:\[::" `
  # <para>@@TEXT@@ (@@JIRA@@) (@@AUTHOR@@)</para>
  echo "s:@@TEXT@@:$text:" > sed.cmd
  echo "s:@@JIRA@@:$bug:" >> sed.cmd
  echo "s:@@AUTHOR@@:jfclere:" >> sed.cmd
  sed -f sed.cmd add.xml
done < $$.list
rm $$.list
