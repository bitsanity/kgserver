#!/bin/bash

source env.sh

java $JLIB -cp $JARS:./java:. a.kgserver.KGServer 8080 test=0

