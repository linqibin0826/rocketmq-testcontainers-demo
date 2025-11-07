package com.example.rocketmq;

import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * RocketMQ TestContainers 支持类 V2
 * <p>
 * 优化策略:
 * 1. 使用固定端口映射而不是动态端口
 * 2. 使用 Testcontainers 的 network alias
 * 3. 简化 Broker 配置，使用环境变量
 * 4. 增加更稳定的等待策略
 */
public class RocketMQContainerSupportV2 {

    private static final Logger log = LoggerFactory.getLogger(RocketMQContainerSupportV2.class);

    // 使用固定端口避免动态端口导致的问题
    private static final String ROCKETMQ_IMAGE = "apache/rocketmq:5.3.1";
    private static final int NAMESRV_PORT = 9876;
    private static final int BROKER_PORT_10909 = 10909;
    private static final int BROKER_PORT_10911 = 10911;
    private static final int BROKER_PORT_10912 = 10912;

    // 固定映射端口
    private static final int HOST_NAMESRV_PORT = 19876;
    private static final int HOST_BROKER_PORT_10909 = 20909;
    private static final int HOST_BROKER_PORT_10911 = 20911;
    private static final int HOST_BROKER_PORT_10912 = 20912;

    private final Network network;
    private final GenericContainer<?> nameserverContainer;
    private GenericContainer<?> brokerContainer;

    public RocketMQContainerSupportV2() {
        this.network = Network.newNetwork();
        this.nameserverContainer = createNameserverContainer();
    }

    /**
     * 创建 NameServer 容器
     */
    private GenericContainer<?> createNameserverContainer() {
        log.info("创建 RocketMQ NameServer 容器（固定端口模式）");

        return new GenericContainer<>(DockerImageName.parse(ROCKETMQ_IMAGE))
                .withNetwork(network)
                .withNetworkAliases("rocketmq-nameserver")
                .withExposedPorts(NAMESRV_PORT)
                .withCreateContainerCmdModifier(cmd -> {
                    cmd.withHostConfig(cmd.getHostConfig()
                            .withPortBindings(
                                    new com.github.dockerjava.api.model.PortBinding(
                                            com.github.dockerjava.api.model.Ports.Binding.bindPort(HOST_NAMESRV_PORT),
                                            new com.github.dockerjava.api.model.ExposedPort(NAMESRV_PORT)
                                    )
                            )
                    );
                })
                .withCommand("sh", "mqnamesrv")
                .waitingFor(Wait.forLogMessage(".*The Name Server boot success.*", 1))
                .withStartupTimeout(Duration.ofMinutes(2));
    }

    /**
     * 创建 Broker 容器 - 使用固定端口和环境变量配置
     */
    private GenericContainer<?> createBrokerContainer() {
        log.info("创建 RocketMQ Broker 容器（固定端口 + 环境变量模式）");

        return new GenericContainer<>(DockerImageName.parse(ROCKETMQ_IMAGE))
                .withNetwork(network)
                .withNetworkAliases("rocketmq-broker")
                .withExposedPorts(BROKER_PORT_10909, BROKER_PORT_10911, BROKER_PORT_10912)
                .withCreateContainerCmdModifier(cmd -> {
                    cmd.withHostConfig(cmd.getHostConfig()
                            .withPortBindings(
                                    new com.github.dockerjava.api.model.PortBinding(
                                            com.github.dockerjava.api.model.Ports.Binding.bindPort(HOST_BROKER_PORT_10909),
                                            new com.github.dockerjava.api.model.ExposedPort(BROKER_PORT_10909)
                                    ),
                                    new com.github.dockerjava.api.model.PortBinding(
                                            com.github.dockerjava.api.model.Ports.Binding.bindPort(HOST_BROKER_PORT_10911),
                                            new com.github.dockerjava.api.model.ExposedPort(BROKER_PORT_10911)
                                    ),
                                    new com.github.dockerjava.api.model.PortBinding(
                                            com.github.dockerjava.api.model.Ports.Binding.bindPort(HOST_BROKER_PORT_10912),
                                            new com.github.dockerjava.api.model.ExposedPort(BROKER_PORT_10912)
                                    )
                            )
                    );
                })
                .withEnv("NAMESRV_ADDR", "rocketmq-nameserver:9876")
                .withEnv("BROKER_IP1", "127.0.0.1")  // 使用 localhost
                .withEnv("brokerClusterName", "DefaultCluster")
                .withEnv("brokerName", "broker-a")
                .withEnv("brokerId", "0")
                .withCommand("sh", "-c",
                        "echo 'brokerClusterName=DefaultCluster\n" +
                        "brokerName=broker-a\n" +
                        "brokerId=0\n" +
                        "deleteWhen=04\n" +
                        "fileReservedTime=48\n" +
                        "brokerRole=ASYNC_MASTER\n" +
                        "flushDiskType=ASYNC_FLUSH\n" +
                        "brokerIP1=127.0.0.1\n" +
                        "listenPort=10911\n" +
                        "haListenPort=10909\n" +
                        "fastListenPort=10912\n" +
                        "brokerIP1=127.0.0.1\n" +
                        "listenPort=" + HOST_BROKER_PORT_10911 + "\n" +  // 使用映射后的端口
                        "autoCreateTopicEnable=true\n" +
                        "autoCreateSubscriptionGroup=true' > /tmp/broker.conf && " +
                        "cat /tmp/broker.conf && " +
                        "sh mqbroker -n rocketmq-nameserver:9876 -c /tmp/broker.conf"
                )
                .waitingFor(Wait.forLogMessage(".*The broker.*boot success.*", 1))
                .withStartupTimeout(Duration.ofMinutes(2))
                .dependsOn(nameserverContainer);
    }

    /**
     * 启动容器
     */
    public void start() {
        log.info("=== 启动 RocketMQ NameServer 容器 ===");
        nameserverContainer.start();
        log.info("NameServer 启动成功，固定端口: {}", HOST_NAMESRV_PORT);

        log.info("=== 创建 RocketMQ Broker 容器 ===");
        this.brokerContainer = createBrokerContainer();

        log.info("=== 启动 RocketMQ Broker 容器 ===");
        brokerContainer.start();

        log.info("=== RocketMQ 容器启动完成 ===");
        log.info("NameServer 地址: localhost:{}", HOST_NAMESRV_PORT);
        log.info("Broker 端口: 10909={}, 10911={}, 10912={}",
                HOST_BROKER_PORT_10909, HOST_BROKER_PORT_10911, HOST_BROKER_PORT_10912);

        // 等待 Broker 注册到 NameServer
        waitForBrokerRegistration();
    }

    /**
     * 等待 Broker 注册到 NameServer
     */
    private void waitForBrokerRegistration() {
        log.info("等待 Broker 注册到 NameServer...");
        try {
            // 给予充足的时间让 Broker 完成注册
            Awaitility.await()
                    .atMost(30, TimeUnit.SECONDS)
                    .pollInterval(2, TimeUnit.SECONDS)
                    .until(() -> {
                        try {
                            var result = brokerContainer.execInContainer(
                                    "sh", "mqadmin", "clusterList",
                                    "-n", "rocketmq-nameserver:9876"
                            );
                            String output = result.getStdout();
                            boolean registered = output.contains("broker-a");
                            if (registered) {
                                log.info("✅ Broker 已成功注册到 NameServer");
                            } else {
                                log.debug("Broker 尚未注册，继续等待...");
                            }
                            return registered;
                        } catch (Exception e) {
                            log.debug("检查 Broker 注册状态失败: {}", e.getMessage());
                            return false;
                        }
                    });
        } catch (Exception e) {
            log.warn("等待 Broker 注册超时，但将继续执行测试: {}", e.getMessage());
        }
    }

    /**
     * 停止容器
     */
    public void stop() {
        log.info("停止 RocketMQ 容器");
        if (brokerContainer != null && brokerContainer.isRunning()) {
            brokerContainer.stop();
        }
        if (nameserverContainer != null && nameserverContainer.isRunning()) {
            nameserverContainer.stop();
        }
        if (network != null) {
            network.close();
        }
    }

    /**
     * 获取 NameServer 地址（使用固定端口）
     */
    public String getNameserverAddress() {
        return "localhost:" + HOST_NAMESRV_PORT;
    }

    public GenericContainer<?> getNameserverContainer() {
        return nameserverContainer;
    }

    public GenericContainer<?> getBrokerContainer() {
        return brokerContainer;
    }
}
