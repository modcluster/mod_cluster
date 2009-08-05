dnl modules enabled in this directory by default

APACHE_MODPATH_INIT(mod_slotmem)

slotmem_objects="mod_sharedmem.lo sharedmem_util.lo"

APACHE_MODULE(slotmem, A slotmem provider for mod_cluster, $slotmem_objects, , no)

APACHE_MODPATH_FINISH
