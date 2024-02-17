package com.benjsoft.org.springcloudstreamdemo;

import com.azure.spring.messaging.checkpoint.Checkpointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.azure.spring.messaging.AzureHeaders.CHECKPOINTER;

@SpringBootApplication
@EnableScheduling
public class SpringCloudStreamDemoApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringCloudStreamDemoApplication.class);
    private static final Sinks.Many<Message<String>> sinks = Sinks.many().unicast().onBackpressureBuffer();

    public static void main(String[] args) {
        SpringApplication.run(SpringCloudStreamDemoApplication.class, args);
    }

    @Bean
    public Consumer<Message<String>> consume() {
        return message -> {
            Checkpointer checkpointer = (Checkpointer) message.getHeaders().get(CHECKPOINTER);
            assert checkpointer != null;
            checkpointer.success()
                    .doOnSuccess(s -> {
                        LOGGER.info("Message received: {}", message);
                        sinks.emitNext(MessageBuilder.withPayload(message.getPayload()).build(),
                                Sinks.EmitFailureHandler.FAIL_FAST);
                    })
                    .doOnError(e -> LOGGER.error("Error receiving message!"))
                    .block();
        };
    }

    @Bean
    public Supplier<Flux<Message<String>>> supply() {
        return () -> sinks.asFlux()
                .doOnNext(m -> LOGGER.info("Sending message: {}", m))
                .doOnError(e -> LOGGER.error("Error sending message!"));
    }

    @Bean
    public Supplier<Flux<Message<String>>> supplyOrder() {
        return () -> sinks.asFlux()
                .doOnNext(m -> LOGGER.info("Sending order: {}", m))
                .doOnError(e -> LOGGER.error("Error sending order!"));
    }

    @Autowired
    private StreamBridge streamBridge;

    @Scheduled(fixedRate = 60000)
    public void sendRandomOrder() {
        for (int i = 0; i < 5; i++) {
            Message<String> message = MessageBuilder.withPayload("Hello from StreamBridge: " + i).build();
            streamBridge.send("supply-out-1", message);
        }
    }
}
