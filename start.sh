#! /bin/bash

fullSqlPath="$(cd "$(dirname "$1")" && pwd -P)/$(basename "$1")"

oldPath=$(pwd)
currentDir=$(dirname $0)
cd $currentDir

java -Xmx16g -XX:MinHeapFreeRatio=0 -XX:MaxHeapFreeRatio=10 -cp "lib/*" com.zwc.sqldataprocessor.SqlDataProcessor "$fullSqlPath"

cd $oldPath