#!/usr/bin/env bash
docker run -it --rm -v "$PWD/src/main/resources":/home/jovyan/resources -v "$PWD/notebook":/home/jovyan/notebook -p 8888:8888  jupyter/all-spark-notebook start.sh jupyter lab