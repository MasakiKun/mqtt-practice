package kr.ayukawa.mqttpractice;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.time.LocalDateTime;
import java.util.UUID;

public class MqttSubscribe implements Runnable {
	private final String topic = "hello/world/message";
	final int qos = 2;
	final String broker = "tcp://192.168.5.129:1883";
	final MemoryPersistence persistence = new MemoryPersistence();

	final String msgPrefix = "[SUB] ";

	@Override
	public void run() {
		try {
			MqttClient client = new MqttClient(this.broker, UUID.randomUUID().toString(), persistence);
			MqttConnectOptions options = new MqttConnectOptions();
			options.setCleanSession(true);
			options.setUserName("mqttuser");
			options.setPassword("1q2w3e".toCharArray());
			showMsg("Connecting to broker: " + this.broker);
			client.setCallback(new MqttSubscribeCallback());
			client.connect(options);
			showMsg("connected");
			showMsg("topic \"" + topic + "\" subscribing...");
			client.subscribe(topic);
			showMsg("subscribe");

			LocalDateTime tenSecAfter = LocalDateTime.now().plusSeconds(10);
			while(tenSecAfter.isAfter(LocalDateTime.now())) {
				Thread.sleep(100);
			}

			client.disconnect();
			showMsg("disconnected");
		} catch(MqttException e) {
			showMsg("reason: " + e.getReasonCode());
			showMsg("msg: " + e.getMessage());
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void showMsg(String msg) {
		System.out.println(this.msgPrefix + msg);
	}
}

class MqttSubscribeCallback implements MqttCallback {
	private MqttClient client;
	private String msgPrefix = "[SUB] ";
	@Override
	public void connectionLost(Throwable cause) {}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		String recvMsg = new String(message.getPayload());
		String msg = String.format("%s Receive content from topic \"%s\": payload \"%s\"", this.msgPrefix.trim(), topic, recvMsg);
		System.out.println(msg);
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {}
}