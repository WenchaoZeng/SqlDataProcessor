mvn clean
mvn package

# 拷贝文件到目标目录
rm -rdf ./target/release
mkdir ./target/release
cp ./target/sqldataprocessor-*.jar ./target/release/SqlDataProcessor.jar

# mac app打包: http://centerkey.com/mac/java/
jdk=$(/usr/libexec/java_home)
$jdk/bin/javapackager \
   -deploy \
   -native image \
   -name SqlDataProcessor \
   -BappVersion=0.0.0 \
   -Bicon=app.icns \
   -srcdir ./target/release \
   -srcfiles SqlDataProcessor.jar \
   -appclass com.zwc.sqldataprocessor.MacApp \
   -outdir target/release/result \
   -outfile SqlDataProcessor \
   -nosign \
   -v

mkdir ./target/release/result/bundles/SqlDataProcessor.app/Contents/Java/lib
cp ./target/sqldataprocessor-0.0.0-SNAPSHOT/lib/* ./target/release/result/bundles/SqlDataProcessor.app/Contents/Java/lib/

bash zip.sh

# 打夸平台的命令行启动的zip包
mkdir mkdir ./target/release/java
mkdir mkdir ./target/release/java/lib
cp ./target/sqldataprocessor-0.0.0-SNAPSHOT/lib/* ./target/release/java/lib/
cp ./start.sh ./target/release/java/
cp ./start.sh ./target/release/java/start.bat
bash zip2.sh

exit 0