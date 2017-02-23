#@author: Michal Karm Babacek
cmake_minimum_required(VERSION 3.3)
project(advertise)

set(CMAKE_C_FLAGS "-std=c99 -Wall")

set(CMAKE_MODULE_PATH ${CMAKE_SOURCE_DIR}/cmake_modules ${CMAKE_MODULE_PATH})

find_package(APR REQUIRED)

include_directories(${APR_INCLUDE_DIR})
include_directories(${APR_LIBRARIES})

set(SOURCE_FILES src/advertise.c)
add_executable(advertise ${SOURCE_FILES})
target_link_libraries(advertise ${APR_LIBRARIES})
