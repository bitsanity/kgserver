#!/bin/bash

source env.sh

javac -g -classpath $JARS:./java:. java/a/kgserver/util/*.java
javac -g -classpath $JARS:./java:. java/a/kgserver/*.java

cd ./C
./compile.sh
cd ..

