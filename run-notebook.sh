#!/usr/bin/env bash
wget http://central.maven.org/maven2/org/apache/spark/spark-sql-kafka-0-10_2.11/2.2.1/spark-sql-kafka-0-10_2.11-2.2.1.jar -O libs/spark-kafka-connector.jar &&
docker run -it --rm -v "$PWD/src/main/resources":/home/jovyan/resources -v "$PWD/libs":/home/jovyan/ -v "$PWD/notebook":/home/jovyan/notebook -p 8888:8888  jupyter/all-spark-notebook