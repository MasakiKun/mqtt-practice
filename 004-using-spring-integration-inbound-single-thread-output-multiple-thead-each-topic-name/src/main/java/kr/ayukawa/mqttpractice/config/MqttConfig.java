package kr.ayukawa.mqttpractice.config;

import kr.ayukawa.mqttpractice.config.router.EachMqttTopicNameRouter;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.*;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.handler.annotation.Header;

import java.lang.invoke.MethodHandles;
import java.util.UUID;

@Configuration
@EnableIntegration
@IntegrationComponentScan
public class MqttConfig {

	@Autowired
	private MqttConnectInfo mqttInfo;

	@Autowired
	private ApplicationContext appCtx;

	@Value("${mqtt.topic.root}")
	private String rootTopic;

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private MqttConnectOptions connectOptions() {
		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(false);
		options.setServerURIs(new String[] {
				mqttInfo.getBrokerUri()
		});
		options.setUserName(mqttInfo.getUsername());
		options.setPassword(mqttInfo.getPassword().toCharArray());
		return options;
	}

	private MqttPahoClientFactory mqttClientFactory() {
		DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
		factory.setConnectionOptions(connectOptions());
		factory.setPersistence(new MemoryPersistence());
		return factory;
	}

	/***** input channel 설정 (MQTT -> application) *****/

	@Bean
	public MessageChannel inputChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageProducerSupport mqttInboundChannel() {
		MqttPahoMessageDrivenChannelAdapter channelAdapter =
				new MqttPahoMessageDrivenChannelAdapter(UUID.randomUUID().toString(), mqttClientFactory(), rootTopic);
		channelAdapter.setCompletionTimeout(5000);
		channelAdapter.setConverter(new DefaultPahoMessageConverter());
		channelAdapter.setQos(mqttInfo.getQos());
		channelAdapter.setOutputChannel(inputChannel());
		channelAdapter.setRecoveryInterval(1000);
		return channelAdapter;
	}

	@ServiceActivator
	public MessageHandler inboundMessageHandlerWithSingleThread() {
		return message -> {
			String topic = (String)message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
			String msg = (String)message.getPayload();

			logger.info("message \"{}\" received from topic <{}>", msg, topic);
		};
	}

	@Bean
//	@Router
	public AbstractMessageRouter eachMqttTopicNameRouter() {
		return new EachMqttTopicNameRouter(appCtx);
	}

	@Bean
	public IntegrationFlow mqttInboundFlow() {
		return IntegrationFlows
				.from(mqttInboundChannel())
				.handle(eachMqttTopicNameRouter())
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

	@MessagingGateway
	public interface MqttSendGateway {
		@Gateway(requestChannel="outputChannel")
		void sendStringDataToMqtt(String data, @Header(MqttHeaders.TOPIC) String topic);
	}
}
