package com.hanghae.lemonairchat.kafka;

import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTopicManager {

	@Value("${spring.kafka.admin-client}")
	private String adminClientHost;

	private AdminClient adminClient;

	@PostConstruct
	public void initilize() {
		Properties properties = new Properties();
		properties.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, adminClientHost);
		this.adminClient = AdminClient.create(properties);
	}

	public Mono<Void> createTopic(String roomId, int partitions, short replicationFactor) {
		return Mono.create(sink -> adminClient.listTopics().names().whenComplete((names, ex) -> {
			if (ex != null) {
				sink.error(new RuntimeException(ex.getMessage()));
				return;
			}
			if (names.contains(roomId)) {
				sink.success();
			} else {
				NewTopic newTopic = new NewTopic(roomId, partitions, replicationFactor);
				log.info(" {} 라는 새로운 토픽 생성 : ", roomId);
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
