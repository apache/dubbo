#!/bin/bash

# 设置消费者Pod名称
CONSUMER_POD=$(kubectl get pods -n dubbo-proxyless -l app=dubbo-demo-xds-consumer -o jsonpath='{.items[0].metadata.name}')

if [ -z "$CONSUMER_POD" ]; then
  echo "Error: Consumer pod not found"
  exit 1
fi

echo "Consumer pod: $CONSUMER_POD"

# 设置端口转发
echo "Setting up port forwarding..."
kubectl port-forward -n dubbo-proxyless $CONSUMER_POD 50050:50050 &
PORT_FORWARD_PID=$!

# 等待端口转发建立
sleep 3

# 测试10次调用，统计v1和v2的比例
echo "Testing traffic distribution (10 calls)..."
echo "Expected: ~20% to v1, ~80% to v2"
echo "Results:"
echo "--------"

# 使用curl调用consumer的REST接口10次
for i in {1..10}; do
  RESULT=$(curl -s http://localhost:50050/hello)
  echo "$i: $RESULT"
done

# 终止端口转发
kill $PORT_FORWARD_PID

echo "--------"
echo "Test complete." 