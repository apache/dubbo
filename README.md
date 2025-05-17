---
title: Apache Dubbo
description: A high-performance, Java-based open-source RPC framework.
---

# Apache Dubbo

> A powerful, scalable, and extensible Web & RPC framework by the Apache Software Foundation.

![Dubbo Architecture](https://dubbo.apache.org/imgs/architecture.png)

---

## 🚀 Quick Start

### ✅ Lightweight RPC

Build an RPC service with minimal config and Java SDK:

- [5-minute RPC Guide](https://cn.dubbo.apache.org/zh-cn/overview/mannual/java-sdk/tasks/framework/lightweight-rpc/)
- Protocols supported:
  - Triple (gRPC-compatible)
  - REST
  - Dubbo2 (TCP)
  - Custom

### 🌱 Spring Boot Starter

Modern microservices with Spring Boot:

- [Spring Boot Integration Guide](https://cn.dubbo.apache.org/zh-cn/overview/mannual/java-sdk/tasks/develop/springboot/)
- YAML config + dependency = full Dubbo stack
- Out-of-the-box:
  - Service registry
  - Metrics + tracing
  - Traffic control
  - Security

---

## 🔧 Features

| Feature           | Description                                                                 |
|------------------|-----------------------------------------------------------------------------|
| Protocol Support  | Triple (gRPC), REST, TCP (Dubbo2), Custom                                  |
| Service Discovery | Zookeeper, Nacos, etc.                                                     |
| Traffic Control   | Load balancing, circuit breaking, retries                                  |
| Observability     | Metrics, Tracing, Health checks                                             |
| Security          | Authentication, TLS, Permissions                                           |
| Admin Console     | Web-based management UI ([Admin Console](https://dubbo.apache.org/zh-cn/overview/reference/admin/)) |
| K8s/Mesh Ready    | Supports Kubernetes and Service Mesh                                        |
| Extensible        | Plugin-based SPI system                                                     |

📚 [Explore all features](https://dubbo.apache.org/zh-cn/overview/core-features/)

---

## 🧱 Architecture Overview

Dubbo uses a flexible, plugin-based model to connect services:

- **Consumer ↔ Provider** communication via RPC
- **Registries** (Zookeeper, Nacos) used for service discovery
- **Config Center**, **Tracing**, **Metrics**, and **Security** out of the box

---

## 📦 Versions & Compatibility

### Dubbo 3 (Active Development)

| Version | JDK | Highlights                                                                                 |
|---------|-----|---------------------------------------------------------------------------------------------|
| 3.3.x   | 1.8–21 | Triple, REST, Spring Boot Starter, improved observability, native image support          |
| 3.2.x   | 1.8–17 | Performance +30%, metrics/tracing, thread isolation                                       |

### Dubbo 2 (Legacy)

| Version | Status  |
|---------|---------|
| 2.7.x   | EOL     |
| 2.6.x   | EOL     |

📖 [Version Compatibility Matrix](https://github.com/apache/dubbo#-which-dubbo-version-should-i-use)

---

## 🤝 Contribute

We welcome all contributions! 🎉

- [Contributing Guide](https://github.com/apache/dubbo/blob/master/CONTRIBUTING.md)
- [Open Issues](https://github.com/apache/dubbo/issues)
- [Pull Requests](https://github.com/apache/dubbo/pulls)
- [Project Board](https://github.com/orgs/apache/projects/337)

Join the conversation:

- [GitHub Discussions](https://github.com/apache/dubbo/discussions)
- [Mailing List Guide](https://github.com/apache/dubbo/wiki/Mailing-list-subscription-guide)

---

## 📬 Contact Us

| Platform       | Info                                                                 |
|----------------|----------------------------------------------------------------------|
| **WeChat**     | ID: `apachedubbo`                                                    |
| **DingTalk**   | Group ID: `37290003945`                                              |
| **Mailing List** | [dev@dubbo.apache.org](mailto:dev@dubbo.apache.org)                |
| **Twitter**    | [@ApacheDubbo](https://twitter.com/ApacheDubbo)                     |

---

## 🛡️ Security

Found a vulnerability? Please report it **privately** to:

📧 [security@dubbo.apache.org](mailto:security@dubbo.apache.org)

---

## 📄 License

Apache Dubbo is licensed under the [Apache License 2.0](https://github.com/apache/dubbo/blob/3.3/LICENSE).

---

## 🧭 Helpful Links

- 🔧 [Dubbo Admin Console](https://dubbo.apache.org/zh-cn/overview/reference/admin/)
- 📚 [Full Documentation](https://dubbo.apache.org/)
- 📊 [Project Dashboard](https://github.com/orgs/apache/projects/337)
- 🔗 [All Dubbo Repos](https://github.com/apache?q=dubbo)

---

_Thanks for being a part of the Dubbo community! ❤️
