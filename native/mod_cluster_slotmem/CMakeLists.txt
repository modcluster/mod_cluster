#==================================
# mod_cluster_slotmem CMake file
# @author: Michal Karm Babacek
#==================================

CMAKE_MINIMUM_REQUIRED(VERSION 2.8)
PROJECT(mod_cluster_slotmem)

SET(PROJECT_INCLUDE_DIR ${PROJECT_SOURCE_DIR}/include)
SET(PROJECT_SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR})

SET(MOD_CLUSTER_SLOTMEM_SRCS
        ${PROJECT_SOURCE_DIR}/mod_sharedmem.c
        ${PROJECT_SOURCE_DIR}/sharedmem_util.c
)

INCLUDE_DIRECTORIES("${PROJECT_BINARY_DIR}")
INCLUDE_DIRECTORIES("${PROJECT_INCLUDE_DIR}")

ADD_LIBRARY(mod_cluster_slotmem MODULE ${MOD_CLUSTER_SLOTMEM_SRCS})
SET_TARGET_PROPERTIES(mod_cluster_slotmem PROPERTIES PREFIX "")
SET_TARGET_PROPERTIES(mod_cluster_slotmem PROPERTIES SUFFIX ".so")
TARGET_LINK_LIBRARIES(mod_cluster_slotmem ${APR_LIBRARIES} ${APRUTIL_LIBRARIES} ${APACHE_LIBRARY})