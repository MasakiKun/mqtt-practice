package kr.ayukawa.mqttpractice.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.handler.annotation.Header;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.UUID;

/*
1.
   Spring Boot에서 Spring Integration을 사용하기 위해서는 @EnableIntegration 어노테이션을 @Configuration 클래스에 붙인다.
   이 어노테이션은 Spring Integration을 사용하기 위해 미리 준비되어야 하는 필수 빈을 자동으로 등록해준다.
2.
   @MessagingGateway 어노테이션을 사용하기 위해서는 @IntegrationComponentScan을, 해당 @MessageGateway 어노테이션을 사용중인
   @Configuration 클래스에 붙이거나, 해당 클래스를 @IntegrationComponentScan 어노테이션에 인자로 지정한다.
 */
@Configuration
@EnableIntegration
@IntegrationComponentScan
public class MqttConfig {

	// MQTT Connection options...
	private static final String[] MQTT_BROKER_URLS = {
			"tcp://192.168.5.129:1883"
	};
	private static final String MQTT_USERNAME = "mqttuser";
	private static final String MQTT_PASSWORD = "1q2w3e";
	private static final int MQTT_QoS = 2;
	private static final String MQTT_TOPIC_NAME = "hello/world/message";

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Bean
	public DefaultMqttPahoClientFactory mqttClientFactory() {
		MqttConnectOptions options = new MqttConnectOptions();
		/*
		속성명과는 의미가 좀 다르지만(...) 클라이언트와 브로커 연결시 이 상태 정보의 기억 여부를 지정한다.
		이 값이 true인 경우, 재연결시 기존 연결을 파기하고 새로운 세션으로 연결한다.
		이 경우, 현재 세션의 구독이 영구히 유지되지 않기 때문에
		QoS 1 이상에서 지원하는 "최소한 한번은 송신/수신"한다는 옵션을 유지할 수 없게 된다.
		이 값이 false인 경우, 재연결시 기존 연결 상태를 유지한 채로 세션을 연결한다.
		이 경우, 현재 상태 세션의 구독이 영구히 유지됨을 의미하므로
		통신 상태 불안정 등의 이유로 브로커와 재연결시에도 QoS 1 이상의 전송을 보장할 수 있게 된다.
		 */
		options.setCleanSession(false);
		options.setServerURIs(MQTT_BROKER_URLS);
		options.setUserName(MQTT_USERNAME);
		options.setPassword(MQTT_PASSWORD.toCharArray());

		DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
		factory.setConnectionOptions(options);
		factory.setPersistence(new MemoryPersistence());
		return factory;
	}

	/*** input channel 설정 (MQTT -> application) ***/

	@Bean
	public MessageChannel inputChannel() {
		return new DirectChannel();		// 기본 채널로 지정
	}

	@Bean
	public MessageProducerSupport mqttInboundChannel() {
		MqttPahoMessageDrivenChannelAdapter channelAdapter =
				new MqttPahoMessageDrivenChannelAdapter(UUID.randomUUID().toString(), mqttClientFactory(), MQTT_TOPIC_NAME);
		channelAdapter.setCompletionTimeout(5000);
		channelAdapter.setConverter(new DefaultPahoMessageConverter());
		channelAdapter.setQos(MQTT_QoS);
		/*
		속성명은 outputChannel이지만, 이 MQTT adapter에서 데이터를 전달할 채널을 지정하는 것이므로 inputChannel을 지정한다.
		inputChannel이 어플리케이션 입장에서는 inputChannel이지만,
		MQTT adapter 입장에서는 outputChannel인 것.
		 */
		channelAdapter.setOutputChannel(inputChannel());
		channelAdapter.setRecoveryInterval(1000);

		return channelAdapter;
	}

	@ServiceActivator
	public MessageHandler inboundMessageHandler() {
		return message -> {
			String topic = (String)message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
			String msg = (String)message.getPayload();

			logger.info("message \"{}\" received from topic <{}>", msg, topic);
		};
	}

	/*
	 * MQTT input channel flow
	 *
	 * MQTT -> mqttInboundChannel (-> inputChannel) -> inboundMessageHandler(= application)
	 */
	@Bean
	public IntegrationFlow mqttInboundFlow() {
		return IntegrationFlows
				.from(mqttInboundChannel())
				.handle(inboundMessageHandler())
				.get();
	}

	/*** output channel 설정 (application -> MQTT) ***/
	@Bean
	public MessageChannel outputChannel() {
		return new DirectChannel();
	}

	@ServiceActivator
	public MessageHandler mqttOutboundChannel() {
		MqttPahoMessageHandler handler = new MqttPahoMessageHandler(UUID.randomUUID().toString(), mqttClientFactory());
		handler.setAsync(false);
		return handler;
	}

	/*
	 * MQTT output channel flow
	 *
	 * application -> outputChannel -> inboundMessageHandler -> MQTT
	 */
	@Bean
	public IntegrationFlow mqttOutputFlow() {
		return IntegrationFlows
				.from(outputChannel())
				.handle(mqttOutboundChannel())
				.get();
	}

	/*
	※ Spring Integration을 사용하는 application에서,
	외부 시스템으로의 데이터 전송 로직에서 application과 Spring Integration과의 의존성을 끊고 싶을때
	 @MessagingGateway 어노테이션과 @Gateway 어노테이션을 사용한다.

	 본래 Spring Integration을 통해 외부 이기종 시스템과 통신하기 위해서는, 외부 시스템과 접근하기 위한
	 MessageChannel의 구현체와, MessageChannel을 통해 전송할 데이터를 캡슐화한 Message 인스턴스를 필수적으로 이용해야 한다.
	 (MqttSampleApplication의 ApplicationRunner에서 예제 확인)

	 @MessagingGateway 어노테이션이 붙은 인터페이스의 @Gateway 어노테이션이 붙은 메서드는
	 Spring Integration이 @Gateway 어노테이션의 requestChannel 까지 흐르는 flow를 확인해서
	 자동으로 구현체를 만들어준다.
	 이 구현체에는 Message 인스턴스를 생성하는 부분과 이 Message를 MessageChannel로 전달하는 로직을 캡슐화하므로,
	 비즈니스 로직에서는 Spring Integration의 컴포넌트인 MessageChannel이나 Message 인스턴스와의 의존성을 갖지 않게 된다.

	 단, MessagingGateway 어노테이션을 사용하기 위해서는 @IntegrationComponentScan 어노테이션을 통해
	 MessagingGateway 클래스의 위치를 지정해야 한다.
	 */
	@MessagingGateway
	public interface MqttSendGateway {
		@Gateway(requestChannel="outputChannel")
		void sendStringDataToMqtt(String data, @Header(MqttHeaders.TOPIC) String topic);
	}

}
