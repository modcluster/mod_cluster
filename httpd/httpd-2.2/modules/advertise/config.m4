dnl modules enabled in this directory by default

APACHE_MODPATH_INIT(advertise)

if test "$enable_advertise" = "shared"; then
  advertise_mods_enable=shared
elif test "$enable_advertise" = "yes"; then
  advertise_mods_enable=yes
else
  advertise_mods_enable=no
fi

advertise_objs="mod_advertise.lo"
APACHE_MODULE(advertise, Apache advertise module, $advertise_objs, , $advertise_mods_enable)


APR_ADDTO(INCLUDES, [-I\$(top_srcdir)/$modpath_current/../generators])

APACHE_MODPATH_FINISH
