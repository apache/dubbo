# Dubbo 开发指南

本文档面向 Apache Dubbo 核心开发者，介绍如何设置开发环境、运行测试和贡献代码。

## 环境要求

- JDK 8+ (推荐 JDK 11 或 17)
- Maven 3.6+
- Git

## 快速开始

### 1. 克隆仓库

```bash
git clone https://github.com/apache/dubbo.git
cd dubbo
```

### 2. 编译项目

```bash
# 完整编译
mvn clean install -DskipTests

# 只编译核心模块
mvn clean install -DskipTests -pl dubbo-common,dubbo-config/dubbo-config-api,dubbo-rpc/dubbo-rpc-api,dubbo-registry/dubbo-registry-api
```

### 3. 运行示例

```bash
cd dubbo-demo/dubbo-demo-api/dubbo-demo-api-provider
mvn exec:java -Dexec.mainClass="org.apache.dubbo.demo.provider.Application"
```

## 项目结构

```
dubbo/
├── dubbo-common/              # 公共工具类
├── dubbo-serialization/       # 序列化扩展
├── dubbo-remoting/            # 网络通信
├── dubbo-rpc/                 # RPC 层
├── dubbo-cluster/             # 集群容错
├── dubbo-registry/            # 注册中心
├── dubbo-config/              # 配置管理
├── dubbo-metadata/            # 元数据管理
├── dubbo-metrics/             # 监控指标
├── dubbo-monitor/             # 监控中心
├── dubbo-filter/              # 过滤器扩展
├── dubbo-plugin/              # 插件
├── dubbo-admin/               # 控制台
├── dubbo-demo/                # 示例
└── dubbo-test/                # 测试工具
```

## 开发模式

### IDE 设置

**IntelliJ IDEA:**
1. `File > New > Project from Existing Sources` 选择 `pom.xml`
2. 启用 Lombok 插件: `Settings > Plugins > Marketplace`
3. 配置 Maven:
   - `Settings > Build > Build Tools > Maven`
   - Maven home: 使用 bundled 或自定义安装
4. 配置 JDK:
   - `Project Structure > SDKs`
   - 添加 JDK 11 或 17

### 常用命令

```bash
# 编译
mvn clean compile

# 打包
mvn clean package -DskipTests

# 安装到本地仓库
mvn clean install -DskipTests

# 运行单元测试
mvn test

# 代码风格检查
mvn checkstyle:check

# 格式化代码
mvn spotless:apply
```

## 调试技巧

### 启用 DEBUG 日志

```java
# logback.xml
<logger name="org.apache.dubbo" level="DEBUG"/>
```

或通过系统属性:
```bash
-Dorg.apache.dubbo.config.log.level=DEBUG
```

### 远程调试

```bash
# 启动时添加 JVM 参数
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
```

然后在 IDEA 中配置 Remote Debug，端口 5005。

### 查看依赖树

```bash
mvn dependency:tree -Dincludes=org.apache.dubbo
```

## 编写测试

### 单元测试

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MyServiceTest {
    @Test
    void testService() {
        // Arrange
        MyService service = new MyService();
        
        // Act
        String result = service.sayHello("Dubbo");
        
        // Assert
        assertEquals("Hello Dubbo", result);
    }
}
```

### 集成测试

```bash
# 运行集成测试
mvn verify -Pit

# 运行指定测试类
mvn test -Dtest=MyServiceTest
```

## 代码规范

### 代码风格

Dubbo 使用 Apache 代码风格:

```bash
# 检查代码风格
mvn checkstyle:check

# 自动格式化
mvn spotless:apply
```

### 提交信息

格式: `[ISSUE-XXX] Description`

示例:
```
[DUBBO-1234] Fix service discovery timeout issue
[DUBBO-5678] Add support for new protocol
```

注意事项:
- 提交信息使用英文
- 关联的 Issue 编号必须在最前面
- 描述清晰简洁

## 性能测试

### 基准测试

```bash
cd dubbo-benchmark
mvn clean package

# 运行基准测试
java -jar target/benchmarks.jar
```

### 压力测试

使用 Dubbo 自带的压测工具:

```bash
cd dubbo-test/dubbo-test-tools
mvn clean package

# 运行压测
java -jar target/dubbo-test-tools.jar
```

## 贡献流程

1. **Fork 仓库**
   ```bash
   gh repo fork apache/dubbo
   ```

2. **创建分支**
   ```bash
   git checkout -b fix/DUBBO-XXX-short-description
   ```

3. **开发和测试**
   - 编写代码
   - 添加测试
   - 确保所有测试通过

4. **提交代码**
   ```bash
   git add .
   git commit -m "[DUBBO-XXX] Fix: description"
   git push origin fix/DUBBO-XXX-short-description
   ```

5. **创建 Pull Request**
   - 填写 PR 模板
   - 关联相关 Issue
   - 等待 Review

## 常见问题

**Q: 编译失败，找不到依赖**
```bash
# 清理并重新编译
mvn clean install -U
```

**Q: 测试失败**
```bash
# 跳过测试编译
mvn clean install -DskipTests

# 或只运行特定模块测试
mvn test -pl dubbo-common
```

**Q: IDEA 显示大量错误，但 Maven 编译通过**
```
# 重新导入 Maven 项目
# IDEA: Maven 面板 -> Reimport All Maven Projects
```

## 资源链接

- [官方文档](https://dubbo.apache.org/)
- [API 文档](https://dubbo.apache.org/zh-cn/overview/mannual/java-sdk/)
- [开发者指南](https://dubbo.apache.org/zh-cn/overview/mannual/)
- [社区讨论](https://github.com/apache/dubbo/discussions)

## 获取帮助

- GitHub Issues: https://github.com/apache/dubbo/issues
- 邮件列表: dev@dubbo.apache.org
- Slack: https://apache-dubbo.slack.com

---

## 架构设计

### 核心组件

| 组件 | 职责 | 关键类 |
|------|------|--------|
| Invoker | 调用抽象 | Invoker, AbstractInvoker |
| Protocol | 协议扩展 | Protocol, ProtocolFilterWrapper |
| Exporter | 服务导出 | Exporter, AbstractExporter |
| Cluster | 集群容错 | Cluster, FailoverCluster |
| LoadBalance | 负载均衡 | LoadBalance, RandomLoadBalance |
| Registry | 注册中心 | Registry, AbstractRegistry |

### 扩展机制

Dubbo 使用 SPI (Service Provider Interface) 机制:

```java
// 1. 定义扩展接口
@SPI("default")
public interface MyExtension {
    void doSomething();
}

// 2. 实现扩展
public class MyExtensionImpl implements MyExtension {
    @Override
    public void doSomething() {
        // implementation
    }
}

// 3. 注册扩展
// META-INF/dubbo/org.apache.dubbo.MyExtension
// myImpl=org.apache.dubbo.MyExtensionImpl

// 4. 使用扩展
ExtensionLoader.getExtensionLoader(MyExtension.class)
    .getExtension("myImpl");
```

## 故障排查

### 服务调用超时

排查步骤:
1. 检查网络连通性
2. 查看 provider 是否存活
3. 调整 timeout 配置
4. 检查 provider 性能

### 注册中心连接失败

排查步骤:
1. 确认注册中心地址正确
2. 检查网络防火墙
3. 查看注册中心日志
4. 验证账号权限

### 序列化异常

排查步骤:
1. 确认序列化方式一致
2. 检查类路径是否包含序列化类
3. 验证类版本兼容性

## 社区贡献

### 如何开始

1. **Good First Issues**: https://github.com/apache/dubbo/labels/good%20first%20issue
2. **Help Wanted**: https://github.com/apache/dubbo/labels/help%20wanted
3. **Documentation**: https://github.com/apache/dubbo/labels/type%2Fdocumentation

### 代码审查流程

1. 提交 PR
2. CI 自动化测试
3. Committer Review
4. 处理反馈
5. Merge

---

感谢你对 Apache Dubbo 的贡献！🎉
