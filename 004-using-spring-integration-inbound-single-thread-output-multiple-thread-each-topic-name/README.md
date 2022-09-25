# Spring Integration + MQTT 환경에서, MQTT 토픽명에 따라 별도의 스레드로 처리하기

이 프로젝트 코드는 와일드 카드를 사용해서 MQTT 데이터를 수신하고 있는 Spring Integration + MQTT 프로젝트에서
토픽명에 따라 별도의 스레드를 갖도록 구성된 예제 코드이다.

> 주의: 이 코드는 프로덕션 환경에 적용된 적이 없는 코드이므로, 개인 책임 하에 사용하시기 바랍니다.

## issue

현행 시스템(편의상 IoT 모니터링 프로젝트로 호칭)은 IoT 장비가 주기적으로 자신의 상태를 MQTT로 전송하면(편의상 주기보고로 호칭),
MQTT 전체 토픽을 구독하고 있는 모니터링 시스템이 이 주기보고 데이터를 수신해서 장비의 상태를 파악하고, 문제 발생 감지시 대시보드에 알람을 표시하는 구성으로 되어 있다.

![](https://user-images.githubusercontent.com/12710869/192129955-87ef70a4-571e-497a-b444-e4720075455a.png)

처음에는 잘 동작하는 시스템이었으나, 프로젝트를 조금 오래 개발하기 시작하니 시스템에 문제가 생겼다.

개발을 진행하는 과정에서 아래의 2가지가 계속해서 진행되었다.

1. IoT 장비가 하나둘씩 계속 늘어남
2. IoT 모니터에서 IoT 장비에 대한 요구사항이 점점 늘어나면서, 메시지 핸들러가 점점 무거워짐

결국 2에 의해서 메시지 핸들러가 점점 무거워지고 처리 시간도 길어지는데, 1에 의해서 그 트래픽도 감당할 수 없을만큼 늘어나서,
모니터가 이 트래픽을 다 해소하지 못하는 상황이 오게 되었다.

![](https://user-images.githubusercontent.com/12710869/192130886-19bc96f9-2b42-4b13-91db-cb0e16b25065.png)

~~미친 얘기 같지만 전부 사실이에요~~

IoT 장비 여러대에서 이상 발생이 표시되어 살펴보니, 메시지 핸들러의 처리시간이 길어짐에 따라 MQTT로 전달된 메시지를 제때 처리하지 못하게 되었다.
그래서 IoT 모니터가 주기보고를 수신했을 때 직전에 수신했다고 읽어들인 주기보고는 수십초 이전에 수신한 데이터였으며,
IoT 모니터는 그 수십초 동안은 IoT 장비가 주기보고를 전달하지 않은 이상상태인 것으로 인지하고,
이렇게 모든 장비들의 메시지 처리가 늦어지면서 결국 모든 장비들을 이상으로 인지한 것이 문제였다.

1. IoT 모니터의 메시지 핸들러가 무거워져서, IoT 장비의 주기보고 메시지 처리할 때마다 1초 이상이 걸림.
이 메시지 핸들러는 직전 주기보고 데이터와 수신된 주기보고 데이터와 비교해서 이상 상태를 비교하는 로직이 있는데, 
이를 위해서 주기보고 데이터를 임시로 외부에 저장하는 로직도 들어있음
2. IoT 장비들의 수가 늘어남에 따라, 주기보고 양이 엄청나게 늘어남
3. 결국 IoT 모니터에서 MQTT의 주기보고 데이터를 수신하고, 장비의 상태를 비교/확인하기 위해
1에서 저장한 직전 주기보고 데이터를 조회하면 이 데이터는 수십초 이전의 데이터이므로
IoT 모니터는 수십초 동안 주기보고가 전달되지 않은걸로 인지하고 장비 이상 알림을 표시함.

이 시기의 나는 해당 프로젝트에서는 빠져서 이 이슈를 내가 수정하지는 않았는데, 메시지 핸들러를 별도의 클래스로 빼고 MQTT에서 메시지를 수신할 때마다
메시지 핸들러를 그때그때 신규 스레드로 띄워서 처리하는 식으로 이슈를 해결했다고 들었다.

그러나 이런 방법으로는 주기보고 데이터가 MQTT로 송신되었을 때, 이 데이터들이 송신 순서대로 처리된다는 보장이 없어지게 된다. 최악의 경우, 먼저 수신된 주기보고 데이터가
아직 핸들러에서 처리중임에도 불구하고 나중에 수신된 주기보고 데이터도 동시에 처리되기 시작하여, 나중에 수신된 주기보고 데이터의 처리가 먼저 끝나게 될 수도 있다.
이렇게 될 경우, 주기보고 데이터의 순서가 올바르다고 가정하고 개발된 기능들은 모두 무용지물이 되어, 해당 기능을 폐기하거나 별도 개발해야 할 것이다.

(나중애 듣기론, 실제로 주기보고 데이터의 수신 순서대로 판단하는 로직은 모두 제거했다고 들었음)

## MQTT 토픽명 별 메시지 핸들러

이 IoT 모니터링 시스템은 와일드 카드가 포함된 토픽을 구독하고 있었다. 또한, 각 IoT 장비들은 시스템에 의해 고유의 장비 번호를 부여받게 되어 있다.
그에 따라 루트 토픽명이 ```iotmon```이라고 가정하면, 각 장비들은 ```iotmon/{장비고유번호}``` 토픽으로 주기보고를 전송하게 되어 있었다.

기존에는 아예 주기보고 수신시마다 메시지 핸들러 스레드를 띄웠다고 했으니, 타협점으로 토픽명 별로 핸들러 스레드를 할당해주면 IoT 모니터의 처리시간이 길어져서
MQTT에 데이터가 쌓이는 문제도 해소하고 주기보고 수신의 순서를 보장하지 못하는 이슈도 해소할 수 있지 않을까 생각했다.

즉, ```iotmon/001``` 토픽과 ```iotmon/002``` 토픽에 명시적으로 별도의 스레드를 할당해주면 되지 않을까 생각하게 되었다.

### 메시지 채널 변경

먼저 해야 할 일은 메시지 채널을 변경하는 일이다. Spring Integration의 기본 채널은 ```DirectChannel```인데, 이 채널의
[javadoc 문서](https://docs.spring.io/spring-integration/api/org/springframework/integration/channel/DirectChannel.html)를
보면 다음과 같이 적혀있다.

> A channel that invokes a single subscriber for each sent Message. The invocation will occur in the sender's thread.
> 
> 전송된 각 메시지를 단일 구독자에게 전달하는 채널입니다. 메시지 전달은 메시지 송신측의 스레드에서 발생합니다.

즉, 아래와 같은 구성으로 개발되었을 때

![](https://user-images.githubusercontent.com/12710869/192131754-82e0d4a7-ff80-472e-9d21-3077789c44d4.png)

DirectChannel의 특성상 그 다음의 end-point는 ~~때려죽여도~~ 멀티스레드로 처리할 수는 없다는 의미가 된다.

그렇기 때문에, 중간에 라우터를 둬서 아래와 같은 구성으로 간다 하더라도

![](https://user-images.githubusercontent.com/12710869/192131865-4b407d58-40d3-43f8-9237-e838de7cfd35.png)

상단의 DirectChannel과 하단의 end-point까지가 모두 하나의 스레드로 묶이게 되므로, 최초에 메시지가 ```end-point #1```로 도달한 시점에서
```end-point #1```의 처리가 완료될 때까지 상단의 Direct Channel이 함께 멈추게 된다.

물론 Spring Integration에는 DirectChannel처럼 구독형 채널(후술)의 성질을 가지면서도, 메시지 수신시 이후 처리를 별도의 스레드(정확히는 Executor)에게 위임하는, 
[ExecutorChannel](https://docs.spring.io/spring-integration/api/org/springframework/integration/channel/ExecutorChannel.html)이
있기는 하다. 하지만 이 채널은 메시지 수신시마다 별도의 스레드가 생성된 후 메시지가 전달되기 때문에, 맨 처음에 언급한 접근법과 동일한 결과를 보여주게 될 것이다.
~~사실 아직 안써봄~~

이 이슈를 해소하기 위해서는, 메시지를 수신하면 스스로 다른 끝점으로 전달하는(그 덕에 메시지 수신시부터 끝점의 처리가 끝날때까지 단일 스레드 처리가 강제되는)
DirectChannel이 아닌, 외부에서 직접 메시지를 가져가도록 구성된 QueueChannel을 사용할 필요가 있다.

DirectChannel과 QueueChannel은 채널의 종류 자체가 근본적으로 다른데, DirectChannel은 구독형 채널로서 메시지가 수신되면 채널 자기 자신에게 가입한 메시지 핸들러에게
수신한 메시지를 전달해주는 반면, QueueChannel은 폴러블(Pollable) 채널로서 메시지가 수신되면 채널 내부에 메시지를 저장해두고 이 메시지를 외부의 핸들러가 가져갈
때까지 메시지를 저장해둔다.

간략하게 의사 코드로 나타내보면, DirectChannel은

```java
// 선언시
DirectChannel channel = new DirectChannel();
channel.subscribe(messageHandler);    // 바로 이 부분이 Spring Integration 어노테이션이 대신 해주는 부분

// 메시지 수신시
channel.send(message);
// 이렇게 호출될 경우, channel 내부에서 messageHandler.handleMessage(message) 를 호출해서 메시지를 직접 전달해준다.
```

이렇게 DirectChannel이 능동적으로 메시지 핸들러를 호출해서 전달하는데 반해, QueueChannel은

```java
// 선언시
QueueChannel channel = new QueueChannel();
// DirectChannel과는 달리, 메시지 핸들러를 구독하지 않는다. DirectChannel이라면 이 경우, send가 호출될 경우 예외가 발생한다.

// 메시지 수신시
channel.send(message);
// 이렇게 메시지가 수신될 경우, channel 내부의 큐에 메시지를 저장한다. 이후 별도의 핸들러에서 receive() 메서드를 호출해서 Queue의 내용을 조회한다.

class Handler implements Runnable {
    @Override
    public void run() {
        while(true) {
            Message<?> message = channel.receive();
            if(message != null) {
                // 메시지 처리
            }
        }
    }
}

new Thread(new Handler).start();  // 메시지 핸들러는 별도의 핸들러에서 구동한다
```

위와 같이, 메시지 수신시에는 메시지를 저장만 할 뿐 아무것도 안하고 있다가(응?) 외부에서 데이터를 가져갈 때 메시지를 반환만 해주는 특성이 있다.

### 라우터 작성

Spring Integration에는 메시지 수신시, 지정된 특정 조건에 따라 다른 채널로의 분기를 지원하는 라우터가 있다.

이 라우터를 통해 토픽에 맞는 채널로 분기를 하도록 짜되, 만약 토픽에 맞는 채널이 생성되어 있지 않다면 채널을 새로 생성해서 스프링 빈으로 등록한다.

```java
// kr.ayukawa.mqttpractice.config.router.EachMqttTopicNameRouter

// package 및 import 생략
// 이 코드는 저장소에 저장된 코드와는 조금 다른데, 코드를 짧게 작성하기 위해 의도적으로 예외처리나 번잡한 반복 코드들을 제외했기 때문임
public class EachMqttTopicNameRouter extends AbstractMessageRouter {
    @Autowired private ApplicationContext ctx; 
    @Override
    protected Collection<MessageChannel> determineTargetChannels(Message<?> message) {
        // 수신 토픽명
        final String topicName = (String)message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        // 이 토픽을 처리할 채널 및 핸들러의 bean 이름
        final String
                channelBeanName = String.format("TOPIC %s channel", topicName),
                handlerBeanName = String.format("TOPIC %s handler", topicName);
        MessageChannel channel = ctx.getBean(channelBeanName);
		
        if(channel == null) {
            /*
             * 토픽을 처리할 채널이 등록되지 않았다면, 채널과 핸들러를 생성해서 빈에 등록하고
             * 핸들러는 별도의 스레드로 띄운다
             */
            // 채널 생성
            MessageChannel newChannel = new QueueChannel();
            // 핸들러 생성
            Runnable handler = new MessageHandler(newChannel);

            // 채널과 핸들러를 스프링 빈으로 등록 생략
            // 핸들러 스레드를 띄운다
            // 이 핸들러 스레드는 IoT 장비가 늘어날 때마다 계속해서 늘어날 것이므로, Executor 를 사용하지 않고 바로 스레드를 띄운다.
            new Thread(handler).start();

            channel = newChannel;
        }

        return Collections.singleton(channel);
    }
}
```

### 라우터 등록

이후에는 위에서 작성한 라우터를 Spring Integration flow로 만들어서 스프링 빈으로 등록한다.

```java
import java.beans.BeanProperty;

@Configuration
@EnableIntegration
public class MqttConfig {
    @Bean
    public MessageChannel inputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducerSuport mqttInboundChannel() {
        // blahblah...
    }
	
    @Bean
    public AbstractMessageRouter router() {
        return new EachMqttTopicNameRouter();
    }
	
    @Bean
    public IntegrationFlow mqttInboundFlow() {
        return IntegrationFlows
                .from(mqttInboundChannel())
                .handle(router())
                .get();
    }
}
```

### 테스트

실제로 별도의 스레드에서 처리가 되는지 확인한다.

ASAP

### unresolved issues

실제로 이렇게 개발을 다 해 놓았으나, 내부에서 이야기해본 결과 이 로직은 사용하지 않고 당분간은 계속 싱글 스레드로 처리하는걸로 이야기 되었다.

그리하야 이 로직은 프로덕션 레벨에서 사용해보지 못하고 파기했는데 ~~흐, 흥! 절대 내 코드가 아까워서 올린건 아니니깐!~~, 덕택에 테스트해보지 못한 몇가지 이슈가 있다.

#### 스프링 개발 의도와 다른 스프링의 사용

스프링은 기본적으로 어플리케이션이 구동될 때, 어플리케이션에 필요한 빈을 모두 등록한 뒤 필요할 때마다 빈을 가져와서 실행하는 것을 원칙으로 삼는다.

또한 스프링은 어플리케이션이 구동되는 동안 동적으로 빈을 추가하는 것을 권장하지 않는걸로 추정된다. ```ApplicationContext```에 빈을 직접 추가할 수 있게 해주는
클래스 타입으로 적절히 캐스팅한 다음 ```refresh()``` 메서드를 호출해서 빈 목록을 재구성하려 할때 예외가 발생하는 것에서 추정할 수 있다.
그렇다면, 이렇게 스프링의 개발 원칙(추정)을 어겨가면서 개발을 할만한 합당한 요소가 있는가 고민해보지 않을 수 없다.

사실 이 코드를 처음 짤때는 Spring Integration의 구성 요소는 당연히 스프링 빈으로 등록해야 하는거 아닐까 하는 생각에, 메세지 채널도 핸들러도 어떻게든(...)
수단과 방법을 가리지 않고(......) 스프링 빈으로 등록했는데, 실제 코드에서는 이 메시지 채널을 생성 후에 다시 가져다 쓰지 않는다. 다시 가져다 쓰지 않는다면, 빈으로
등록하지 말고 어플리케이션 내에서 Map 등을 이용해서 채널과 메시지 핸들러를 직접 관리하면 됬던거 아닌가 싶다. 코드를 파기했기 때문에 이 이상 테스트를 해보지는 않았다.

#### 퍼포먼스 문제

이 코드는 MQTT 토픽의 수만큼 핸들러 스레드를 구동한다. 이 스레드는 어플리케이션이 종료되기 전까지는 계속 무한루프를 처리하며, 종료되지 않는다.

만약 감시해야 하는 IoT 장비가 200대라면? 400대라면? 800대라면? 단위 수를 늘려서 200대라면? 8000대라면? 8000개의 스레드를 과연 서버가 버틸 수 있을까?

#### 메모리 누수 문제

코드 상으로는 별다른 메모리 누수의 요인은 보이지 않지만, 스레드를 계속해서 올리는 이상 메모리 누수의 위협이 없다고는 할 수 없다.

#### 설계 자체에 대한 의심

처음에 토픽명 별로 별도 스레드를 가져가야겠다고 생각하고 이것저것 많이 조새를 했었다.
그러다가 스택오버플로의 [이 댓글](https://stackoverflow.com/questions/57426802/identifying-a-bottleneck-in-a-multithreaded-mqtt-publisher#comment101388085_57426802)을
을 발견했는데, 이런 내용이다. 구글 번역기로 번역한 내용이므로, 내용이 틀릴수도 있다.

    * 댓글1
    (전략) MQTT is not designed for large amount of concurrent in-flight messages.
    Also, do you have the same QoS level in your benchmark and this execution?
    MQTT는 대량의 동시 메시지 전송용으로 설계되지 않았습니다. 또한, 벤치마크와 이 코드에서 동일한 MQTT QoS 수준을 가지고 있습니까?

    * 댓글2
    I really have an increasing doubt that MQTT is the right transport for your use case.
    You have apparently big-ish messages, you have a lot of them, and proof of delivery is essential to you.
    MQTT can satisfy one requirement, but not all of them. It has other design goals.
    Is it your authority to use or propose a real message queue instead (AMQP or other)?
    MQTT가 당신의 사례에 적합한지 의심이 커지고 있습니다. 당신은 분명 큰 메시지를 전송하려 하고 있고, 많은 메시지를 전송하려 하고 있으며,
    전송 증명이 필수인 것으로 보입니다.
    MQTT가 여기서 하나의 요구사항을 충족해줄 수는 었지만, 전체는 아닙니다. (MQTT에는 그것과는 다른) 설계 목표가 있습니다.
    당신은 AMQP나 그 외의 다른 메시지 큐를 사용하도록 제안할 수 있는 권한이 있습니까?

즉, MQTT는 대용량/대량의 메시지를 전송할 목적으로 개발된 시스템은 아니라는 의미이다. 만약 메시지가 최초 상상했던 것 이상으로 대량으로 발생한다면,
아예 MQTT의 설계 사상에 맞춰서 아예 시스템을 분리하는 것이 더 낫지 않았을까?