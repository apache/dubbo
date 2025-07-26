#!/bin/bash
set -e

echo "=== 开始重新构建和部署consumer ==="

# 1. 清理旧的构建文件
echo "清理旧的构建文件..."
cd ../..
mvn clean -pl dubbo-demo/dubbo-demo-xds/dubbo-demo-xds-consumer -Dspotless.check.skip=true

# 2. 重新构建consumer
echo "重新构建consumer..."
mvn install -pl dubbo-demo/dubbo-demo-xds/dubbo-demo-xds-consumer -am -DskipTests -Dspotless.check.skip=true

# 3. 返回到dubbo-demo-xds目录
cd dubbo-demo/dubbo-demo-xds

# 4. 构建并推送Docker镜像
echo "构建并推送Docker镜像..."
CONSUMER_DIR="$(pwd)/dubbo-demo-xds-consumer"
CONSUMER_IMAGE="dubbo-demo-xds-consumer:latest"
REMOTE_REPO="crpi-ta376u07gp1439ff.cn-beijing.personal.cr.aliyuncs.com/test_xds/test_dubbo_consumer"
REMOTE_TAG="latest"

cd $CONSUMER_DIR
JAR_FILE=$(find target -name "*.jar" -type f | grep -v "sources\|javadoc\|tests" | head -1)
if [ -z "$JAR_FILE" ]; then
  echo "错误: 在 $CONSUMER_DIR/target 中找不到JAR文件"
  exit 1
fi

echo "找到JAR文件: $JAR_FILE"
# 将JAR文件复制到当前目录，以便Docker能够找到它
cp "$JAR_FILE" .
docker build -t $CONSUMER_IMAGE --build-arg ARTIFACT=$(basename $JAR_FILE) .
# 清理复制的JAR文件
rm $(basename $JAR_FILE)
docker tag $CONSUMER_IMAGE $REMOTE_REPO:$REMOTE_TAG
docker push $REMOTE_REPO:$REMOTE_TAG

# 5. 重新部署到Kubernetes
cd ..
echo "重新部署到Kubernetes..."
kubectl delete -f consumer-services.yaml || true
sleep 5
kubectl apply -f consumer-services.yaml

# 6. 等待pod启动
echo "等待consumer pod启动..."
sleep 10
kubectl get pods -n dubbo-proxyless | grep consumer

echo "=== 重新构建和部署完成 ==="
echo "你可以使用以下命令查看日志:"
echo "./consumer_logs.sh"
echo "或者使用以下命令进行远程调试:"
echo "./debug_consumer.sh" 