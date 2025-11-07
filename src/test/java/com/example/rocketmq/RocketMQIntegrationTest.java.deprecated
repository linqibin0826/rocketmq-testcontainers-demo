package com.example.rocketmq;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RocketMQ TestContainers 集成测试
 * <p>
 * 验证基于 Apache Camel 方案的 RocketMQ TestContainers 配置
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RocketMQIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(RocketMQIntegrationTest.class);

    private static final String TEST_TOPIC = "TEST_TOPIC";
    private static final String TEST_TAG = "TEST_TAG";
    private static final String PRODUCER_GROUP = "test-producer-group";

    private static RocketMQContainerSupport rocketmqSupport;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private TestMessageConsumer messageConsumer;

    private static final AtomicInteger messageCounter = new AtomicInteger(0);

    /**
     * 启动 RocketMQ 容器
     */
    @BeforeAll
    static void setupContainers() {
        log.info("========================================");
        log.info("初始化 RocketMQ TestContainers");
        log.info("========================================");

        rocketmqSupport = new RocketMQContainerSupport();
        rocketmqSupport.start();

        log.info("NameServer 地址: {}", rocketmqSupport.getNameserverAddress());

        // ✅ 等待 Broker 注册到 NameServer (不手动创建 Topic,使用自动创建)
        log.info("等待 Broker 注册到 NameServer...");
        try {
            Thread.sleep(5000);  // 等待5秒让 Broker 完成注册
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("========================================");
        log.info("RocketMQ TestContainers 初始化完成");
        log.info("========================================");
    }

    /**
     * 动态配置 RocketMQ NameServer 地址
     */
    @DynamicPropertySource
    static void configureRocketMQ(DynamicPropertyRegistry registry) {
        // ✅ 关键: 使用宿主机映射端口
        String nameServerAddr = rocketmqSupport.getNameserverAddress();
        registry.add("rocketmq.name-server", () -> nameServerAddr);
        registry.add("rocketmq.producer.group", () -> PRODUCER_GROUP);

        log.info("动态配置 RocketMQ NameServer: {}", nameServerAddr);
    }

    /**
     * 停止容器
     */
    @AfterAll
    static void tearDownContainers() {
        log.info("清理 RocketMQ TestContainers");

        if (rocketmqSupport != null) {
            rocketmqSupport.stop();
        }

        log.info("RocketMQ TestContainers 已停止");
    }

    /**
     * 测试 1: 验证容器启动成功
     */
    @Test
    @Order(1)
    @DisplayName("测试1: 验证 RocketMQ 容器启动成功")
    void testContainersStarted() {
        log.info("========================================");
        log.info("测试 1: 验证容器启动");
        log.info("========================================");

        Assertions.assertNotNull(rocketmqSupport, "RocketMQ 支持类不应为 null");
        Assertions.assertTrue(
                rocketmqSupport.getNameserverContainer().isRunning(),
                "NameServer 容器应该正在运行"
        );
        Assertions.assertTrue(
                rocketmqSupport.getBrokerContainer().isRunning(),
                "Broker 容器应该正在运行"
        );

        String nameServerAddr = rocketmqSupport.getNameserverAddress();
        Assertions.assertNotNull(nameServerAddr, "NameServer 地址不应为 null");
        Assertions.assertTrue(
                nameServerAddr.startsWith("localhost:") || nameServerAddr.startsWith("127.0.0.1:"),
                "NameServer 地址应该是 localhost"
        );

        log.info("✅ 容器启动验证通过");
        log.info("   NameServer: {}", nameServerAddr);
    }

    /**
     * 测试 2: 验证 RocketMQTemplate 注入成功
     */
    @Test
    @Order(2)
    @DisplayName("测试2: 验证 RocketMQTemplate 注入成功")
    void testRocketMQTemplateInjected() {
        log.info("========================================");
        log.info("测试 2: 验证 RocketMQTemplate 注入");
        log.info("========================================");

        Assertions.assertNotNull(rocketMQTemplate, "RocketMQTemplate 不应为 null");

        log.info("✅ RocketMQTemplate 注入成功");
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

        String messageBody = "Hello RocketMQ - " + System.currentTimeMillis();
        String destination = TEST_TOPIC + ":" + TEST_TAG;

        log.info("发送消息到: {}", destination);
        log.info("消息内容: {}", messageBody);

        // ✅ 关键测试: 发送消息应该成功，不会出现 "No route info" 错误
        Assertions.assertDoesNotThrow(() -> {
            var sendResult = rocketMQTemplate.syncSend(destination, messageBody);
            log.info("消息发送成功，MessageId: {}", sendResult.getMsgId());
            Assertions.assertNotNull(sendResult.getMsgId(), "消息 ID 不应为 null");
        }, "发送消息不应抛出异常");

        log.info("✅ 同步消息发送成功");
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
        int batchSize = 5;

        for (int i = 0; i < batchSize; i++) {
            final int messageIndex = i;  // Lambda 需要 final 变量
            String messageBody = "Batch Message #" + messageIndex + " - " + System.currentTimeMillis();

            Assertions.assertDoesNotThrow(() -> {
                var sendResult = rocketMQTemplate.syncSend(destination, messageBody);
                log.info("消息 #{} 发送成功，MessageId: {}", messageIndex, sendResult.getMsgId());
            }, "批量发送消息不应抛出异常");

            messageCounter.incrementAndGet();
        }

        log.info("✅ 批量消息发送成功，共发送 {} 条消息", batchSize);
        log.info("   累计消息数: {}", messageCounter.get());
    }

    /**
     * 测试 5: 发送带有自定义 Key 的消息
     */
    @Test
    @Order(5)
    @DisplayName("测试5: 发送带有自定义 Key 的消息")
    void testSendMessageWithKey() {
        log.info("========================================");
        log.info("测试 5: 发送带自定义 Key 的消息");
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
            var sendResult = rocketMQTemplate.syncSend(destination, message);
            log.info("消息发送成功，MessageId: {}, Key: {}", sendResult.getMsgId(), messageKey);
            Assertions.assertNotNull(sendResult.getMsgId());
        }, "发送带 Key 的消息不应抛出异常");

        log.info("✅ 带 Key 的消息发送成功");
    }

    /**
     * 测试 6: 验证消息发送性能
     */
    @Test
    @Order(6)
    @DisplayName("测试6: 验证消息发送性能")
    void testMessageSendingPerformance() {
        log.info("========================================");
        log.info("测试 6: 消息发送性能测试");
        log.info("========================================");

        String destination = TEST_TOPIC + ":" + TEST_TAG;
        int messageCount = 10;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < messageCount; i++) {
            String messageBody = "Performance Test Message #" + i;
            rocketMQTemplate.syncSend(destination, messageBody);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double avgTime = (double) duration / messageCount;

        log.info("✅ 性能测试完成");
        log.info("   发送消息数: {}", messageCount);
        log.info("   总耗时: {} ms", duration);
        log.info("   平均耗时: {} ms/条", String.format("%.2f", avgTime));

        Assertions.assertTrue(avgTime < 1000, "平均发送时间应小于 1 秒");
    }

    /**
     * 测试 7: 验证异步发送
     */
    @Test
    @Order(7)
    @DisplayName("测试7: 验证异步发送")
    void testAsyncSend() {
        log.info("========================================");
        log.info("测试 7: 异步发送消息");
        log.info("========================================");

        String messageBody = "Async Message - " + System.currentTimeMillis();
        String destination = TEST_TOPIC + ":" + TEST_TAG;
        AtomicInteger callbackCounter = new AtomicInteger(0);

        log.info("异步发送消息: {}", messageBody);

        rocketMQTemplate.asyncSend(destination, messageBody, new org.apache.rocketmq.client.producer.SendCallback() {
            @Override
            public void onSuccess(org.apache.rocketmq.client.producer.SendResult sendResult) {
                log.info("异步消息发送成功，MessageId: {}", sendResult.getMsgId());
                callbackCounter.incrementAndGet();
            }

            @Override
            public void onException(Throwable e) {
                log.error("异步消息发送失败", e);
                Assertions.fail("异步发送不应失败: " + e.getMessage());
            }
        });

        // 等待回调执行
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> callbackCounter.get() > 0);

        log.info("✅ 异步发送成功，回调已执行");
    }

    /**
     * 测试 8: 验证消息发送和消费完整流程
     * <p>
     * 这是最重要的测试，验证消息从发送到消费的完整链路
     */
    @Test
    @Order(8)
    @DisplayName("测试8: 验证消息发送和消费完整流程")
    void testSendAndConsumeMessage() throws InterruptedException {
        log.info("========================================");
        log.info("测试 8: 消息发送和消费完整流程");
        log.info("========================================");

        // 清空之前接收到的消息
        messageConsumer.clearMessages();

        // 准备发送3条消息
        int messageCount = 3;
        CountDownLatch latch = new CountDownLatch(messageCount);
        messageConsumer.setLatch(latch);

        String destination = TEST_TOPIC + ":" + TEST_TAG;

        log.info("准备发送 {} 条消息", messageCount);

        // 发送消息
        for (int i = 0; i < messageCount; i++) {
            String messageBody = "Consumer Test Message #" + i + " - " + System.currentTimeMillis();
            var sendResult = rocketMQTemplate.syncSend(destination, messageBody);
            log.info("消息 #{} 发送成功，MessageId: {}", i, sendResult.getMsgId());

            // 短暂延迟，避免消息发送过快
            Thread.sleep(100);
        }

        log.info("所有消息已发送，等待消费者接收...");

        // 等待消费者接收所有消息(最多等待30秒)
        boolean received = latch.await(30, TimeUnit.SECONDS);

        Assertions.assertTrue(received, "消费者应该在30秒内接收到所有消息");

        // 验证接收到的消息数量
        int receivedCount = messageConsumer.getMessageCount();
        log.info("消费者接收到的消息数量: {}", receivedCount);

        Assertions.assertTrue(
                receivedCount >= messageCount,
                String.format("消费者应该至少接收到 %d 条消息，实际接收: %d", messageCount, receivedCount)
        );

        // 打印接收到的消息
        var messages = messageConsumer.getReceivedMessages();
        for (int i = 0; i < messages.size(); i++) {
            log.info("接收到的消息 #{}: {}", i, messages.get(i));
        }

        log.info("✅ 消息发送和消费完整流程验证通过");
        log.info("   发送消息数: {}", messageCount);
        log.info("   接收消息数: {}", receivedCount);
    }
}
