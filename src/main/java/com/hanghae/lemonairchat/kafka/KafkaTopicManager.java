package com.hanghae.lemonairchat.kafka;

import java.util.Collections;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaTopicManager {

    private final AdminClient adminClient;

    @Value("${spring.kafka.admin-client}")
    private String adminClientHost;
    public KafkaTopicManager() {
        Properties properties = new Properties();
        properties.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, adminClientHost);

        this.adminClient = AdminClient.create(properties);
    }

    public Mono<Void> createTopic(String roomId, int partitions, short replicationFactor) {
        return Mono.create(sink -> adminClient.listTopics().names().whenComplete((names, ex) -> {
            log.info("토픽 매니저");
            if (ex != null) {
                sink.error(new RuntimeException(ex.getMessage()));
                return;
            }

            if (names.contains(roomId)) {
                sink.success();
            } else {
                NewTopic newTopic = new NewTopic(roomId, partitions, replicationFactor);
                adminClient.createTopics(Collections.singleton(newTopic)).all().whenComplete((result, createEx) -> {
                    if (createEx != null) {
                        sink.error(new RuntimeException(createEx.getMessage()));
                    } else {
                        sink.success();
                    }
                });
            }
        }));
    }
}
