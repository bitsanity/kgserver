#!/bin/bash

source env.sh

CLPTH=$JARS:./java:.

# unit tests for utils
java $JLIB -cp $CLPTH a.kgserver.util.MessagePart
java $JLIB -cp $CLPTH a.kgserver.util.Message
java $JLIB -cp $CLPTH a.kgserver.KGWorker

