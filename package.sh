mvn clean
mvn package

# 拷贝文件到目标目录
rm -rdf ./target/release
mkdir ./target/release
mkdir ./target/release/lib
cp ./target/sqldataprocessor-0.0.0-SNAPSHOT/lib/* ./target/release/lib/
cp ./start.sh ./target/release
cp ./start.sh ./target/release/start.bat

# 打包
zip -r9 ./target/release/java_SqlDataProcessor.zip ./target/release/*

exit 0