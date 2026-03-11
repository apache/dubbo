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

感谢你对 Apache Dubbo 的贡献！🎉
