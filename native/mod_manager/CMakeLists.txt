#==================================
# mod_cluster_slotmem CMake file
# @author: Michal Karm Babacek
#==================================

CMAKE_MINIMUM_REQUIRED(VERSION 2.8)
PROJECT(mod_manager)

SET(PROJECT_INCLUDE_DIR ${PROJECT_SOURCE_DIR}/include)
SET(PROJECT_SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR})

SET(MOD_MANAGER_SRCS    ${PROJECT_SOURCE_DIR}/mod_manager.c
        ${PROJECT_SOURCE_DIR}/balancer.c
        ${PROJECT_SOURCE_DIR}/context.c
        ${PROJECT_SOURCE_DIR}/domain.c
        ${PROJECT_SOURCE_DIR}/host.c
        ${PROJECT_SOURCE_DIR}/jgroupsid.c
        ${PROJECT_SOURCE_DIR}/node.c
        ${PROJECT_SOURCE_DIR}/sessionid.c
)

INCLUDE_DIRECTORIES("${PROJECT_BINARY_DIR}")
INCLUDE_DIRECTORIES("${PROJECT_INCLUDE_DIR}")

ADD_LIBRARY(mod_manager MODULE ${MOD_MANAGER_SRCS})
SET_TARGET_PROPERTIES(mod_manager PROPERTIES PREFIX "")
SET_TARGET_PROPERTIES(mod_manager PROPERTIES SUFFIX ".so")
TARGET_LINK_LIBRARIES(mod_manager ${APR_LIBRARIES} ${APRUTIL_LIBRARIES} ${APACHE_LIBRARY})