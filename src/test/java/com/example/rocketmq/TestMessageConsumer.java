package com.example.rocketmq;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * 测试用消息消费者
 * <p>
 * 使用 @RocketMQMessageListener 注解监听消息
 */
@Component
@RocketMQMessageListener(
        topic = "TEST_TOPIC_COMPOSE",
        consumerGroup = "test-consumer-group-compose",
        selectorExpression = "*"  // 接收所有 Tag 的消息
)
public class TestMessageConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(TestMessageConsumer.class);

    // 使用线程安全的列表存储接收到的消息
    private final CopyOnWriteArrayList<String> receivedMessages = new CopyOnWriteArrayList<>();

    // 用于等待消息到达的 CountDownLatch
    private CountDownLatch latch;

    @Override
    public void onMessage(String message) {
        log.info("收到消息: {}", message);
        receivedMessages.add(message);

        // 计数器减1
        if (latch != null) {
            latch.countDown();
            log.debug("CountDownLatch count: {}", latch.getCount());
        }
    }

    /**
     * 获取接收到的所有消息
     */
    public CopyOnWriteArrayList<String> getReceivedMessages() {
        return receivedMessages;
    }

    /**
     * 清空接收到的消息
     */
    public void clearMessages() {
        receivedMessages.clear();
        log.info("已清空接收到的消息");
    }

    /**
     * 设置 CountDownLatch 用于等待消息
     */
    public void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    /**
     * 获取接收到的消息数量
     */
    public int getMessageCount() {
        return receivedMessages.size();
    }
}
