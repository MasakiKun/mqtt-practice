package kr.ayukawa.mqttpractice;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.UUID;

public class MqttPublish implements Runnable {
	private final String topic = "hello/world/message";
	final int qos = 2;
	final String broker = "tcp://192.168.5.129:1883";
	final MemoryPersistence persistence = new MemoryPersistence();

	final String msgPrefix = "[PUB] ";

	@Override
	public void run() {
		try {
			MqttClient client = new MqttClient(this.broker, UUID.randomUUID().toString(), persistence);
			MqttConnectOptions options = new MqttConnectOptions();
			options.setCleanSession(true);
			options.setUserName("mqttuser");
			options.setPassword("1q2w3e".toCharArray());
			showMsg("Connecting to broker: " + this.broker);
			client.connect(options);
			showMsg("connected");
			for(int i = 0; i < 3; i++) {
				String contents = UUID.randomUUID().toString();
				showMsg("Publishing content: " + contents);
				MqttMessage message = new MqttMessage(contents.getBytes());
				message.setQos(this.qos);
				client.publish(topic, message);
				Thread.sleep(1000);
			}
			client.disconnect();
			showMsg("Disconnected");
		} catch (MqttException e) {
			showMsg("reason: " + e.getReasonCode());
			showMsg("msg: " + e.getMessage());
			e.printStackTrace();
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void showMsg(String msg) {
		System.out.println(this.msgPrefix + msg);
	}
}
