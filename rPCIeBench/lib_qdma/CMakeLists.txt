cmake_minimum_required(VERSION 3.7)
set(CMAKE_CXX_STANDARD 11)

project(qdma LANGUAGES CXX)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2 -fPIC ")
add_definitions(-mavx512f)

set(SOURCE_FILES
	QDMAController.cpp
)

set(HEAD_FILES 
	QDMAController.h
)

add_library(qdma SHARED ${SOURCE_FILES})

install(
	TARGETS qdma
	DESTINATION /usr/lib64
)

install(
	FILES ${HEAD_FILES}
	DESTINATION /usr/include)