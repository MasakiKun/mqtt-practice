# Spring Integration + MQTT 환경에서, MQTT 토픽명에 따라 별도의 스레드로 처리하기

이 프로젝트 코드는 MQTT에서 와일드카드 토픽을 구독하고 있는 Spring Integration + MQTT 프로젝트에서,
각 토픽명 별 메시지 핸들러에 별도의 스레드를 할당하여 처리하도록 구성된 예제 코드이다.

> :warning: 주의: 이 코드는 프로덕션 환경에 적용된 적이 없는 코드입니다. 코드를 참고하실때 감안하시기 바랍니다.

## issue

현행 시스템(편의상 IoT 모니터링 시스템으로 호칭)은 IoT 장비가 주기적으로 자신의 상태를 MQTT로 전송(편의상 주기보고로 호칭)하면,
MQTT 전체 토픽을 구독하고 있는 모니터링 시스템이 이 주기보고 데이터를 수신해서 데이터를 저장하고
분석해서 장비의 상태를 파악하며, 장비 이상 감지시 대시보드에 알람을 표시하는 구성으로 되어 있다(대시보드는 별도의 시스템이지만, 이 문서에서는 생략).

![](https://user-images.githubusercontent.com/12710869/192129955-87ef70a4-571e-497a-b444-e4720075455a.png)

처음에는 잘 동작하는 시스템이었으나, 조금 오래 개발하기 시작하니 시스템에 문제가 생겼다.

개발을 진행하는 과정에서 아래의 2가지가 계속해서 진행되었다.

1. IoT 장비가 하나둘씩 계속 늘어남
2. 주기보고 수신시의 요구사항이 점점 늘어나면서, IoT 모니터링 시스템의 메시지 핸들러가 점점 무거워짐

결국 2에 의해서 메시지 핸들러는 점점 처리 시간이 길어지는데, 1에 의해서 장비의 수가 늘어남에 따라 주기보고 데이터의 양이 감당할 수 없을만큼 늘어나서,
모니터가 이 트래픽을 다 해소하지 못하는 상황이 오게 되었다.

![](https://user-images.githubusercontent.com/12710869/192130886-19bc96f9-2b42-4b13-91db-cb0e16b25065.png)

~~미친 얘기 같지만 전부 사실이에요~~

IoT 모니터의 이상상태 감지 로직은, 위에서 언급한 미리 저장해둔 직전 주기보고 데이터를 가져와서 현재 주기보고 데이터의 수신 시간과 비교하는 부분이 있다.
이 비교 결과, 장비의 보고 주기(예: 10초)보다 늦게 주기보고를 전달받은 경우, 장비나 네트워크에 이상이 발생하여 주기보고를 발행하지 못했다고 판단하게 된다.

어느날, 대시보드에서 IoT 장비 여러대에 동시에 이상 발생이 표시되게 되었다.

로그를 살펴보니, IoT 모니터가 주기보고를 수신하자마자 1초 이내에 처리하고 반환되어야 할 메시지 핸들러가 수 초 ~ 수십초 이상 동안 실행되고 있었다.

IoT 모니터가 동일한 장비의 다음 주기보고를 수신했을 때 비교를 위해 조회한 직전 수신 데이터는 수십초 이전에 수신한 데이터였으며,
IoT 모니터는 그 수십초 동안은 IoT 장비가 주기보고를 전달하지 않은 이상상태인 것으로 인지한다.
하지만 메시지 핸들러의 처리가 이것 하나만 있는것은 아니라서 이 외에도 요구사항으로 접수됬던 처리들을 진행하면서 메시지 핸들러의 처리에 또다시 시간이 걸린다.
결국 다음 메시지를 수신했을때는 또다시 메시지 핸들러의 실행시간 만큼 데이터를 수신 및 처리하지 않은 상태가 되어 있었다.

이런 식의 진행이 반복되며, 최종적으로는 모든 장비들의 메시지 처리가 늦어지면서 모든 장비들을 이상으로 인지하게 되었다.

1. IoT 모니터의 메시지 핸들러가 무거워져서, IoT 장비의 주기보고 메시지 처리할 때마다 1초 이상이 걸림.
2. IoT 장비들의 수가 늘어남에 따라, 주기보고 양이 엄청나게 늘어남.
3. 결국 IoT 모니터에서 MQTT의 주기보고 데이터를 수신하고, 장비의 상태를 비교/확인하기 위해
직전 주기보고 데이터를 조회하면 이 데이터는 수십초 이전의 데이터이므로
IoT 모니터는 수십초 동안 주기보고가 전달되지 않은걸로 인지하고 장비 이상 알림을 표시함.

이 시기의 나는 해당 프로젝트에서는 빠져서 이 이슈를 내가 수정하지는 않았는데, 메시지 핸들러를 별도의 클래스로 빼고 MQTT에서 메시지를 수신할 때마다
메시지 핸들러를 그때그때 신규 스레드로 띄워서 처리하는 식으로 이슈를 해결했다고 들었다.

```java
// before...
@ServiceActivator
public MessageHandler inboundMessageHandler() {
    return message -> {
        final String topic = (String)message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        final String msg = (String)message.getPayload();

        System.out.println("message received: " + msg);
    }
}

// after...
// 난 잘 모르겠는데 이렇게 해결했다는듯;
class MessageHandler implements Runnable {
    final private Message<?> message;
	
    public MessageHandler(Message<?> message) {
        this.message = message;
    }
	
    @Override
    public void run() {
        final String topic = (String)message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        final String msg = (String)message.getPayload();

        System.out.println("message received: " + msg);
    }
}

@ServiceActivator
public MessageHandler inboundMessageHandler() {
    return (message) -> {
        new Thread(new MessageHandler(message)).start();
    }
}
```

그러나 이런 방법으로는, 주기보고 데이터가 MQTT로 송신되었을 때 모니터 시스템은 데이터를 수신할 때마다 새로 스레드를 띄우므로 이 데이터들의 순서를 보장하지 못하게 된다.
최악의 경우, 먼저 수신된 주기보고 데이터가 아직 핸들러에서 처리중임에도 불구하고 나중에 수신된 주기보고 데이터도 동시에 처리되기 시작하여,
나중에 수신된 주기보고 데이터의 처리가 먼저 끝나게 될 수도 있다.
이렇게 될 경우, 주기보고 데이터의 순서가 올바르다고 가정하고 개발된 기능들은 모두 무용지물이 되어, 해당 기능을 폐기하거나 별도로 새로 개발해야 할 것이다.

(나중애 듣기론, 그런 기능들은 진짜로 다 제거했다고 들었음-_-)

## MQTT 토픽명 별 메시지 핸들러

이 IoT 모니터링 시스템은 와일드 카드가 포함된 토픽을 구독하고 있다. 또한, 각 IoT 장비들은 시스템에 의해 고유의 장비 번호를 부여받게 되어 있다.
그에 따라 루트 토픽명이 ```iotmon```이라고 가정하면, 각 장비들은 ```iotmon/{장비고유번호}``` 토픽으로 주기보고를 발행하고
IoT 모니터링 시스템은 ```iotmon/#```을 구독하여 수신하고 있었다.

```java
@Bean
public inputChannel() { return new DirectChannel(); }

@Bean
public MessageProducerSupport inboundChannel() {
    MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
        CLIEND_ID,
        mqttClient(),
        "iotmon/#"    /* topic name */
    );
    // blahblah
    return adapter;
}

@ServiceActivator
public MessageHandler inboundMessageHandler() {
    return message -> {
        final String topic = (String)message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        final String msg = (String)message.getPayload();

        System.out.println("message received: " + msg);
    }
}

@Bean
public IntegrationFlow inboundFlow() {
    return IntegrationFlows
        .from(inboundChannel())
        .handle(inboundMessageHandler())
        .get();
}
```

기존에는 아예 주기보고 수신시마다 메시지 핸들러 스레드를 띄웠다고 했으니, 타협점으로
토픽명 별로 핸들러 스레드를 할당해주면 MQTT에 데이터가 쌓이는 문제도 어느정도 해소되고
주기보고 데이터의 수신 순서를 보장하지 못하는 이슈도 해소할 수 있지 않을까 생각했다.

즉, ```iotmon/001``` 토픽과 ```iotmon/002``` 토픽에 명시적으로 별도의 스레드를 할당해주면 되지 않을까 생각하게 되었다.

### 메시지 채널 변경

먼저 해야 할 일은 메시지 채널을 변경하는 일이다. Spring Integration의 기본 채널은 ```DirectChannel```인데, 위 코드를 보면 알겠지만
이 프로젝트 또한 DirectChannel을 사용하고 있었다.

이 채널의
[javadoc 문서](https://docs.spring.io/spring-integration/api/org/springframework/integration/channel/DirectChannel.html)를
보면 다음과 같이 적혀있다.

> A channel that invokes a single subscriber for each sent Message. The invocation will occur in the sender's thread.
> 
> 전송된 각 메시지를 단일 구독자에게 전달하는 채널입니다. 메시지 전달은 메시지 송신측의 스레드에서 발생합니다.

즉, 스레드는 DirectChannel로 메시지를 송신한 MQTT 어댑터 등에서 생성되어, end-point까지 단일 스레드로 흐른다는 의미인 것 같다.

이것은 결국 아래와 같은 구성으로 개발되었을 때

![](https://user-images.githubusercontent.com/12710869/192131754-82e0d4a7-ff80-472e-9d21-3077789c44d4.png)

DirectChannel의 특성상 그 다음의 end-point는 상위의 DirectChannel과 같은 스레드에서 실행되므로, ~~때려죽여도~~ 멀티스레드로 처리할 수는 없다는 의미가 된다.

그렇기 때문에, 중간에 라우터를 둬서 아래와 같은 구성으로 간다 하더라도

![](https://user-images.githubusercontent.com/12710869/192131865-4b407d58-40d3-43f8-9237-e838de7cfd35.png)

상단의 DirectChannel과 하단의 end-point까지가 모두 하나의 스레드로 묶이게 되므로, 최초에 메시지가 ```end-point #1```로 도달한 시점에서
```end-point #1```의 처리가 완료될 때까지 상단의 DirectChannel이 함께 멈추게 된다.

물론 Spring Integration에는 DirectChannel같은 구독형 채널(후술)의 성질을 가지면서도, 메시지 전달은 별도의 스레드(정확히는 Executor)에게 위임하는, 
[ExecutorChannel](https://docs.spring.io/spring-integration/api/org/springframework/integration/channel/ExecutorChannel.html)이
있기는 하다. 하지만 이 채널은 메시지 수신시마다 별도의 스레드가 생성된 후 메시지를 해당 스레드로 전달하기 때문에, 맨 처음에 언급한 해결법과 동일한 결과를 보여줄 것이다.
~~사실 아직 안써봄~~

이 이슈를 해소하기 위해서는, 메시지를 수신하면 다음 구성요소로 스스로 전달하는(그 덕에 메시지 수신시부터 끝점의 처리가 끝날때까지 단일 스레드 처리가 강제되는)
DirectChannel이 아닌, 외부에서 직접 메시지를 가져가도록 구성된 QueueChannel을 사용할 필요가 있다.

DirectChannel과 QueueChannel은 채널의 종류 자체가 근본적으로 다른데, DirectChannel은 구독형 채널로서 메시지가 수신되면 채널 자기 자신에게 가입한 메시지 핸들러에게
채널이 수신한 메시지를 직접 달해주는 반면, QueueChannel은 폴러블(Pollable) 채널로서 메시지가 수신되면 채널 내부에 메시지를 저장하고 이 메시지를 외부의 핸들러가 가져갈
때까지 메시지를 저장해둔다.

|                  | DirectChannel                    | QueueChannel         |
|------------------|----------------------------------|----------------------|
| 채널 구분          | 구독형(Subscrible) 채널               | 폴러블(Pollable) 채널) |
| 메시지 수신시 처리   | 구독자 메시지 핸들러의 메서드를 채널이 직접 호출해서 전송 | 채널은 메시지를 저장하고, 외부에서 데이터를 가져갈 때까지 저장을 유지 |

간략하게 의사 코드로 나타내보면, DirectChannel은

```java
// 선언시
DirectChannel channel = new DirectChannel();
channel.subscribe(messageHandler);    // 바로 이 부분이 Spring Integration 어노테이션이 대신 해주는 부분

// 채널로 메시지 송신시
channel.send(message);
// 이렇게 호출될 경우, DirectChannel 내부에서 messageHandler.handleMessage(message) 를 호출해서 메시지를 직접 전달해준다.
```

이렇게 DirectChannel이 능동적으로 메시지 핸들러를 호출해서 전달하는데 반해, QueueChannel은

```java
// 선언시
QueueChannel channel = new QueueChannel();
// DirectChannel과는 달리, 메시지 핸들러를 구독하지 않는다.

// 채널로 메시지 송신시
channel.send(message);
// 이렇게 메시지가 수신될 경우, channel 내부의 큐에 메시지를 저장된다. 이후 별도의 핸들러에서 receive() 메서드를 호출해서 Queue의 내용을 조회한다.

// another message handler...
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

위와 같이, 메시지 수신시에는 메시지를 저장만 할 뿐 아무것도 안하고 있다가(응?) 외부에서 receive() 메서드를 호출했을 때에
메시지를 반환만 하고 자신이 갖고 있는 메시지는 삭제하는 특성이 있다.

이런 특성으로, QueueChannel이 채널과 핸들러를 별로 클래스로 분리하기에 좋은 것 같으므로, DirectChannel을 QueueChannel로 변경한다.

### 라우터 작성

Spring Integration에는 메시지 수신시, 지정된 특정 조건에 따라 다른 채널로의 분기를 지원하는 라우터가 있다.

이 라우터를 통해 토픽에 맞는 채널로 분기를 하도록 짜되, 만약 토픽에 맞는 채널이 생성되어 있지 않다면 채널을 새로 생성해서 스프링 빈으로 등록한다.

```java
// kr.ayukawa.mqttpractice.config.router.EachMqttTopicNameRouter

// package 및 import 생략
// 이 코드는 저장소에 저장된 코드와는 조금 다른데, 코드를 짧게 작성하기 위해 의도적으로 예외처리나 번잡한 반복 코드들을 제외했기 때문임
public class EachMqttTopicNameRouter extends AbstractMessageRouter {
    @Autowired
    private ApplicationContext ctx;

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
            // 이 핸들러 스레드는 IoT 장비가 늘어날 때마다 개수의 제한 없이 계속해서 늘어날 것이므로, Executor 를 사용하지 않고 바로 스레드를 띄운다.
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

스프링은 기본적으로 어플리케이션이 구동될 때, 어플리케이션에 사용할 빈을 모두 컨텍스트에 등록한 뒤 필요할 때마다 빈을 가져와서 실행하는 것을 원칙으로 삼는다.

또한 스프링은 어플리케이션이 구동되는 동안 동적으로 빈을 추가하는 것을 권장하지 않는걸로 추정된다. ```ApplicationContext```를 빈을 직접 추가할 수 있게 해주는
클래스 타입으로 적절히 캐스팅한 다음 ```refresh()``` 메서드를 호출해서 빈 목록을 재구성하려 할때 예외가 발생하는 것에서 추정할 수 있다.
그렇다면, 이렇게 스프링의 개발 원칙(추정)을 어겨가면서 개발을 할만한 합당한 요소가 있었는가 고민해보지 않을 수 없다.

#### 애시당초 빈으로 등록할 필요가 있었는가?

사실 이 코드를 처음 짤때는 Spring Integration의 구성 요소도 당연히 스프링 빈으로 등록해야 하는거 아닐까 하는 강박관념 같은게 좀 있었다.
그래서 메세지 채널도 핸들러도 어떻게든(...) 수단과 방법을 가리지 않고(......) 스프링 빈으로 등록하려고 발악(.........)을 했었다.
위 문장에서 언급한, ```ApplicationContext```가 빈 목록을 재구성하지 못한다는 내용도 이거 하려다가 발견했던 이슈였다.

그런데 실제로 코드에서는 메시지 채널을 생성하고 빈으로 등록한 다음, 그 빈을 다시 가져다 쓰지 않는다. 다시 가져다 쓰지도 않는데 굳이
이걸 스프링 빈으로 등록해서 썼어야 했나 싶은 것이다.

어차피 다시 가져다 쓰지 않는다면, 채널과 핸들러만 별도 관리하는 전역 공유 Map을 만들어서 이걸 통해서 관리하면 됬던 것이 아닐까 싶다.

코드를 파기했기 때문에 이 이상 테스트를 해보지는 않았다.

#### 퍼포먼스 문제

이 코드는 MQTT 토픽의 수만큼 핸들러 스레드를 구동한다. 이 스레드는 어플리케이션이 종료되기 전까지는 계속 무한루프를 처리하며, 종료되지 않는다.

위에서도 언급했지만, MQTT 토픽의 수는 장비의 수와 동일하다. 만약 감시해야 하는 IoT 장비가 200대라면? 400대라면? 800대라면?
단위 수를 늘려서 200대라면? 8000대라면? 8000개의 스레드를 과연 서버가 버틸 수 있을까?

#### 메모리 누수 문제

코드 상으로는 별다른 메모리 누수의 요인은 보이지 않지만, 스레드를 계속해서 올리는 이상 메모리 누수의 위협이 없다고는 할 수 없다.

#### 설계 자체에 대한 의심

처음에 토픽명 별로 별도 스레드를 가져가야겠다고 생각하고 이것저것 많이 조사를 했었다.
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
아예 MQTT의 설계 사상에 맞춰서 아예 시스템을 분리하거나 다른 메시지 브로커를 쓰는 것이 더 낫지 않았을까?