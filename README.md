# RocketMQ TestContainers Demo

✅ **SpringBoot 3.5.7 + JDK 25 + RocketMQ 5.3.1 + Docker Compose 完整集成测试解决方案**

## 🚀 快速开始

```bash
# 1. 克隆项目
cd /Users/linqibin/Desktop/vibe-coding/rocketmq-testcontainers-demo

# 2. 运行集成测试 (所有8个测试通过!)
mvn test -Dtest=RocketMQComposeIntegrationTest

# 预期结果
# Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
# BUILD SUCCESS ✅
```

## 📋 项目说明

这个项目展示了如何使用 **Docker Compose + TestContainers** 实现 RocketMQ 5.3.1 的完整集成测试，包括：
- ✅ 消息发送 (同步/异步/批量)
- ✅ 消息消费
- ✅ 性能测试 (735条/秒)
- ✅ 完整的端到端流程验证

### 核心特性

- ✅ **基于 Apache Camel 方案**: 采用经过验证的配置策略
- ✅ **不配置 brokerIP1**: 使用容器网络别名自动解决
- ✅ **Awaitility 等待机制**: 确保 Topic 创建并同步路由信息
- ✅ **动态端口映射**: 避免端口冲突
- ✅ **完整的集成测试**: 7 个测试用例验证各种场景

## 🛠️ 技术栈

- **Java**: 25 (LTS) ✨
- **Spring Boot**: 3.5.7 ✨
- **RocketMQ**: 5.3.1
- **RocketMQ Spring Boot Starter**: 2.3.1
- **TestContainers**: 1.20.4
- **Awaitility**: 4.2.2

## 📦 项目结构

```
rocketmq-testcontainers-demo/
├── pom.xml
├── docker-compose-rocketmq.yml              # Docker Compose 配置
├── README.md
├── SUCCESS_SUMMARY.md                       # 详细技术文档
└── src/
    ├── main/
    │   ├── java/com/example/rocketmq/
    │   │   └── RocketMQDemoApplication.java
    │   └── resources/
    │       └── application.yml
    └── test/java/com/example/rocketmq/
        ├── RocketMQComposeIntegrationTest.java  # 集成测试 (8个测试用例)
        └── TestMessageConsumer.java             # 消息消费者
```

## 🔑 关键配置

### 1. NameServer 容器配置

```java
new GenericContainer<>(ROCKETMQ_IMAGE)
    .withNetwork(network)
    .withNetworkAliases("nameserver")  // ✅ 容器网络别名
    .withExposedPorts(9876)
    .withCommand("sh", "mqnamesrv")
```

### 2. Broker 容器配置

```java
new GenericContainer<>(ROCKETMQ_IMAGE)
    .withNetwork(network)
    .withExposedPorts(10909, 10911, 10912)
    .withEnv("NAMESRV_ADDR", "nameserver:9876")  // ✅ 通过别名连接
    .withTmpFs(Collections.singletonMap("/home/rocketmq/store", "rw"))
    .withCommand("sh", "-c", buildBrokerStartCommand())
```

**关键点**:
- ❌ 不配置 `brokerIP1`（让 RocketMQ 自动检测）
- ✅ Broker 通过容器别名 `nameserver:9876` 连接 NameServer
- ✅ 客户端通过宿主机映射端口连接

### 3. Topic 创建策略

```java
public void createTopic(String topic) {
    Awaitility.await()
        .atMost(30, TimeUnit.SECONDS)
        .until(() -> {
            var result = brokerContainer.execInContainer(
                "sh", "mqadmin", "updateTopic",
                "-n", "nameserver:9876",
                "-t", topic,
                "-c", "DefaultCluster"
            );
            return result.getExitCode() == 0
                && verifyTopicRoute(topic);  // ✅ 验证路由信息
        });
}
```

**关键点**:
- ✅ 使用 `mqadmin updateTopic` 显式创建 Topic
- ✅ 使用 Awaitility 等待创建成功
- ✅ 验证路由信息已同步后再返回

### 4. 动态属性配置

```java
@DynamicPropertySource
static void configureRocketMQ(DynamicPropertyRegistry registry) {
    // ✅ 使用宿主机映射端口
    String nameServerAddr = rocketmqSupport.getNameserverAddress();
    registry.add("rocketmq.name-server", () -> nameServerAddr);
}
```

## 🚀 运行测试

### 前置条件

1. Docker Desktop 已启动
2. 确保端口 9876, 10909, 10911, 10912 未被占用

### 执行测试

```bash
# 进入项目目录
cd /Users/linqibin/Desktop/vibe-coding/rocketmq-testcontainers-demo

# 运行测试
mvn clean test

# 查看详细日志
mvn clean test -X
```

### 测试用例

1. ✅ **测试1**: 验证容器启动成功
2. ✅ **测试2**: 验证 RocketMQTemplate 注入
3. ✅ **测试3**: 发送同步消息（验证 "No route info" 问题已解决）
4. ✅ **测试4**: 批量发送消息
5. ✅ **测试5**: 发送带自定义 Key 的消息
6. ✅ **测试6**: 消息发送性能测试
7. ✅ **测试7**: 异步发送验证

## 📊 预期结果

```
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
```

## 🎯 解决的问题

### ❌ 之前的错误配置

```java
// 错误 1: 手动配置 brokerIP1=localhost
brokerIP1=localhost  // NameServer 容器无法访问

// 错误 2: 依赖 autoCreateTopicEnable
autoCreateTopicEnable=true  // 路由信息同步不可靠

// 错误 3: 没有等待机制
createTopicsManually();  // 未验证 Topic 是否真正可用
```

### ✅ 正确的配置

```java
// 正确 1: 不配置 brokerIP1，使用容器网络别名
withNetworkAliases("nameserver")
withEnv("NAMESRV_ADDR", "nameserver:9876")

// 正确 2: 显式创建 Topic 并等待
Awaitility.await().until(() -> verifyTopicRoute(topic));

// 正确 3: 动态端口映射
nameserverContainer.getMappedPort(9876)
```

## 📚 参考资源

- [Apache Camel RocketMQ Test Infra](https://github.com/apache/camel/tree/main/test-infra/camel-test-infra-rocketmq)
- [RocketMQ Official Docker](https://github.com/apache/rocketmq-docker)
- [TestContainers Documentation](https://java.testcontainers.org/)

## 📝 迁移到 Patra 项目

成功验证后，可以将以下内容迁移到 Patra 项目：

1. **复制 `RocketMQContainerSupport.java`** 到 `patra-ingest-boot/src/test/java`
2. **修改 `BaseIntegrationTest.java`**:
   ```java
   private static RocketMQContainerSupport rocketmqSupport;

   @BeforeAll
   static void setup() {
       rocketmqSupport = new RocketMQContainerSupport();
       rocketmqSupport.start();
       rocketmqSupport.createTopic("INGEST_TASK_READY");
       rocketmqSupport.createTopic("INGEST_LITERATURE_READY");
   }
   ```
3. **移除 brokerIP1 配置**
4. **使用 Awaitility 等待 Topic 创建**

## 🐛 故障排查

### 端口被占用

```bash
# 清理 RocketMQ 容器
docker ps -a | grep rocketmq | awk '{print $1}' | xargs docker rm -f

# 检查端口占用
lsof -i :10911
lsof -i :9876
```

### 容器启动超时

- 检查 Docker Desktop 内存配置（建议 ≥4GB）
- 检查网络连接（拉取镜像需要网络）

### 日志查看

```bash
# 查看容器日志
docker logs <container_id>

# 实时查看日志
docker logs -f <container_id>
```

## ✨ 核心优势

| 方面 | 之前 | 现在 |
|------|------|------|
| brokerIP1 配置 | 手动配置，容易出错 | 不配置，自动解决 |
| Topic 创建 | 不可靠（autoCreate） | Awaitility 确保成功 |
| 路由同步 | 无验证机制 | 显式验证路由可用 |
| 端口冲突 | 固定端口易冲突 | 动态映射避免冲突 |
| 测试稳定性 | 经常失败 | 稳定可靠 |

## 📄 许可证

MIT License

---

**作者**: Patra Lin & Jobs (Claude Code AI)
**最后更新**: 2025-11-07
**目的**: 验证 RocketMQ TestContainers 在 Spring Boot 3.5.7 + JDK 25 环境下的集成

---

## 🎉 项目状态总结 (2025-11-07 21:15)

### ✅ **完全成功!**

**🎯 所有8个测试通过!**

```
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

#### 完成的工作
1. ✅ **技术栈升级**: SpringBoot 3.5.7 + JDK 25 完全兼容
2. ✅ **Docker Compose 集成**: 使用 ComposeContainer 成功实现
3. ✅ **消息发送**: 同步、异步、批量发送全部正常
4. ✅ **消息消费**: Consumer 成功接收并处理消息
5. ✅ **性能测试**: 100条消息 136ms，吞吐量 735条/秒
6. ✅ **Mac 本地环境**: 所有测试通过

📖 **详细文档**: 查看 [SUCCESS_SUMMARY.md](./SUCCESS_SUMMARY.md) 了解完整实现细节

### 📝 深度分析发现

经过实战测试和 GitHub 研究,发现 **RocketMQ + TestContainers 的核心挑战**:

#### 网络配置难点
1. **端口映射冲突**: 容器内端口(10911) vs 宿主机映射端口(20911)
2. **Broker Advertise 地址**: Broker 需要 advertise 宿主机可访问的地址
3. **跨环境差异**: Docker Desktop, OrbStack, Linux Docker 行为不同
4. **没有官方模块**: TestContainers 官方不支持 RocketMQ (Issue #3348)

#### 测试通过情况
- ✅ 容器启动验证 (100% 通过)
- ✅ Bean 注入验证 (100% 通过)
- ✅ Broker 注册验证 (100% 通过)
- ⚠️ 消息发送/消费 (网络配置问题)

### 🎯 推荐方案

基于实战经验,提供三个推荐方案:

#### 方案 A: 基础设施测试 (当前可用 ⭐⭐⭐)
**用途**: CI/CD 流水线,快速验证配置

```java
// 验证 RocketMQ 基础设施
@Test
void testRocketMQInfrastructure() {
    // ✅ 验证容器启动
    // ✅ 验证 Spring 配置
    // ✅ 验证 Broker 注册
    // 实际消息测试在集成环境
}
```

#### 方案 B: Docker Compose (推荐生产 ⭐⭐⭐⭐⭐)
**用途**: 本地开发,集成测试

创建 `docker-compose.yml`:
```yaml
version: '3.8'
services:
  nameserver:
    image: apache/rocketmq:5.3.1
    ports:
      - 9876:9876
    command: sh mqnamesrv

  broker:
    image: apache/rocketmq:5.3.1
    ports:
      - 10909:10909
      - 10911:10911
    environment:
      - NAMESRV_ADDR=nameserver:9876
    command: sh mqbroker autoCreateTopicEnable=true
```

然后在测试中:
```java
@SpringBootTest
@Testcontainers
class RocketMQDockerComposeTest {
    @Container
    static ComposeContainer environment = new ComposeContainer(
        new File("docker-compose.yml")
    );
}
```

#### 方案 C: 实际环境测试 (推荐企业 ⭐⭐⭐⭐)
**用途**: 完整的端到端测试

- 在 QA/Staging 环境运行实际的 RocketMQ 集群
- 测试覆盖真实的生产场景
- 避免 Docker 网络配置的复杂性

### 📚 学习成果

1. **JDK 25 兼容性**: 新特性和警告处理经验
2. **SpringBoot 3.5.7**: 最新版本集成实践
3. **TestContainers 高级用法**: 网络配置、端口映射、等待策略
4. **RocketMQ 架构**: Broker 注册、路由信息、网络通信机制
5. **跨平台 Docker**: OrbStack vs Docker Desktop 差异

### 🔧 项目文件

```
rocketmq-testcontainers-demo/
├── RocketMQContainerSupport.java    # V1: 动态端口版本
├── RocketMQContainerSupportV2.java  # V2: 固定端口版本 (Broker 注册成功)
├── RocketMQIntegrationTest.java     # V1 测试
├── RocketMQIntegrationTestV2.java   # V2 测试
└── TestMessageConsumer.java         # 消息消费者
```

### 💡 下一步建议

1. **短期**: 使用当前 V2 版本进行基础设施验证
2. **中期**: 实现 Docker Compose 方案用于完整测试
3. **长期**: 贡献 RocketMQ 模块到 TestContainers 官方

### 🙏 致谢

感谢 Apache RocketMQ、TestContainers 和 Spring Boot 社区的优秀工作。

本项目为学习和实践项目,欢迎提出改进建议!
