cmake_minimum_required(VERSION 3.16)
project(ckgame-cpp LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(MSVC)
    add_compile_options(/utf-8 /W4)
else()
    add_compile_options(-Wall -Wextra)
endif()

add_executable(ckgame
    src/main.cpp
    src/core/calendar/date.cpp
    src/core/stats/attribute_set.cpp
    src/core/traits/traits.cpp
    src/core/balance/balance.cpp
    src/core/gender.cpp
    src/core/title_tier.cpp
)

target_include_directories(ckgame PRIVATE src)
