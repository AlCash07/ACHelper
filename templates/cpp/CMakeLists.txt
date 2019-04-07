cmake_minimum_required(VERSION 3.9)

# custom directories
set(INCLUDES_DIR templates)
set(TEMPLATES_DIR templates/cpp)
set(CHECKERS_DIR templates)
set(CODE_INLINER ../caide-cpp-inliner/build/cmd/cmd)

# IDE project name
get_filename_component(PROJECT_NAME ${CMAKE_SOURCE_DIR} NAME)
project(${PROJECT_NAME})

# C++ standard
set(CMAKE_CXX_STANDARD 17)

# custom preprocessor definitions to distinguish local and online judge environment
add_definitions(-DAlCash)

# increase stack size
set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -Wl,-stack_size,1000000000")

# create only the necessary configuration types
set(CMAKE_CONFIGURATION_TYPES Debug RelWithDebInfo)

# remove ZERO_CHECK target
set(CMAKE_SUPPRESS_REGENERATION true)

# platform specific stuff
if(APPLE)
    if(NOT IOS)
        # AppCode support
        set(CMAKE_OSX_SYSROOT macosx)
        set(CMAKE_XCODE_ATTRIBUTE_CXX_LANGUAGE_STANDARD "с++${CMAKE_CXX_STANDARD}")
        set(CMAKE_XCODE_ATTRIBUTE_CLANG_CXX_LIBRARY "libc++")
        set(CMAKE_XCODE_ATTRIBUTE_GCC_WARN_UNUSED_VARIABLE "NO")
        set(CMAKE_XCODE_GENERATE_SCHEME TRUE)
    endif()
elseif(UNIX)
    # multithreading support on unix for FHC
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -pthread")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -pthread")
endif()

# common include directories for all targets
include_directories(${INCLUDES_DIR} ${TEMPLATES_DIR})

# make includes show up in IDE as a separate group
# preserving directory structure
file(GLOB_RECURSE HEADER_FILES ${INCLUDES_DIR}/*.h ${INCLUDES_DIR}/*.hpp)
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

# get system include directories for the code inliner
execute_process(COMMAND g++ -v -x c++ -E /dev/null
        OUTPUT_VARIABLE GCC_OUTPUT ERROR_VARIABLE GCC_OUTPUT)
string(REGEX REPLACE "^.*#include <...> search starts here:.|End of search list..*$" ""
        GCC_OUTPUT ${GCC_OUTPUT})
string(REGEX REPLACE "[^|\n][^\n]*\\(framework directory\\)" "" GCC_OUTPUT ${GCC_OUTPUT})
string(REGEX REPLACE "^ |\n |\n" ";" SYSTEM_DIRS ${GCC_OUTPUT})
foreach(SYSTEM_DIR ${SYSTEM_DIRS})
    if(NOT ${SYSTEM_DIR} STREQUAL "")
        list(APPEND INLINER_PARAMS -isystem ${SYSTEM_DIR})
    endif()
endforeach()

# set other common inliner params
string(REPLACE 17 1z CAIDE_CXX_STANDARD ${CMAKE_CXX_STANDARD})
list(APPEND INLINER_PARAMS "-std=c++${CAIDE_CXX_STANDARD}")
list(APPEND INLINER_PARAMS -nostdinc -fparse-all-comments -DONLINE_JUDGE)
list(APPEND INLINER_PARAMS -I ${INCLUDES_DIR} -I ${TEMPLATES_DIR} --)
file(MAKE_DIRECTORY ${CMAKE_BINARY_DIR}/caide)
list(APPEND INLINER_PARAMS -l 1 -d ${CMAKE_BINARY_DIR}/caide -o submission.cpp)

# target to view the submission code and validate that it actually compiles :)
file(WRITE submission.cpp)
add_executable(submission submission.cpp)
source_group("" FILES submission.cpp)

# problems

set(PROBLEM_DIR @problem_dir@)
# make test files show up in IDE along with source file
file(GLOB PROBLEM_FILES ${PROBLEM_DIR}/source.cpp ${PROBLEM_DIR}/problem_limits.cpp
        ${PROBLEM_DIR}/*.txt ${PROBLEM_DIR}/*.in ${PROBLEM_DIR}/*.out ${PROBLEM_DIR}/*.ans)
if(@interactive@)
    # run problem solution with console I/O
    set(PROBLEM_FILES ${PROBLEM_FILES} ${PROBLEM_DIR}/main.cpp)
    add_executable(@problem_id@ ${PROBLEM_FILES})
    if(@platform_id@ STREQUAL new-gcj)
        # automatically run interactor provided by Google
        add_custom_command(TARGET @problem_id@ POST_BUILD COMMAND
                python ${PROBLEM_DIR}/testing_tool.py $<TARGET_FILE_DIR:@problem_id@>/@problem_id@)
    endif()
else()
    add_executable(@problem_id@ ${PROBLEM_FILES})
    # link problem solution with tester library that will run it on all the tests
    target_link_libraries(@problem_id@ tester)
    # create a file alongside the binary pointing to tests location
    add_custom_command(TARGET @problem_id@ POST_BUILD COMMAND ${CMAKE_COMMAND} -E
            echo ${PROBLEM_DIR}/tests > $<TARGET_FILE_DIR:@problem_id@>/@problem_id@Path)
endif()
source_group("" FILES ${PROBLEM_FILES})

# run code inliner in the background after the build
add_custom_command(TARGET @problem_id@ POST_BUILD COMMAND
        scripts/launcher.sh
        ARGS ${CODE_INLINER} ${INLINER_PARAMS} ${PROBLEM_DIR}/source.cpp ${PROBLEM_DIR}/main.cpp
        WORKING_DIRECTORY ${CMAKE_SOURCE_DIR})

if(@custom_checker@)
    # custom checker target
    set(CHECKER_FILE ${PROBLEM_DIR}/checker.cpp)
    source_group("" FILES ${CHECKER_FILE})
    add_executable(@problem_id@Checker ${CHECKER_FILE})
    target_include_directories(@problem_id@Checker PRIVATE ${CHECKERS_DIR})
endif()
