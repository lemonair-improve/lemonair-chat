//package com.hanghae.lemonairchat.kafka;
//
//import com.hanghae.lemonairchat.entity.Chat;
//import java.time.Duration;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.springframework.boot.context.event.ApplicationStartedEvent;
//import org.springframework.context.event.EventListener;
//import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
//import reactor.core.publisher.Flux;
//
//@Slf4j
//public class KafkaListener {
//    private final ReactiveKafkaConsumerTemplate<String, Chat> reactiveKafkaConsumerTemplate;
//
//
//    public KafkaListener(ReactiveKafkaConsumerTemplate<String, Chat> reactiveKafkaConsumerTemplate) {
//        this.reactiveKafkaConsumerTemplate = reactiveKafkaConsumerTemplate;
//    }
//
//
//    @EventListener(ApplicationStartedEvent.class)
//    public Flux<Chat> startKafkaConsumer() {
//        return reactiveKafkaConsumerTemplate
//            .receiveAutoAck()
//            .delayElements(Duration.ofSeconds(1L)) // BACKPRESSURE
//            .doOnNext(consumerRecord -> log.info("received key={}, value={} from topic={}, offset={}",
//                consumerRecord.key(),
//                consumerRecord.value(),
//                consumerRecord.topic(),
//                consumerRecord.offset())
//            )
//            .map(ConsumerRecord<String, Chat>::value)
//            .doOnNext(chat -> log.info("successfully consumed {}", chat))
//            .doOnError(throwable -> log.error("something bad happened while consuming : {}", throwable.getMessage()));
//    }
//}
