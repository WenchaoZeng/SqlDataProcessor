@echo off

java "-Xmx16g" "-XX:MinHeapFreeRatio=0" "-XX:MaxHeapFreeRatio=10" "-cp" "lib/*" "com.zwc.sqldataprocessor.SqlDataProcessor" "%~1"