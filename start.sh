oldPath=$(pwd)
currentDir=$(dirname $0)
cd $currentDir
java -XX:MinHeapFreeRatio=0 -XX:MaxHeapFreeRatio=10 -cp "lib/*" com.zwc.sqldataprocessor.SqlDataProcessor $1
cd $oldPath