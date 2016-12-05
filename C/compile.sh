#!/bin/bash

javah -classpath ../java a.kgserver.util.Secp256k1

gcc -D __int64="long long" -c -fPIC -I"$HOME/secp256k1/include" -I"$JAVAHOME/include" -I"$JAVAHOME/include/linux" -shared a_kgserver_util_Secp256k1.c

gcc -shared -o ../lib/libkgserver.so a_kgserver_util_Secp256k1.o $HOME/secp256k1/.libs/libsecp256k1.so

