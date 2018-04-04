#!/bin/bash
./gradlew checkSolution -PfileNames="$(realpath $1)","$(realpath $2)"
