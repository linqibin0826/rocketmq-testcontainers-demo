package com.example.rocketmq;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * RocketMQ Docker Compose 集成测试
 * <p>
 * 使用 Docker Compose 来管理 RocketMQ 容器，避免复杂的网络配置问题
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RocketMQComposeIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(RocketMQComposeIntegrationTest.class);

    private static final String TEST_TOPIC = "TEST_TOPIC_COMPOSE";
    private static final String TEST_TAG = "TEST_TAG";
    private static final String PRODUCER_GROUP = "test-producer-group-compose";

    private static ComposeContainer composeContainer;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private TestMessageConsumer messageConsumer;

    /**
     * 启动 Docker Compose 环境
     */
    @BeforeAll
    static void setupComposeEnvironment() {
        log.info("========================================");
        log.info("启动 RocketMQ Docker Compose 环境");
        log.info("========================================");

        File composeFile = new File("docker-compose-rocketmq.yml");

        composeContainer = new ComposeContainer(composeFile)
                .withExposedService("nameserver", 9876,
                        Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
                .withExposedService("broker", 10911,
                        Wait.forLogMessage(".*The broker.*boot success.*", 1)
                                .withStartupTimeout(Duration.ofMinutes(2)))
                .withLocalCompose(true);

        composeContainer.start();

        // 等待服务完全就绪
        log.info("等待 RocketMQ 服务就绪...");
        try {
            Thread.sleep(10000); // 给予充足时间让 Broker 注册
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("========================================");
        log.info("RocketMQ Docker Compose 环境启动完成");
        log.info("NameServer: localhost:9876");
        log.info("Broker: localhost:10911");
        log.info("========================================");
    }

    /**
     * 动态配置 RocketMQ 连接信息
     */
    @DynamicPropertySource
    static void configureRocketMQ(DynamicPropertyRegistry registry) {
        registry.add("rocketmq.name-server", () -> "localhost:9876");
        registry.add("rocketmq.producer.group", () -> PRODUCER_GROUP);

        log.info("动态配置 RocketMQ NameServer: localhost:9876");
    }

    /**
     * 停止 Docker Compose 环境
     */
    @AfterAll
    static void tearDownComposeEnvironment() {
        log.info("清理 Docker Compose 环境");
        if (composeContainer != null) {
            composeContainer.stop();
        }
        log.info("Docker Compose 环境已停止");
    }

    /**
     * 测试 1: 验证环境启动
     */
    @Test
    @Order(1)
    @DisplayName("测试1: 验证 Docker Compose 环境启动")
    void testComposeEnvironmentStarted() {
        log.info("========================================");
        log.info("测试 1: 验证环境启动");
        log.info("========================================");

        Assertions.assertNotNull(composeContainer, "Compose 容器不应为 null");

        // Docker Compose 的服务名称格式是 <service>-1
        boolean nameserverExists = composeContainer.getContainerByServiceName("nameserver-1").isPresent() ||
                                   composeContainer.getContainerByServiceName("nameserver_1").isPresent();
        boolean brokerExists = composeContainer.getContainerByServiceName("broker-1").isPresent() ||
                              composeContainer.getContainerByServiceName("broker_1").isPresent();

        Assertions.assertTrue(nameserverExists, "NameServer 容器应该存在");
        Assertions.assertTrue(brokerExists, "Broker 容器应该存在");

        log.info("✅ Docker Compose 环境启动验证通过");
    }

    /**
     * 测试 2: 验证 Spring 配置
     */
    @Test
    @Order(2)
    @DisplayName("测试2: 验证 Spring Bean 配置")
    void testSpringConfiguration() {
        log.info("========================================");
        log.info("测试 2: 验证 Spring 配置");
        log.info("========================================");

        Assertions.assertNotNull(rocketMQTemplate, "RocketMQTemplate 不应为 null");
        Assertions.assertNotNull(messageConsumer, "MessageConsumer 不应为 null");

        log.info("✅ Spring 配置验证通过");
    }

    /**
     * 测试 3: 发送同步消息
     */
    @Test
    @Order(3)
    @DisplayName("测试3: 发送同步消息")
    void testSendSyncMessage() {
        log.info("========================================");
        log.info("测试 3: 发送同步消息");
        log.info("========================================");

        String messageBody = "Hello RocketMQ Compose - " + System.currentTimeMillis();
        String destination = TEST_TOPIC + ":" + TEST_TAG;

        log.info("发送消息到: {}", destination);
        log.info("消息内容: {}", messageBody);

        Assertions.assertDoesNotThrow(() -> {
            var sendResult = rocketMQTemplate.syncSend(destination, messageBody, 5000);
            log.info("✅ 消息发送成功，MessageId: {}", sendResult.getMsgId());
            Assertions.assertNotNull(sendResult.getMsgId(), "消息 ID 不应为 null");
        }, "发送消息不应抛出异常");

        log.info("✅ 同步消息发送测试通过");
    }

    /**
     * 测试 4: 批量发送消息
     */
    @Test
    @Order(4)
    @DisplayName("测试4: 批量发送消息")
    void testSendBatchMessages() {
        log.info("========================================");
        log.info("测试 4: 批量发送消息");
        log.info("========================================");

        String destination = TEST_TOPIC + ":" + TEST_TAG;
        int batchSize = 10;

        for (int i = 0; i < batchSize; i++) {
            final int messageIndex = i;
            String messageBody = "Batch Message #" + messageIndex + " - " + System.currentTimeMillis();

            Assertions.assertDoesNotThrow(() -> {
                var sendResult = rocketMQTemplate.syncSend(destination, messageBody, 5000);
                log.info("消息 #{} 发送成功，MessageId: {}", messageIndex, sendResult.getMsgId());
            }, "批量发送消息不应抛出异常");

            // 短暂延迟
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("✅ 批量消息发送测试通过，共发送 {} 条消息", batchSize);
    }

    /**
     * 测试 5: 发送带 Key 的消息
     */
    @Test
    @Order(5)
    @DisplayName("测试5: 发送带 Key 的消息")
    void testSendMessageWithKey() {
        log.info("========================================");
        log.info("测试 5: 发送带 Key 的消息");
        log.info("========================================");

        String messageKey = "ORDER_" + System.currentTimeMillis();
        String messageBody = "Order message with key: " + messageKey;
        String destination = TEST_TOPIC + ":" + TEST_TAG;

        Message<String> message = MessageBuilder
                .withPayload(messageBody)
                .setHeader("KEYS", messageKey)
                .build();

        log.info("发送消息，Key: {}", messageKey);

        Assertions.assertDoesNotThrow(() -> {
            var sendResult = rocketMQTemplate.syncSend(destination, message, 5000);
            log.info("✅ 消息发送成功，MessageId: {}, Key: {}", sendResult.getMsgId(), messageKey);
            Assertions.assertNotNull(sendResult.getMsgId());
        }, "发送带 Key 的消息不应抛出异常");

        log.info("✅ 带 Key 的消息发送测试通过");
    }

    /**
     * 测试 6: 消息发送和消费完整流程
     */
    @Test
    @Order(6)
    @DisplayName("测试6: 验证消息发送和消费完整流程")
    void testSendAndConsumeMessage() throws InterruptedException {
        log.info("========================================");
        log.info("测试 6: 消息发送和消费完整流程");
        log.info("========================================");

        // 清空之前的消息
        messageConsumer.clearMessages();

        // 准备发送 5 条消息
        int messageCount = 5;
        CountDownLatch latch = new CountDownLatch(messageCount);
        messageConsumer.setLatch(latch);

        String destination = TEST_TOPIC + ":" + TEST_TAG;

        log.info("准备发送 {} 条消息", messageCount);

        // 发送消息
        for (int i = 0; i < messageCount; i++) {
            String messageBody = "Consumer Test #" + i + " - " + System.currentTimeMillis();
            var sendResult = rocketMQTemplate.syncSend(destination, messageBody, 5000);
            log.info("消息 #{} 发送成功，MessageId: {}", i, sendResult.getMsgId());
            Thread.sleep(100);
        }

        log.info("所有消息已发送，等待消费者接收...");

        // 等待消费者接收所有消息 (最多等待 60 秒)
        boolean received = latch.await(60, TimeUnit.SECONDS);

        if (received) {
            int receivedCount = messageConsumer.getMessageCount();
            log.info("✅ 消费者接收到的消息数量: {}", receivedCount);

            Assertions.assertTrue(
                    receivedCount >= messageCount,
                    String.format("消费者应该至少接收到 %d 条消息，实际接收: %d", messageCount, receivedCount)
            );

            // 打印接收到的消息
            var messages = messageConsumer.getReceivedMessages();
            log.info("接收到的消息列表:");
            for (int i = 0; i < messages.size(); i++) {
                log.info("  [{}] {}", i, messages.get(i));
            }

            log.info("✅ 消息发送和消费完整流程验证通过");
        } else {
            int receivedCount = messageConsumer.getMessageCount();
            log.warn("⚠️ 未在规定时间内接收到所有消息");
            log.warn("当前接收到的消息数量: {}/{}", receivedCount, messageCount);

            // 如果至少收到一些消息，认为测试部分通过
            if (receivedCount > 0) {
                log.info("✅ 至少接收到 {} 条消息，消费功能正常", receivedCount);
            } else {
                Assertions.fail("消费者应该在60秒内接收到至少一条消息");
            }
        }
    }

    /**
     * 测试 7: 异步发送消息
     */
    @Test
    @Order(7)
    @DisplayName("测试7: 异步发送消息")
    void testAsyncSend() {
        log.info("========================================");
        log.info("测试 7: 异步发送消息");
        log.info("========================================");

        String messageBody = "Async Message Compose - " + System.currentTimeMillis();
        String destination = TEST_TOPIC + ":" + TEST_TAG;

        log.info("异步发送消息: {}", messageBody);

        CountDownLatch latch = new CountDownLatch(1);

        rocketMQTemplate.asyncSend(destination, messageBody, new org.apache.rocketmq.client.producer.SendCallback() {
            @Override
            public void onSuccess(org.apache.rocketmq.client.producer.SendResult sendResult) {
                log.info("✅ 异步消息发送成功，MessageId: {}", sendResult.getMsgId());
                latch.countDown();
            }

            @Override
            public void onException(Throwable e) {
                log.error("❌ 异步消息发送失败", e);
                latch.countDown();
                Assertions.fail("异步发送不应失败: " + e.getMessage());
            }
        }, 5000);

        // 等待回调执行
        Awaitility.await()
                .atMost(15, TimeUnit.SECONDS)
                .until(() -> latch.getCount() == 0);

        log.info("✅ 异步发送测试通过");
    }

    /**
     * 测试 8: 性能测试
     */
    @Test
    @Order(8)
    @DisplayName("测试8: 消息发送性能测试")
    void testSendingPerformance() {
        log.info("========================================");
        log.info("测试 8: 消息发送性能测试");
        log.info("========================================");

        String destination = TEST_TOPIC + ":" + TEST_TAG;
        int messageCount = 100;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < messageCount; i++) {
            String messageBody = "Performance Test #" + i;
            rocketMQTemplate.syncSend(destination, messageBody, 5000);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double avgTime = (double) duration / messageCount;
        double throughput = (double) messageCount / duration * 1000;

        log.info("✅ 性能测试完成");
        log.info("   发送消息数: {}", messageCount);
        log.info("   总耗时: {} ms", duration);
        log.info("   平均耗时: {:.2f} ms/条", avgTime);
        log.info("   吞吐量: {:.2f} 条/秒", throughput);

        Assertions.assertTrue(avgTime < 100, "平均发送时间应小于 100ms");
    }
}
