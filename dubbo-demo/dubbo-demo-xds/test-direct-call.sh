#!/bin/bash

# 获取provider pod的IP地址
PROVIDER_IP=$(kubectl get pod -n dubbo-proxyless -l app=dubbo-demo-xds-provider,version=v1 -o jsonpath='{.items[0].status.podIP}')
echo "Provider IP: $PROVIDER_IP"

# 修改consumer的配置，直接连接provider
cat <<EOF > direct-consumer-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: direct-consumer-config
  namespace: dubbo-proxyless
data:
  application.yml: |
    spring:
      application:
        name: dubbo-demo-xds-consumer-direct

    dubbo:
      application:
        name: \${spring.application.name}
        qos-enable: false
      protocol:
        name: tri
        port: 50050
      registry:
        address: N/A
      reference:
        org.apache.dubbo.xds.demo.DemoService:
          url: tri://${PROVIDER_IP}:50051
EOF

# 应用配置
kubectl apply -f direct-consumer-config.yaml

# 创建一个临时的consumer pod来测试
cat <<EOF > direct-consumer-pod.yaml
apiVersion: v1
kind: Pod
metadata:
  name: direct-consumer-test
  namespace: dubbo-proxyless
spec:
  containers:
  - name: consumer
    image: crpi-ta376u07gp1439ff.cn-beijing.personal.cr.aliyuncs.com/test_xds/test_dubbo_consumer:latest
    env:
    - name: JAVA_TOOL_OPTIONS
      value: "-Dspring.config.location=/config/application.yml"
    volumeMounts:
    - name: config-volume
      mountPath: /config
  volumes:
  - name: config-volume
    configMap:
      name: direct-consumer-config
EOF

# 应用pod
kubectl apply -f direct-consumer-pod.yaml

# 等待pod启动
echo "Waiting for direct-consumer-test pod to start..."
kubectl wait --for=condition=Ready pod/direct-consumer-test -n dubbo-proxyless --timeout=60s

# 查看日志
echo "Pod logs:"
kubectl logs -n dubbo-proxyless direct-consumer-test -f 