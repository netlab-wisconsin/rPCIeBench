cmake_minimum_required(VERSION 3.7)
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED True)

project(qdma_benchmark LANGUAGES CXX VERSION 0.1)

add_definitions(-mavx512f)
add_compile_options(-O3 -march=native)

add_executable(CommSub
	CommSub.cpp
	tests.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/dma/dma.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/mmio/mmio.cpp
)
target_include_directories(CommSub PUBLIC
        ${CMAKE_SOURCE_DIR}/dma
        ${CMAKE_CURRENT_SOURCE_DIR}/mmio
)
target_link_libraries(CommSub
        qdma
        pthread
)

