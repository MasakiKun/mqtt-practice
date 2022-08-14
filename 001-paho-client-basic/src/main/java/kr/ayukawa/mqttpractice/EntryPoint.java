package kr.ayukawa.mqttpractice;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.UUID;

public class EntryPoint {
	public static void main(String[] args) {
		final String topicName = "hello/world/message";
		final int qos = 2;
		final String broker = "tcp://192.168.5.129:1883";
		MemoryPersistence persistence = new MemoryPersistence();

		try {
			MqttClient client = new MqttClient(broker, UUID.randomUUID().toString(), persistence);
			MqttConnectOptions options = new MqttConnectOptions();
			options.setUserName("mqttuser");
			options.setPassword("1q2w3e".toCharArray());
			options.setCleanSession(true);
			System.out.println("Connecting to broker: " + broker);
			client.connect(options);
			System.out.println("connected");
			String msg = UUID.randomUUID().toString();
			System.out.println("Published message: " + msg);
			MqttMessage message = new MqttMessage(msg.getBytes());
			message.setQos(qos);
			client.publish(topicName, message);
			client.disconnect();
			System.out.println("disconnected");
			System.exit(0);
		} catch(MqttException e) {
			System.out.println("reason: " + e.getReasonCode());
			System.out.println("msg: " + e.getMessage());
			System.out.println("loc: " + e.getLocalizedMessage());
			System.out.println("cause: " + e.getCause());
			e.printStackTrace();
		}
	}
}
