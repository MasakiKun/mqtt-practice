package kr.ayukawa.mqttpractice.config.router;

import kr.ayukawa.mqttpractice.config.handler.DefaultMessageHandler;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.MessageChannel;

/**
 * MQTT 토픽명 별 다중 스레드 처리를 위해서, 필요시 채널과 핸들러를 생성해주는 클래스
 */
public class RouterUtils {

	public static MessageChannel createChannel() {
		QueueChannel channel = new QueueChannel();
		return channel;
	}

	public static Runnable createHandler(final QueueChannel channel) {
		final Runnable handler = new DefaultMessageHandler(channel);
		return handler;
	}

	private RouterUtils() {}
}
