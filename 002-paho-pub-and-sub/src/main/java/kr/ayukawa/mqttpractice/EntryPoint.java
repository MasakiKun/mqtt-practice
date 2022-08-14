package kr.ayukawa.mqttpractice;

public class EntryPoint {
	public static void main(String[] args) throws Exception {
		Thread pub = new Thread(new MqttPublish());
		Thread sub = new Thread(new MqttSubscribe());

		pub.start();
		sub.start();

		Thread.sleep(15000);

		System.out.println("End");
	}
}
