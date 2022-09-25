package kr.ayukawa.mqttpractice.config.router;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;

public class EachMqttTopicNameRouter extends AbstractMessageRouter {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final ApplicationContext applicationContext;

	@Override
	protected Collection<MessageChannel> determineTargetChannels(Message<?> message) {
		// 토픽명
		final String topicName = (String)message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);

		// 수신 채널 bean 이름
		final String channelBeanName = getChannelBeanName(topicName);
		// 수신 채널 핸들러 bean 이름
		final String handlerBeanName = getHandlerBeanName(topicName);

		// 수신된 토픽을 전달할 채널이 생성되어 있는지 확인한다
		MessageChannel channel = null;
		try {
			channel = applicationContext.getBean(channelBeanName, MessageChannel.class);
		} catch(NoSuchBeanDefinitionException e) {
			logger.warn("{} channel is not created", channelBeanName);
		}

		// 채널이 생성되어 있지 않다면, 토픽을 전달할 채널 및 처리할 핸들러를 생성하고 bean으로 등록한다
		// 1. 채널 생성
		// 2. 채널을 bean으로 등록
		// 3. 채널이 데이터를 전달할 핸들러 생성
		// 4. 핸들러를 bean으로 등록
		// 5. 핸들러를 새로운 스레드로 구동
		if(channel == null) {
			ConfigurableListableBeanFactory beanFactory = getApplicationBeanFactory();
			// 1)
			MessageChannel newChannel = RouterUtils.createChannel();
			// 2)
			beanFactory.registerSingleton(channelBeanName, newChannel);
			MessageChannel channelBean = applicationContext.getBean(channelBeanName, MessageChannel.class);
			// 3)
			Runnable newHandler = RouterUtils.createHandler((QueueChannel)channelBean);
			// 4)
			beanFactory.registerSingleton(handlerBeanName, newHandler);
			Runnable handlerBean = applicationContext.getBean(handlerBeanName, Runnable.class);

			// 5)
			new Thread(handlerBean).start();

			channel = channelBean;
		}

		return Collections.singleton(channel);
	}

	/**
	 * 수신 채널의 bean 이름을 반환한다
	 * @param topicName
	 * @return
	 */
	private String getChannelBeanName(final String topicName) {
		return String.format("MQTT topic %s channel", topicName);
	}

	/**
	 * 수신 채널을 처리할 핸들러의 bean 이름을 반환한다
	 * @param topicName
	 * @return
	 */
	private String getHandlerBeanName(final String topicName) {
		return String.format("MQTT topic %s handler", topicName);
	}

	private ConfigurableListableBeanFactory getApplicationBeanFactory() {
		GenericApplicationContext ctx = ((GenericApplicationContext)applicationContext);
		ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();

		return beanFactory;
	}

	public EachMqttTopicNameRouter(final ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}
}
