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

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;

@SpringBootApplication
public class MqttMultipleThreadApplication {

	public static void main(String[] args) {
		SpringApplication.run(MethodHandles.lookup().lookupClass(), args);
	}

	@Bean
	public ApplicationRunner runner(ApplicationContext ctx) {
		return new ApplicationRunner() {
			@Autowired
			private MqttConfig.MqttSendGateway gateway;

			@Override
			public void run(ApplicationArguments args) throws Exception {
				System.out.println("***** application started... *****");

				System.out.println("5 sec wait...");

				final String topic1 = "hello/world/1sttopic";
				final String topic2 = "hello/world/2ndtopic";

				Thread.sleep(5000);

				gateway.sendStringDataToMqtt(LocalDateTime.now().toString(), topic1);

				System.out.println("message sent to 1st topic, and 1.5 sec wait...");

				Thread.sleep(1500);

				gateway.sendStringDataToMqtt(LocalDateTime.now().toString(), topic2);

				System.out.println("message send to 2nd topic, and 1.5 sec wait...");

				gateway.sendStringDataToMqtt(LocalDateTime.now().toString(), topic1);
				gateway.sendStringDataToMqtt(LocalDateTime.now().toString(), topic2);
				System.out.println("message send to 1st topic and 2nd topic");

				Thread.sleep(1000);

				System.out.println("***** application end... *****");

				System.exit(0);
			}
		};
	}
}
