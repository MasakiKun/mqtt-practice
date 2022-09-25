package kr.ayukawa.mqttpractice.config;

import lombok.Getter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
//@ConfigurationProperties(prefix="mqtt")
//@ConstructorBinding
@ToString
public class MqttConnectInfo {

	@Getter private final String host;
	@Getter private final  int port;
	@Getter private final String username;
	@Getter private final String password;
	@Getter private final int qos;
	@Getter private final String brokerUri;

	@Autowired
	public MqttConnectInfo(
			@Value("${mqtt.host}") final String host,
			@Value("${mqtt.port}") final int port,
			@Value("${mqtt.username}") final String username,
			@Value("${mqtt.qos}") final int qos,
			@Value("${mqtt.password}") final String password
	) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.qos = qos;
		this.brokerUri = String.format("tcp://%s:%d", this.host, this.port);
	}
}
