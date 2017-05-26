cmake_minimum_required(VERSION 2.8)

# custom directories
set(INCLUDES_DIR includes)
set(TEMPLATES_DIR templates/cpp)
set(CHECKERS_DIR templates)

# IDE project name
project("Contest")

# remove ZERO_CHECK target
set(CMAKE_SUPPRESS_REGENERATION true)

# C++11 standard
set(CMAKE_CXX_STANDARD 11)

# custom preprocessor definitions to distinguish local and online judge environment
add_definitions(-DAlCash)

# increase stack size
set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -Wl,-stack_size,1000000000")

# platform specific stuff
if(APPLE)
    if(NOT IOS)
        # AppCode support
        set(CMAKE_OSX_SYSROOT macosx)
        set(CMAKE_XCODE_ATTRIBUTE_CXX_LANGUAGE_STANDARD "с++11")
        set(CMAKE_XCODE_ATTRIBUTE_CLANG_CXX_LIBRARY "libc++")
        set(CMAKE_XCODE_ATTRIBUTE_GCC_WARN_UNUSED_VARIABLE "NO")
    endif()
elseif(UNIX)
    # multithreading support on unix for GCJ and FHC
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -pthread")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -pthread")
endif()

# common include directories for all targets
include_directories(${INCLUDES_DIR} ${TEMPLATES_DIR})

# make includes show up in IDE as a separate group
# preserving directory structure
file(GLOB_RECURSE HEADER_FILES ${INCLUDES_DIR}/*.h)
get_filename_component(FULL_INCLUDES_PATH ${INCLUDES_DIR} ABSOLUTE)
foreach(FILE_NAME ${HEADER_FILES})
    get_filename_component(ABSOLUTE_FILE_NAME ${FILE_NAME} ABSOLUTE)
    file(RELATIVE_PATH FILE_RELATIVE_PATH ${FULL_INCLUDES_PATH} ${ABSOLUTE_FILE_NAME})
    get_filename_component(FILE_DIRECTORY ${FILE_RELATIVE_PATH} DIRECTORY)
    set(FILE_DIRECTORY "\\${FILE_DIRECTORY}")
    string(REPLACE / \\ FILE_DIRECTORY ${FILE_DIRECTORY})
    source_group("${FILE_DIRECTORY}" FILES "${FILE_NAME}")
endforeach()
add_custom_target(includes SOURCES ${HEADER_FILES})

# library consisting only of tester main function
source_group("" FILES ${TEMPLATES_DIR}/tester.cpp)
add_library(tester ${TEMPLATES_DIR}/tester.cpp)

# problems

set(PROBLEM_DIR @problem_dir@)
# make test files show up in IDE along with source file
file(GLOB PROBLEM_FILES ${PROBLEM_DIR}/source.cpp ${PROBLEM_DIR}/limits.cpp
        ${PROBLEM_DIR}/*.in ${PROBLEM_DIR}/*.out ${PROBLEM_DIR}/*.ans)
source_group("" FILES ${PROBLEM_FILES})

if(@interactive@)
    # run problem solution with console I/O
    add_executable(@problem_id@ ${PROBLEM_FILES} ${PROBLEM_DIR}/main.cpp)
else()
    add_executable(@problem_id@ ${PROBLEM_FILES})
    # link problem solution with tester library that will run it on all the tests
    target_link_libraries(@problem_id@ tester)
    # create a file alongside the binary pointing to tests location
    add_custom_command(TARGET @problem_id@ POST_BUILD COMMAND
        echo ${PROJECT_SOURCE_DIR}/${PROBLEM_DIR}/tests > $<TARGET_FILE_DIR:@problem_id@>/@problem_id@Path)
endif()

if(@custom_checker@)
    # custom checker target
    set(CHECKER_FILE {PROBLEM_DIR}/checker.cpp)
    configure_file(${CHECKERS_DIR}/checker.cpp ${CHECKER_FILE})
    source_group("" FILES ${CHECKER_FILE})
    add_executable(@problem_id@Checker ${CHECKER_FILE})
endif()
