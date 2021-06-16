cmake_minimum_required(VERSION 3.6)

set(target carclient)
project(${target} CXX)


set(RPCLIB_DEFAULT_PORT 8080
  CACHE STRING "Default port used for running tests and examples")
set(RPCLIB_DEFAULT_BUFFER_SIZE "1024 << 10"
  CACHE STRING "Default buffer size")
set(RPCLIB_CXX_STANDARD 11 CACHE STRING
  "C++ version used to build rpclib (Currently: Only 11 and 14 supported)")

if(NOT ${RPCLIB_CXX_STANDARD} EQUAL 14 AND
   NOT ${RPCLIB_CXX_STANDARD} EQUAL 11)
  message(fatal_error "Unsupported C++ standard: ${RPCLIB_CXX_STANDARD}")
endif()

set(CMAKE_CXX_STANDARD ${RPCLIB_CXX_STANDARD})

set(ANDROID_OPENCV_COMPONENTS "opencv_java" CACHE STRING "")
message(STATUS "ANDROID_ABI=${ANDROID_ABI}")
find_package(OpenCV REQUIRED COMPONENTS ${ANDROID_OPENCV_COMPONENTS})

set(RPCLIB_DEPENDENCIES "${CMAKE_CURRENT_LIST_DIR}/dependencies")

file(GLOB_RECURSE RPCLIB_HEADERS
  include/rpc/*.h
  include/msgpack/*.hpp)
file(GLOB_RECURSE DEP_HEADERS
  ${RPCLIB_DEPENDENCIES}/include/*.h
  ${RPCLIB_DEP_LIBRARIDEPENDENCIES}/include/*.hpp)

file(GLOB srcs *.cpp *.c)
file(GLOB hdrs *.hpp *.h)

set(DEP_SOURCES
  ${RPCLIB_DEPENDENCIES}/src/format.cc
  ${RPCLIB_DEPENDENCIES}/src/posix.cc)

set(AIRSIM_SOURCES
  lib/api/RpcLibClientBase.cpp
  lib/vehicles/car/api/CarRpcLibClient.cpp)

add_library(${PROJECT_NAME} SHARED
  lib/rpc/dispatcher.cc
  lib/rpc/server.cc
  lib/rpc/client.cc
  lib/rpc/this_handler.cc
  lib/rpc/this_session.cc
  lib/rpc/this_server.cc
  lib/rpc/rpc_error.cc
  lib/rpc/detail/server_session.cc
  lib/rpc/detail/response.cc
  lib/rpc/detail/client_error.cc
  lib/rpc/nonstd/optional.cc
  ${DEP_SOURCES}
  ${DEP_HEADERS}
  ${RPCLIB_HEADERS}
  ${AIRSIM_SOURCES}
  carclient.cpp)


if (RPCLIB_EXTRA_BUILD_FLAGS)
  list(APPEND RPCLIB_BUILD_FLAGS ${RPCLIB_EXTRA_BUILD_FLAGS})
endif()

target_compile_definitions(${PROJECT_NAME}
  PRIVATE
    "${RPCLIB_COMPILE_DEFINITIONS}"
    "${RPCLIB_ARCH_DEF}"
    "ASIO_STANDALONE"
    "RPCLIB_ASIO=clmdep_asio"
    "RPCLIB_FMT=clmdep_fmt"
  PUBLIC
    "${RPCLIB_OS_DEF}"
    "RPCLIB_MSGPACK=clmdep_msgpack"
  )

include_directories("${CMAKE_CURRENT_LIST_DIR}")

target_link_libraries(${target} ${ANDROID_OPENCV_COMPONENTS})

target_link_libraries(${PROJECT_NAME} ${RPCLIB_DEP_LIBRARIES})
target_include_directories(
  ${PROJECT_NAME} PUBLIC
  $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>
  $<INSTALL_INTERFACE:include>
  )
target_include_directories(
  ${PROJECT_NAME} SYSTEM
  PRIVATE ${RPCLIB_DEPENDENCIES}/include
  )

