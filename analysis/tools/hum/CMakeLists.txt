# minidaw-hum: a tool of :analysis on the host (development_roadmap.md TK-009). It converts WAV files
# to Notes with one pitch candidate, for the evaluation on the public set vocadito.
add_executable(minidaw-hum
  hum.cpp)
target_link_libraries(minidaw-hum PRIVATE analysis-core wavio)
target_compile_options(minidaw-hum PRIVATE -Wall -Wextra -Wpedantic -Werror)
if(TARGET analysis-swiftf0)
  target_link_libraries(minidaw-hum PRIVATE analysis-swiftf0)
  target_compile_definitions(minidaw-hum PRIVATE MINIDAW_HAVE_SWIFTF0=1)
endif()
