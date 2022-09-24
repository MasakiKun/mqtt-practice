package kr.ayukawa.mqttpractice;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MqttSampleApplication {
	public static void main(String[] args) {
		SpringApplication.run(MqttSampleApplication.class, args);
	}

	@Bean
	public ApplicationRunner runner(ApplicationContext ctx) {
		return new ApplicationRunner() {
			@Override
			public void run(ApplicationArguments args) throws Exception {
				System.out.println("***** Application started *****");
			}
		};
	}
}
