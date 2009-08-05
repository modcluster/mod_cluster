dnl modules enabled in this directory by default

APACHE_MODPATH_INIT(jk)

jk_objects="mod_jk.lo jk_ajp12_worker.lo jk_ajp13.lo jk_ajp13_worker.lo jk_ajp14.lo jk_ajp14_worker.lo jk_ajp_common.lo jk_connect.lo jk_context.lo jk_lb_worker.lo jk_map.lo jk_md5.lo jk_msg_buff.lo jk_pool.lo jk_shm.lo jk_sockbuf.lo jk_status.lo jk_uri_worker_map.lo jk_url.lo jk_util.lo jk_worker.lo"

APACHE_MODULE(jk, AJP protocol handling, $jk_objects, , no)

if test "$jk_enable" != "no"; then
  APR_ADDTO(INCLUDES, [-I\$(top_srcdir)/$modpath_current/common])
fi

APACHE_MODPATH_FINISH
