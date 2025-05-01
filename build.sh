#!/bin/bash

# Если требуется ручная сборка:
# exit 1

docker build -t rmp_ps_build --file DockerfileBuild .
docker run --rm -v .:/home/gradle/project -v gradle_cache_rmp_ps:/home/gradle/.gradle rmp_ps_build
