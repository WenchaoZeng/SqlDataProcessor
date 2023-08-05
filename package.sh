mvn clean
mvn package

# 拷贝文件到目标目录
rm -rdf ./target/release
mkdir ./target/release
mkdir ./target/release/lib
cp ./target/sqldataprocessor-0.0.0-SNAPSHOT/lib/* ./target/release/lib/
cp ./start.sh ./target/release

# 打包
oldPath=$(pwd)
cd ./target/release
zip -r9 ./java_SqlDataProcessor.zip ./*
cd $oldPath