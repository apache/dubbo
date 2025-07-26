#!/bin/bash

echo "=== Setting up Debug Port Forward ==="

# 获取consumer pod名称
POD_NAME=$(kubectl get pod -l app=dubbo-demo-xds-consumer -n dubbo-proxyless -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)

if [ -z "$POD_NAME" ]; then
    echo "Error: Consumer pod not found. Please make sure the consumer is deployed."
    exit 1
fi

echo "Consumer pod name: $POD_NAME"
echo "Setting up port forward for debug port 31000..."
echo "Press Ctrl+C to stop port forwarding"
echo ""

# 设置端口转发
kubectl port-forward $POD_NAME 31000:31000 -n dubbo-proxyless 