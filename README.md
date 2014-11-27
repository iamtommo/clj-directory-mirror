Simple clojure app which detects file events in a source directory, and mirrors them to a target directory.

Note that this tool doesn't specifically mirror directory contents, it will only mirror file events such as creation, modification, deletion *during* the lifetime of the program.
