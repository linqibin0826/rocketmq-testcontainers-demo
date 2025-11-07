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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * RocketMQ TestContainers 集成测试 V2
 * <p>
 * 使用固定端口映射的优化版本
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RocketMQIntegrationTestV2 {

    private static final Logger log = LoggerFactory.getLogger(RocketMQIntegrationTestV2.class);

    private static final String TEST_TOPIC = "TEST_TOPIC_V2";
    private static final String TEST_TAG = "TEST_TAG";
    private static final String PRODUCER_GROUP = "test-producer-group-v2";

    private static RocketMQContainerSupportV2 rocketmqSupport;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private TestMessageConsumer messageConsumer;

    /**
     * 启动 RocketMQ 容器
     */
    @BeforeAll
    static void setupContainers() {
        log.info("========================================");
        log.info("初始化 RocketMQ TestContainers V2");
        log.info("========================================");

        rocketmqSupport = new RocketMQContainerSupportV2();
        rocketmqSupport.start();

        log.info("========================================");
        log.info("RocketMQ TestContainers V2 初始化完成");
        log.info("========================================");
    }

    /**
     * 动态配置 RocketMQ NameServer 地址
     */
    @DynamicPropertySource
    static void configureRocketMQ(DynamicPropertyRegistry registry) {
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
                nameServerAddr.startsWith("localhost:"),
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
        Assertions.assertNotNull(messageConsumer, "TestMessageConsumer 不应为 null");

        log.info("✅ RocketMQTemplate 和 MessageConsumer 注入成功");
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

        String messageBody = "Hello RocketMQ V2 - " + System.currentTimeMillis();
        String destination = TEST_TOPIC + ":" + TEST_TAG;

        log.info("发送消息到: {}", destination);
        log.info("消息内容: {}", messageBody);

        Assertions.assertDoesNotThrow(() -> {
            var sendResult = rocketMQTemplate.syncSend(destination, messageBody);
            log.info("✅ 消息发送成功，MessageId: {}", sendResult.getMsgId());
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
            final int messageIndex = i;
            String messageBody = "Batch Message #" + messageIndex + " - " + System.currentTimeMillis();

            Assertions.assertDoesNotThrow(() -> {
                var sendResult = rocketMQTemplate.syncSend(destination, messageBody);
                log.info("消息 #{} 发送成功，MessageId: {}", messageIndex, sendResult.getMsgId());
            }, "批量发送消息不应抛出异常");
        }

        log.info("✅ 批量消息发送成功，共发送 {} 条消息", batchSize);
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
            log.info("✅ 消息发送成功，MessageId: {}, Key: {}", sendResult.getMsgId(), messageKey);
            Assertions.assertNotNull(sendResult.getMsgId());
        }, "发送带 Key 的消息不应抛出异常");

        log.info("✅ 带 Key 的消息发送成功");
    }

    /**
     * 测试 6: 验证消息发送和消费完整流程
     */
    @Test
    @Order(6)
    @DisplayName("测试6: 验证消息发送和消费完整流程")
    void testSendAndConsumeMessage() throws InterruptedException {
        log.info("========================================");
        log.info("测试 6: 消息发送和消费完整流程");
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

            // 短暂延迟
            Thread.sleep(100);
        }

        log.info("所有消息已发送，等待消费者接收...");

        // 等待消费者接收所有消息(最多等待30秒)
        boolean received = latch.await(30, TimeUnit.SECONDS);

        if (received) {
            int receivedCount = messageConsumer.getMessageCount();
            log.info("✅ 消费者接收到的消息数量: {}", receivedCount);

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
        } else {
            log.warn("⚠️ 未在规定时间内接收到所有消息");
            log.info("当前接收到的消息数量: {}", messageConsumer.getMessageCount());
            Assertions.fail("消费者应该在30秒内接收到所有消息");
        }
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

        String messageBody = "Async Message V2 - " + System.currentTimeMillis();
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
                Assertions.fail("异步发送不应失败: " + e.getMessage());
                latch.countDown();
            }
        });

        // 等待回调执行
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> latch.getCount() == 0);

        log.info("✅ 异步发送成功，回调已执行");
    }
}
