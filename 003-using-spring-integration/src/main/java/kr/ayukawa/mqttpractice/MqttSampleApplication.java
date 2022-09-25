package kr.ayukawa.mqttpractice;

import kr.ayukawa.mqttpractice.config.MqttConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

@SpringBootApplication
public class MqttSampleApplication {
	public static void main(String[] args) {
		SpringApplication.run(MqttSampleApplication.class, args);
	}

	@Bean
	public ApplicationRunner runner(ApplicationContext ctx) {
		return new ApplicationRunner() {
			@Autowired
			@Qualifier(value="outputChannel")
			private MessageChannel outputChannel;

			@Autowired
			private MqttConfig.MqttSendGateway gateway;

			@Override
			public void run(ApplicationArguments args) throws Exception {
				System.out.println("***** Application started *****");

				Thread.sleep(1500);

				// @MessagingGateway 없이 Spring Integration 기능을 통해 MQTT와 통신
				Message<String> message = MessageBuilder
						.withPayload("Hello, world from Spring Integration application")
						.setHeader(MqttHeaders.TOPIC, "hello/world/message")
						.build();

				outputChannel.send(message, 1000);

				Thread.sleep(1500);

				// @MessageGateway를 이용해서, 비즈니스 로직에서 Spring Integration과의 의존성을 제거한 채 통신
				gateway.sendStringDataToMqtt("Message using MessagingGateway", "hello/world/message");
			}
		};
	}
}
