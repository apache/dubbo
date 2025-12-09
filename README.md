
# Apache Dubbo Project

[![Build and Test For PR](https://github.com/apache/dubbo/actions/workflows/build-and-test-pr.yml/badge.svg)](https://github.com/apache/dubbo/actions/workflows/build-and-test-pr.yml)
[![Codecov](https://codecov.io/gh/apache/dubbo/branch/3.3/graph/badge.svg)](https://codecov.io/gh/apache/dubbo)
[![Maven](https://img.shields.io/github/v/release/apache/dubbo.svg?sort=semver)](https://github.com/apache/dubbo/releases)
[![License](https://img.shields.io/github/license/apache/dubbo.svg)](https://github.com/apache/dubbo/blob/3.3/LICENSE)
[![Average time to resolve an issue](http://isitmaintained.com/badge/resolution/apache/dubbo.svg)](http://isitmaintained.com/project/apache/dubbo)
[![Percentage of issues still open](http://isitmaintained.com/badge/open/apache/dubbo.svg)](http://isitmaintained.com/project/apache/dubbo)

Apache Dubbo is a powerful and user-friendly Web and RPC framework. It supports multiple language implementations such as Java, [Go](https://github.com/apache/dubbo-go), [Python](https://github.com/dubbo/py-client-for-apache-dubbo), [PHP](https://github.com/apache/dubbo-php-framework), [Erlang](https://github.com/apache/dubbo-erlang), [Rust](https://github.com/apache/dubbo-rust), and [Node.js/Web](https://github.com/apache/dubbo-js).

Dubbo provides solutions for communication, service discovery, traffic management, observability, security, tooling, and best practices for building enterprise-grade microservices.

> 🚀 We're collecting user info to improve Dubbo. Help us out here: [Who's using Dubbo](https://github.com/apache/dubbo/discussions/13842)

---

## 🧱 Architecture

![Architecture](https://dubbo.apache.org/imgs/architecture.png)

- Communication between consumers and providers is done via RPC protocols like Triple, TCP, REST, etc.
- Consumers dynamically discover provider instances from registries (e.g., Zookeeper, Nacos) and manage traffic using defined strategies.
- Built-in support for dynamic config, metrics, tracing, security, and a visualized console.

---

## 🚀 Getting Started

### 📦 Lightweight RPC API

Start quickly with our [5-minute guide](https://cn.dubbo.apache.org/zh-cn/overview/mannual/java-sdk/tasks/framework/lightweight-rpc/)

Dubbo allows you to build RPC services using a minimal codebase and a lightweight SDK. It supports protocols like:

- [Triple (gRPC-compatible)](https://dubbo.apache.org/zh-cn/overview/reference/protocols/triple/)
- Dubbo2 (TCP)
- REST
- Custom protocols

### 🌱 Microservices with Spring Boot

Kickstart your project using [Spring Boot Starter](https://cn.dubbo.apache.org/zh-cn/overview/mannual/java-sdk/tasks/develop/springboot/).

Using just a dependency and a YAML config, you can unlock the full power of Dubbo: service discovery, observability, tracing, etc.

➡️ Learn how to [deploy](https://dubbo.apache.org/zh-cn/overview/tasks/deploy/), [monitor](https://dubbo.apache.org/zh-cn/overview/tasks/observability/), and [manage traffic](https://dubbo.apache.org/zh-cn/overview/tasks/traffic-management/) for Dubbo services.

---

## 🛠️ More Features

Explore more through our hands-on tasks:

- [Launch a Dubbo project](https://dubbo.apache.org/zh-cn/overview/tasks/develop/template/)
- [RPC protocols](https://dubbo.apache.org/zh-cn/overview/core-features/protocols/)
- [Traffic management](https://dubbo.apache.org/zh-cn/overview/core-features/traffic/)
- [Service discovery](https://dubbo.apache.org/zh-cn/overview/core-features/service-discovery/)
- [Observability](https://dubbo.apache.org/zh-cn/overview/core-features/observability/)
- [Extensibility](https://dubbo.apache.org/zh-cn/overview/core-features/extensibility/)
- [Security](https://dubbo.apache.org/zh-cn/overview/core-features/security/)
- [Visualized Console](https://dubbo.apache.org/zh-cn/overview/reference/admin/)
- [Kubernetes & Service Mesh](https://dubbo.apache.org/zh-cn/overview/core-features/service-mesh/)

---

## 📦 Which Dubbo Version Should I Use?

### Dubbo3

## 📦 Version Compatibility

| Version            | JDK Support | Dependencies                                                                                              | Highlights                                                                                                     |
|--------------------|-------------|-----------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| **3.3.7-SNAPSHOT** | 1.8 – 25    | Coming Soon                                                                                               | ✅ JDK 25 Support
| **3.3.6**          | 1.8 – 21    | [View Dependencies](https://github.com/apache/dubbo/blob/dubbo-3.3.6/dubbo-dependencies-bom/pom.xml#L92)  | ✅ Mutiny Reactive Support <br> ✅ Affinity Router <br> ✅ Method-level TPS Limiting <br> ✅ Spring 6 Security Plugin <br> ✅ Enhanced Environment Variable Config |
| **3.3.5**          | 1.8 – 21    | [View Dependencies](https://github.com/apache/dubbo/blob/dubbo-3.3.5/dubbo-dependencies-bom/pom.xml#L92)  | ✅ Actively Maintained <br> ✅ Triple Protocol (gRPC/cURL) <br> ✅ REST Support <br> ✅ Spring Boot Starters      |
| **3.2.16**         | 1.8 – 17    | [View Dependencies](https://github.com/apache/dubbo/blob/dubbo-3.2.5/dubbo-dependencies-bom/pom.xml#L94)  | ✅ Actively Maintained <br> ✅ Metrics & Tracing <br> ✅ Thread Pool Isolation <br> ✅ +30% Performance <br> ✅ Native Image Support |
| **3.1.11**         | 1.8 – 17    | [View Dependencies](https://github.com/apache/dubbo/blob/dubbo-3.2.11/dubbo-dependencies-bom/pom.xml#L90) | ⚠️ Stable, but Not Actively Maintained                                                                         |

### Dubbo2

| Version     | JDK       | Dependencies                                                                                          | Description |
|-------------|-----------|--------------------------------------------------------------------------------------------------------|-------------|
| 2.7.23      | 1.8       | [dependency list](https://github.com/apache/dubbo/blob/dubbo-2.7.23/dubbo-dependencies-bom/pom.xml#L92) | ❌ EOL       |
| 2.6.x, 2.5.x| 1.6 - 1.7 | [dependency list](https://github.com/apache/dubbo/blob/dubbo-2.6.12/dependencies-bom/pom.xml#L90)       | ❌ EOL       |

---

## 🤝 Contributing

See our [CONTRIBUTING](https://github.com/apache/dubbo/blob/master/CONTRIBUTING.md) guide to get started!

### 🔁 Community Collaboration

- **Issues**: For bugs or tasks – [GitHub Issues](https://github.com/apache/dubbo/issues)
- **Discussions**: For questions, ideas – [GitHub Discussions](https://github.com/apache/dubbo/discussions)
- **PRs**: For merging your contributions – [GitHub Pull Requests](https://github.com/apache/dubbo/pulls)
- **Project Board**: [Dubbo Project Board](https://github.com/orgs/apache/projects/337)

### 💡 How You Can Help

- Check out "help wanted" issues: [Project Board](https://github.com/orgs/apache/projects/337)
- Join [mailing list discussions](https://github.com/apache/dubbo/wiki/Mailing-list-subscription-guide)
- Engage in [discussions](https://github.com/apache/dubbo/discussions)
- Fix [bugs](https://github.com/apache/dubbo/issues) or review [pull requests](https://github.com/apache/dubbo/pulls)
- Enhance the [website](https://github.com/apache/dubbo-website)
- Improve [dubbo-admin](https://github.com/apache/dubbo-admin)
- Contribute to the [ecosystem](https://github.com/apache/?q=dubbo&type=all&language=&sort=)

If you're interested in contributing, email us at [dev@dubbo.apache.org](mailto:dev@dubbo.apache.org).

---

## 🐞 Reporting Issues

Please use our [issue template](https://github.com/apache/dubbo/issues/new?template=dubbo-issue-report-template.md) when reporting bugs.

---

## 🔐 Reporting Security Vulnerabilities

Please report vulnerabilities **privately** to [security@dubbo.apache.org](mailto:security@dubbo.apache.org).

---

## 📬 Contact

- **WeChat**: `apachedubbo`
- **DingTalk**: Group ID `37290003945`
- **Mailing List**: [Contact Guide](https://dubbo.apache.org/zh-cn/contact/)
- **Twitter**: [@ApacheDubbo](https://twitter.com/ApacheDubbo)
- **Security**: [security@dubbo.apache.org](mailto:security@dubbo.apache.org)

---

## 📄 License

Apache Dubbo is licensed under the [Apache License 2.0](https://github.com/apache/dubbo/blob/3.3/LICENSE).
