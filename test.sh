#!/bin/bash

source env.sh

CLPTH=$JARS:./java:.

# note: if the AES test throws an exception remember to download Oracle's
#       JCE Unlimited Strength Jurisdiction Policy Files 8 Download and
#       replace default files in $JAVA_HOME/jre/lib/security/

java $JLIB -cp $CLPTH a.kgserver.util.AES256
java $JLIB -cp $CLPTH a.kgserver.util.ECIES

java $JLIB -cp $CLPTH a.kgserver.util.MessagePart

java $JLIB -cp $CLPTH a.kgserver.util.Message

java $JLIB -cp $CLPTH a.kgserver.KGWorker

