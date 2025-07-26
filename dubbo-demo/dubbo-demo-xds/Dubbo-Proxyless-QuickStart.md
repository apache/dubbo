# Dubbo Proxyless模式快速开始指南

本项目演示了Apache Dubbo在Istio服务网格中的Proxyless模式实现，包括加权路由功能。

## 项目概述

### 什么是Dubbo Proxyless模式？

Dubbo Proxyless模式是一种新的服务网格集成方式，它允许Dubbo应用直接与服务网格控制平面（如Istio）交互，而无需在数据平面使用sidecar代理。这种模式具有以下优势：

- **更低的延迟**：无需经过sidecar代理，直接通信
- **更少的资源消耗**：不需要额外的sidecar容器
- **更简单的部署**：减少了网络复杂性
- **原生支持**：充分利用Dubbo的负载均衡和路由能力

### 项目结构

```
dubbo-demo-xds/
├── dubbo-demo-xds-interface/          # 服务接口定义
├── dubbo-demo-xds-provider/           # 服务提供者
│   ├── src/main/java/.../DemoServiceImpl.java
│   ├── Dockerfile
│   └── pom.xml
├── dubbo-demo-xds-consumer/           # 服务消费者
│   ├── src/main/java/.../XdsConsumerApplication.java
│   ├── Dockerfile
│   └── pom.xml
├── services.yaml                     # Provider v1部署配置
├── provider-v2.yaml                  # Provider v2部署配置
├── consumer-services.yaml            # Consumer部署配置
├── traffic-rules.yaml               # Istio流量规则配置
├── kind-config.yaml                 # Kind集群配置
├── monitor_weighted_routing.sh      # 加权路由监控脚本
└── Dubbo-Proxyless-WeightedRouting-Guide.md  # 详细部署指南
```

## 快速开始

### 1. 环境准备

确保已安装以下工具：
- Docker Desktop
- kubectl
- Kind
- Istioctl
- Maven 3.6+
- JDK 8+

### 2. 一键部署脚本

```bash
# 创建Kind集群
kind create cluster --config kind-config.yaml --name dubbo-demo

# 安装Istio
istioctl install --set values.defaultRevision=default -y

# 创建命名空间
kubectl create namespace dubbo-proxyless
kubectl label namespace dubbo-proxyless istio-injection=enabled

# 构建并部署应用
./start_optimized.sh  # 构建和部署provider
./rebuild_deploy_consumer.sh  # 构建和部署consumer

# 配置流量路由
kubectl apply -f traffic-rules.yaml
```

### 3. 验证效果

```bash
# 监控加权路由分布
./monitor_weighted_routing.sh

# 预期输出：v1 ~20%, v2 ~80%
```

## 核心功能演示

### 1. 服务发现
- Consumer自动发现Provider的两个版本实例
- 通过XDS协议从Istio控制平面获取服务信息

### 2. 加权路由
- 80%的流量路由到v2版本
- 20%的流量路由到v1版本
- 动态调整权重无需重启应用

### 3. 负载均衡
- 支持多种负载均衡策略
- 与Istio的流量管理策略无缝集成

## 技术特点

### XDS协议集成
- **LDS (Listener Discovery Service)**：监听器配置
- **RDS (Route Discovery Service)**：路由配置
- **CDS (Cluster Discovery Service)**：集群配置
- **EDS (Endpoint Discovery Service)**：端点配置

### 关键组件
- **XdsResourceFactory**：XDS资源管理和订阅
- **XdsRouter**：路由决策和集群选择
- **XdsRegistry**：服务注册和发现

### 配置要点
- Bootstrap配置：连接Istio控制平面
- 环境变量：Pod名称、命名空间等
- 日志配置：详细的XDS调试信息

## 监控和调试

### 日志关键词
- `[XDS]`：XDS协议相关日志
- `Selected weighted cluster`：加权集群选择
- `EdsUpdate`：端点更新
- `VirtualHost`：虚拟主机匹配

### 监控命令
```bash
# 实时查看路由选择
kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-consumer -f | grep "Selected weighted cluster"

# 统计路由分布
kubectl logs -n dubbo-proxyless <consumer-pod> | grep "Selected weighted cluster" | grep -o "v[12]" | sort | uniq -c
```

### Istio调试
```bash
# 查看代理配置
istioctl proxy-config cluster <consumer-pod> -n dubbo-proxyless
istioctl proxy-config route <consumer-pod> -n dubbo-proxyless
```

## 进阶配置

### 自定义流量策略
- 基于请求头的路由
- 故障注入和超时设置
- 重试策略配置

### 多环境部署
- 开发、测试、生产环境隔离
- 蓝绿部署和金丝雀发布
- 多集群服务网格

### 性能优化
- 连接池配置
- 断路器设置
- 监控指标收集

## 常见问题

1. **服务发现失败**：检查DNS配置和Service endpoints
2. **路由不生效**：验证VirtualService和DestinationRule配置
3. **连接问题**：确认网络策略和安全设置

## 相关文档

- [详细部署指南](./Dubbo-Proxyless-WeightedRouting-Guide.md)
- [Apache Dubbo官方文档](https://dubbo.apache.org/)
- [Istio官方文档](https://istio.io/)

## 联系支持

如有问题，请参考详细部署指南或提交issue到Apache Dubbo项目。 