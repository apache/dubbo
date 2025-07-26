# Dubbo Proxyless模式加权路由完整指南

本文档详细介绍如何使用Kind部署Kubernetes集群，安装Istio服务网格，并配置Dubbo应用实现Proxyless模式的加权路由功能。

## 目录

1. [环境准备](#环境准备)
2. [Kind集群部署](#kind集群部署)
3. [Istio安装配置](#istio安装配置)
4. [项目构建](#项目构建)
5. [Kubernetes资源配置](#kubernetes资源配置)
6. [应用部署](#应用部署)
7. [加权路由配置](#加权路由配置)
8. [效果验证](#效果验证)
9. [故障排查](#故障排查)

## 环境准备

### 系统要求
- macOS/Linux操作系统
- Docker Desktop
- kubectl命令行工具
- Maven 3.6+
- JDK 8+

### 安装必需工具

#### 1. 安装Kind
```bash
# macOS
brew install kind

# Linux
curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64
chmod +x ./kind
sudo mv ./kind /usr/local/bin/kind
```

#### 2. 安装kubectl
```bash
# macOS
brew install kubectl

# Linux
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv kubectl /usr/local/bin/
```

#### 3. 安装Istioctl
```bash
curl -L https://istio.io/downloadIstio | sh -
cd istio-*
sudo cp bin/istioctl /usr/local/bin/
```

## Kind集群部署

### 1. 创建Kind配置文件

创建 `kind-config.yaml` 文件：

```yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
- role: control-plane
  kubeadmConfigPatches:
  - |
    kind: InitConfiguration
    nodeRegistration:
      kubeletExtraArgs:
        node-labels: "ingress-ready=true"
  extraPortMappings:
  - containerPort: 80
    hostPort: 80
    protocol: TCP
  - containerPort: 443
    hostPort: 443
    protocol: TCP
- role: worker
- role: worker
```

### 2. 创建Kind集群

```bash
# 创建集群
kind create cluster --config kind-config.yaml --name dubbo-demo

# 验证集群状态
kubectl cluster-info --context kind-dubbo-demo
kubectl get nodes
```

**预期输出：**
```
NAME                      STATUS   ROLES           AGE   VERSION
dubbo-demo-control-plane  Ready    control-plane   2m    v1.27.3
dubbo-demo-worker         Ready    <none>          2m    v1.27.3
dubbo-demo-worker2        Ready    <none>          2m    v1.27.3
```

## Istio安装配置

### 1. 安装Istio

```bash
# 使用demo配置安装Istio（包含Istiod、Ingress Gateway、Egress Gateway）
istioctl install --set values.defaultRevision=default --set values.pilot.env.EXTERNAL_ISTIOD=false -y

# 验证Istio安装
kubectl get pods -n istio-system
```

**预期输出：**
```
NAME                                    READY   STATUS    RESTARTS   AGE
istio-egressgateway-xxx                 1/1     Running   0          2m
istio-ingressgateway-xxx                1/1     Running   0          2m
istiod-xxx                              1/1     Running   0          2m
```

### 2. 创建命名空间并启用Sidecar注入

```bash
# 创建dubbo应用的命名空间
kubectl create namespace dubbo-proxyless

# 为命名空间启用Istio sidecar自动注入
kubectl label namespace dubbo-proxyless istio-injection=enabled

# 验证标签
kubectl get namespace dubbo-proxyless --show-labels
```

## 项目构建

### 1. 构建Dubbo XDS模块

```bash
# 切换到dubbo项目根目录
cd /path/to/dubbo

# 构建dubbo-xds模块
mvn clean install -pl dubbo-xds -am -DskipTests -Dspotless.check.skip=true
```

### 2. 构建Demo应用

```bash
# 切换到demo目录
cd dubbo-demo/dubbo-demo-xds

# 构建provider
mvn clean install -pl dubbo-demo-xds-provider -am -DskipTests -Dspotless.check.skip=true

# 构建consumer
mvn clean install -pl dubbo-demo-xds-consumer -am -DskipTests -Dspotless.check.skip=true
```

## Kubernetes资源配置

### 1. Provider配置 (services.yaml)

创建 `services.yaml` 文件，包含v1版本的provider配置：

```yaml
apiVersion: v1
kind: Service
metadata:
  name: dubbo-demo-xds-provider
  namespace: dubbo-proxyless
  labels:
    app: dubbo-demo-xds-provider
    service: dubbo-demo-xds-provider
spec:
  ports:
  - port: 50051
    name: grpc
  - port: 31001
    name: debug
  selector:
    app: dubbo-demo-xds-provider
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: dubbo-demo-xds-provider
  namespace: dubbo-proxyless
  labels:
    app: dubbo-demo-xds-provider
    version: v1
spec:
  replicas: 1
  selector:
    matchLabels:
      app: dubbo-demo-xds-provider
      version: v1
  template:
    metadata:
      labels:
        app: dubbo-demo-xds-provider
        version: v1
    spec:
      containers:
      - name: dubbo-demo-xds-provider
        image: your-registry/dubbo-demo-xds-provider:latest
        imagePullPolicy: Always
        ports:
        - containerPort: 50051
        - containerPort: 31001
        env:
        - name: POD_NAME
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: POD_NAMESPACE
          valueFrom:
            fieldRef:
              fieldPath: metadata.namespace
        - name: JAVA_TOOL_OPTIONS
          value: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=31001 -Ddubbo.application.logger=slf4j -Dlogging.level.root=INFO -Dlogging.level.org.apache.dubbo=DEBUG -Dlogging.level.org.apache.dubbo.xds=TRACE -Dservice.version=v1"
```

### 2. Provider v2版本配置 (provider-v2.yaml)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: dubbo-demo-xds-provider-v2
  namespace: dubbo-proxyless
  labels:
    app: dubbo-demo-xds-provider
    version: v2
spec:
  replicas: 1
  selector:
    matchLabels:
      app: dubbo-demo-xds-provider
      version: v2
  template:
    metadata:
      labels:
        app: dubbo-demo-xds-provider
        version: v2
    spec:
      containers:
      - name: dubbo-demo-xds-provider
        image: your-registry/dubbo-demo-xds-provider:latest
        imagePullPolicy: Always
        ports:
        - containerPort: 50051
        - containerPort: 31001
        env:
        - name: POD_NAME
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: POD_NAMESPACE
          valueFrom:
            fieldRef:
              fieldPath: metadata.namespace
        - name: JAVA_TOOL_OPTIONS
          value: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=31001 -Ddubbo.application.logger=slf4j -Dlogging.level.root=INFO -Dlogging.level.org.apache.dubbo=DEBUG -Dlogging.level.org.apache.dubbo.xds=TRACE -Dservice.version=v2"
```

### 3. Consumer配置 (consumer-services.yaml)

```yaml
apiVersion: v1
kind: Service
metadata:
  name: dubbo-demo-xds-consumer
  namespace: dubbo-proxyless
  labels:
    app: dubbo-demo-xds-consumer
    service: dubbo-demo-xds-consumer
spec:
  ports:
  - port: 50050
    name: grpc
  - port: 31000
    name: debug
  selector:
    app: dubbo-demo-xds-consumer
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: dubbo-demo-xds-consumer
  namespace: dubbo-proxyless
  labels:
    app: dubbo-demo-xds-consumer
    version: v1
spec:
  replicas: 1
  selector:
    matchLabels:
      app: dubbo-demo-xds-consumer
      version: v1
  template:
    metadata:
      labels:
        app: dubbo-demo-xds-consumer
        version: v1
    spec:
      containers:
      - name: dubbo-demo-xds-consumer
        image: your-registry/dubbo-demo-xds-consumer:latest
        imagePullPolicy: Always
        ports:
        - containerPort: 50050
        - containerPort: 31000
        env:
        - name: POD_NAME
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: POD_NAMESPACE
          valueFrom:
            fieldRef:
              fieldPath: metadata.namespace
        - name: JAVA_TOOL_OPTIONS
          value: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=31000 -Ddubbo.application.logger=slf4j -Dlogging.level.root=INFO -Dlogging.level.org.apache.dubbo=DEBUG -Dlogging.level.org.apache.dubbo.xds=TRACE"
```

## 应用部署

### 1. 构建Docker镜像

为了在Kind集群中使用本地构建的镜像，需要将镜像加载到Kind集群中：

```bash
# 构建provider镜像
cd dubbo-demo-xds-provider
docker build -t dubbo-demo-xds-provider:latest .

# 构建consumer镜像
cd ../dubbo-demo-xds-consumer
docker build -t dubbo-demo-xds-consumer:latest .

# 将镜像加载到Kind集群
kind load docker-image dubbo-demo-xds-provider:latest --name dubbo-demo
kind load docker-image dubbo-demo-xds-consumer:latest --name dubbo-demo
```

### 2. 部署应用

```bash
# 部署provider (v1版本)
kubectl apply -f services.yaml

# 部署provider v2版本
kubectl apply -f provider-v2.yaml

# 部署consumer
kubectl apply -f consumer-services.yaml

# 验证部署状态
kubectl get pods -n dubbo-proxyless
```

**预期输出：**
```
NAME                                         READY   STATUS    RESTARTS   AGE
dubbo-demo-xds-consumer-xxx                  2/2     Running   0          1m
dubbo-demo-xds-provider-xxx                  2/2     Running   0          2m
dubbo-demo-xds-provider-v2-xxx               2/2     Running   0          1m
```

注意：每个Pod显示 `2/2` Ready，表示应用容器和Istio sidecar都正常运行。

## 加权路由配置

### 1. 创建VirtualService配置

创建 `traffic-rules.yaml` 文件：

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: dubbo-demo-xds-provider
  namespace: dubbo-proxyless
spec:
  hosts:
  - dubbo-demo-xds-provider.dubbo-proxyless.svc.cluster.local
  http:
  - match:
    - uri:
        prefix: "/"
    route:
    - destination:
        host: dubbo-demo-xds-provider.dubbo-proxyless.svc.cluster.local
        subset: v1
      weight: 20
    - destination:
        host: dubbo-demo-xds-provider.dubbo-proxyless.svc.cluster.local
        subset: v2
      weight: 80
---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: dubbo-demo-xds-provider
  namespace: dubbo-proxyless
spec:
  host: dubbo-demo-xds-provider.dubbo-proxyless.svc.cluster.local
  subsets:
  - name: v1
    labels:
      version: v1
  - name: v2
    labels:
      version: v2
```

### 2. 应用流量规则

```bash
# 应用加权路由配置
kubectl apply -f traffic-rules.yaml

# 验证配置
kubectl get virtualservice -n dubbo-proxyless
kubectl get destinationrule -n dubbo-proxyless
```

**预期输出：**
```
NAME                      GATEWAYS   HOSTS                                                          AGE
dubbo-demo-xds-provider              [dubbo-demo-xds-provider.dubbo-proxyless.svc.cluster.local]   1m

NAME                      HOST                                                          AGE
dubbo-demo-xds-provider   dubbo-demo-xds-provider.dubbo-proxyless.svc.cluster.local    1m
```

## 效果验证

### 1. 检查应用启动状态

```bash
# 检查consumer日志确认启动成功
kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-consumer --tail=20

# 检查provider日志
kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-provider --tail=20
kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-provider-v2 --tail=20
```

### 2. 验证服务发现

确认consumer能够发现provider的两个版本：

```bash
# 查看consumer日志中的EDS更新信息
kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-consumer | grep -i "eds.*endpoint"
```

**预期输出应包含两个endpoint：**
```
[XDS] Received EdsUpdate for cluster outbound|50051|v1|..., endpoints=[10.244.1.x:50051]
[XDS] Received EdsUpdate for cluster outbound|50051|v2|..., endpoints=[10.244.1.y:50051]
```

### 3. 验证加权路由

#### 方法1：查看实时日志

```bash
# 实时查看加权集群选择情况
kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-consumer -f | grep "Selected weighted cluster"
```

**预期输出：**
```
Selected weighted cluster: outbound|50051|v2|...  # 更频繁出现 (80%)
Selected weighted cluster: outbound|50051|v1|...  # 较少出现 (20%)
```

#### 方法2：统计分析

使用提供的监控脚本：

```bash
# 使脚本可执行
chmod +x monitor_weighted_routing.sh

# 运行监控脚本
./monitor_weighted_routing.sh
```

**预期输出：**
```
监控加权路由分布...
预期分布: v1 ~20%, v2 ~80%
================================
14:30:15 - v1: 5次(21%) | v2: 19次(79%) | 总计: 24次
14:30:20 - v1: 6次(20%) | v2: 24次(80%) | 总计: 30次
14:30:25 - v1: 7次(19%) | v2: 29次(81%) | 总计: 36次
```

#### 方法3：一次性统计

```bash
# 获取consumer pod名称
POD_NAME=$(kubectl get pods -n dubbo-proxyless -l app=dubbo-demo-xds-consumer -o jsonpath='{.items[0].metadata.name}')

# 统计加权路由分布
kubectl logs -n dubbo-proxyless $POD_NAME | grep "Selected weighted cluster" | grep -o "v[12]" | sort | uniq -c
```

**预期输出：**
```
   12 v1    # 约20%
   48 v2    # 约80%
```

### 4. 验证版本信息

由于我们在provider中添加了版本信息，可以验证响应是否包含正确的版本：

```bash
# 查看consumer日志中的成功调用结果
kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-consumer | grep "Call successful"
```

**预期输出：**
```
Call successful, result: hello world from v1 pod: dubbo-demo-xds-provider-xxx
Call successful, result: hello world from v2 pod: dubbo-demo-xds-provider-v2-xxx
```

## 故障排查

### 1. 常见问题检查清单

#### Pod状态检查
```bash
# 检查所有pod状态
kubectl get pods -n dubbo-proxyless -o wide

# 检查pod详细信息
kubectl describe pod -n dubbo-proxyless <pod-name>
```

#### 网络连接检查
```bash
# 检查service endpoints
kubectl get endpoints -n dubbo-proxyless

# 检查service详情
kubectl describe service -n dubbo-proxyless dubbo-demo-xds-provider
```

#### Istio配置检查
```bash
# 检查Istio代理配置
istioctl proxy-config cluster <consumer-pod-name> -n dubbo-proxyless

# 检查路由配置
istioctl proxy-config route <consumer-pod-name> -n dubbo-proxyless

# 检查listeners配置
istioctl proxy-config listener <consumer-pod-name> -n dubbo-proxyless
```

### 2. 常见问题及解决方案

#### 问题1：Pod无法启动
**症状：**Pod状态为Pending或CrashLoopBackOff

**解决方案：**
```bash
# 查看pod事件
kubectl describe pod -n dubbo-proxyless <pod-name>

# 查看pod日志
kubectl logs -n dubbo-proxyless <pod-name>

# 检查资源限制
kubectl top pods -n dubbo-proxyless
```

#### 问题2：服务发现失败
**症状：**Consumer无法发现Provider

**解决方案：**
```bash
# 检查DNS解析
kubectl exec -n dubbo-proxyless <consumer-pod> -- nslookup dubbo-demo-xds-provider.dubbo-proxyless.svc.cluster.local

# 检查service配置
kubectl get svc -n dubbo-proxyless dubbo-demo-xds-provider -o yaml
```

#### 问题3：加权路由不生效
**症状：**所有请求都路由到同一个版本

**解决方案：**
```bash
# 检查VirtualService配置
kubectl get virtualservice -n dubbo-proxyless dubbo-demo-xds-provider -o yaml

# 检查DestinationRule配置
kubectl get destinationrule -n dubbo-proxyless dubbo-demo-xds-provider -o yaml

# 检查pod标签
kubectl get pods -n dubbo-proxyless --show-labels
```

#### 问题4：应用调试
**症状：**需要调试应用代码

**解决方案：**
```bash
# 端口转发用于调试
kubectl port-forward -n dubbo-proxyless <consumer-pod> 31000:31000

# 然后在IDE中连接到localhost:31000进行远程调试
```

### 3. 日志分析

#### 重要日志关键词
- `[XDS]` - XDS相关日志
- `Selected weighted cluster` - 加权集群选择
- `EdsUpdate` - Endpoint更新
- `VirtualHost` - 虚拟主机匹配
- `CdsUpdate` - 集群更新

#### 获取详细日志
```bash
# 获取所有XDS相关日志
kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-consumer | grep "\[XDS\]"

# 获取路由相关日志
kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-consumer | grep -E "(Selected|Route|Cluster)"
```

## 总结

本指南演示了如何使用Kind部署Kubernetes集群，安装Istio，并配置Dubbo应用实现Proxyless模式的加权路由。关键要点包括：

1. **环境配置**：Kind + Istio + Dubbo应用的完整部署流程
2. **加权路由**：通过Istio VirtualService和DestinationRule配置流量分发策略
3. **Proxyless模式**：Dubbo应用直接与Istio控制平面交互，无需sidecar代理数据平面
4. **效果验证**：通过多种方式验证加权路由是否按预期工作

通过本指南，你可以深入理解Dubbo在服务网格环境中的Proxyless模式工作原理，以及如何实现精细化的流量管理策略。 