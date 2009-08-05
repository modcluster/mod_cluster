dnl modules enabled in this directory by default

APACHE_MODPATH_INIT(mod_manager)

manager_objects="mod_manager.lo balancer.lo  context.lo  host.lo node.lo"

APACHE_MODULE(manager, Manager for mod_cluster, $manager_objects, , no)

if test "$manager_enable" != "no"; then
  APR_ADDTO(INCLUDES, [-I\$(top_srcdir)/../proxy/])
fi

APACHE_MODPATH_FINISH
