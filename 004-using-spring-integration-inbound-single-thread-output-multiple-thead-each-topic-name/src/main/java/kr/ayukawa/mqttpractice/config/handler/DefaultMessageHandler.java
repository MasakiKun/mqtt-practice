package kr.ayukawa.mqttpractice.config.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;

import java.lang.invoke.MethodHandles;

public class DefaultMessageHandler implements Runnable {
	private final QueueChannel queueChannel;
	private final int timeout = 500;

	private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public void run() {
		while(true) {
			Message<?> message = queueChannel.receive(timeout);
			if(message != null) {
				handleMessage(message);
			} else {
				try {
					Thread.sleep(timeout);
				} catch(InterruptedException ie) {
					logger.warn("", ie);
				}
			}
		}
	}

	private void handleMessage(Message<?> message) {
		try {
			final String topicName = (String)message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
			final String payload = (String)message.getPayload();
			final String logMsg = String.format("message \"%s\" received on <%s> topic", payload, topicName);

			logger.info(logMsg);
		} catch(Exception e) {
			logger.error("exception raise when message handle", e);
		}
	}

	public DefaultMessageHandler(QueueChannel queueChannel) {
		this.queueChannel = queueChannel;
	}
}
