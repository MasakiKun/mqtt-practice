# Spring Integration + MQTT

Spring Integration을 사용해서 MQTT의 토픽 구독 및 발행을 해야 하는데, Spring Integration 자체도 잘 이해가 가지 않아서
관련 코드를 작성하는데 애를 많이 먹었다.

Spring Integration은 [Enterprise Integration Pattern](https://www.enterpriseintegrationpatterns.com/)을 구현해서 스프링에
적절하게 녹여낸 프레임워크라고는 하는데, EIP를 몰라서 그런지 그냥 사용하기엔 굉장히 까다로운 느낌이다.

이 문서에서는 내가 의식의 흐름대로(...) Spring Integration 예제 코드를 분석하면서 의식의 흐름대로(......) 익힌
Spring Integration 관련 내용을 정리한다. 의식의 흐름대로 야매로 익힌 내용이기 때문에, 실제 내용과는 다른 부분이 있을 수 있다.

## Spring Integration? Enterprise Integration Pattern?

Spring Integration은 기업 통합 패턴(Enterprise Integration Pattern)을 구현한 프레임워크이다.

인터넷을 살펴보거나 관련 책자를 살펴보면 대부분 설명이 여기서 딱 끊겨있어서-_- 기업 통합 패턴이 무엇인지 알지 못해 고생했는데,
서로 다른 시스템을 통합하기 위한 패턴으로 이해했다. 여기서 다른 시스템이란 이기종 시스템도 포함한다.

원래는 두 시스템의 통합으로 예시를 들 생각이었지만, Spring Integration + MQTT에 대해서 생각해보기에는 내용이 너무 번잡해지기 때문에 생략하고
여기서는 MQTT에만 집중해서 생각하기로 했다.

스프링을 사용해서 개발된 어떤 어플리케이션(이하 어플)과 MQTT간에 통신이 필요하다고 가정해보자.

![](https://user-images.githubusercontent.com/12710869/192128366-6ae8b790-a477-4162-a964-301c02c2a3ce.png)

이 경우, MQTT와 어플이 직접 통신하지는 못할 것이고 중간에 통신을 위한 무언가를 하나 두게 될 것이다. 이것은 MQTT와의 연결을 생성해주기 위해
MQTT 통신을 분석해서 직접 만든 유틸리티 클래스일 수도 있을 것이고, MQTT와의 통신을 위한 라이브러리를 캡슐화한 팩토리 클래스일 수도 있을 것이다. 

![](https://user-images.githubusercontent.com/12710869/192128466-139d40a3-0e97-484f-b0a1-a35edec4c498.png)

Spring Integration은 여기서 어플리케이션 외부에 있는 시스템(여기서는 MQTT)를 외부의 시스템으로 보고,
어플리케이션과 외부의 시스템이 통신하기 위한 통로를 캡슐화해서 메시지 채널(MessageChannel) 또는 채널이라고 부른다.

말이 어렵지만, 시스템 요소와 통신하기 위해 자바 표준 JDK에서 제공하는게 입출력 스트림이라고 한다면,
외부의 시스템과 통신하기 위해 Spring Integration에서 제공하는게 채널이라고 할 수 있겠다.

즉, 어플이 시스템 요소와 통신하기 위해서 스트림을 사용한다면, 외부의 시스템과 통신하기 위해서는 채널을 생각한다고 보면 될 것 같다.

(실제로는 내부 시스템 요소와 통신할 수도 있다. 타 시스템과의 통합이 목적이지, 외부 시스템과의 통신이 목적이 아니기 때문이다.)

그러므로 여기서도 MQTT와 어플 사이에는 논리적인 데이터 통로인 채널을 둔다.

![](https://user-images.githubusercontent.com/12710869/192128715-13ecb095-4521-444a-8cb3-54b4ddbe3406.png)

그러나 어플과 MQTT는 이른바 이기종 통신이라고 할 수 있다. 어플 내부에서는 자바 코드를 통해 만들어진 메서드를 통해 통신하거나,
시스템 요소(공유 파일이나 공유 DB 등)를 이용해서 통신하겠지만, MQTT와는 자바 코드로 통신하는 것도 아니고 공유 시스템 요소를 통해서 통신하는 것도 아니기 때문이다.

그러므로 여기에선 자바 코드로 만들어진 메시지 채널과 MQTT 사이에서 실제로 물리적인 통신을 담당해줄 어댑터를 둔다.
(어댑터 자체는 Spring Integration의 구성 요소는 아니고, 다시 Spring Integration의 요소인 MessageProducerSupport-메시지 제공 지원자-로
캡슐화 되어 있지만 여기서는 그냥 어댑터로 인지하고 넘어가자)

![](https://user-images.githubusercontent.com/12710869/192128803-ded418e0-223e-4f24-b410-1f59a71e6640.png)

여기까지 준비가 되었다면, MQTT가 어플로 데이터를 전달할 준비는 끝났다고 볼 수 있다. 하지만 실제로 어플 코드를 들여다보면,
채널이나 어댑터는 어플의 외부에 있는 것이 아니라 어플의 한 구성요소라고 할 수 있다.

그렇다면 채널이 MQTT로부터 받은 데이터를 전달하는 것은 어플이 아니라 어플 내의 또다른 구성 요소라고 할 수 있다.

![](https://user-images.githubusercontent.com/12710869/192128965-cc3b3058-2629-4f9f-8433-e48c0c9b87c2.png)

즉, Spring Integration은 (여기서 어댑터가 없다고 가정하면) 특정 포인터에서 데이터를 전달받아,
특정 엔드포인트로 데이터를 단순히 전달만 하는 통로의 역할을 맡기 때문에,
이 데이터를 전달받아 실제로 데이터를 처리할 끝점이 필요하게 된다.

Spring Integration에서는 이것을 끝점(Endpoint)라고 부르며, 서비스 액티베이터나 라우터, 멀티플렉서 등이 Spring Integration이 제공하는 끝점 요소이지만
기본적으로는 서비스 액티베이터를 사용하게 된다. 서비스 액티베이터는 채널로부터 전달받은 데이터를 처리하기 위한 메시지 핸들러의 역할을 한다.

![](https://user-images.githubusercontent.com/12710869/192129174-038eb186-b08a-4993-98a9-0b664e935adb.png)

여기까지의 내용을 종합해보면, Spring Integration이란 어플과 외부 시스템간의 통신을 위해서 가상의 통신 통로인 채널을 놓고,
실제 통신이나 실제 데이터 통신을 위한 요소를 채널 앞뒤에 배치해서 이기종 시스템간 통신을 지원해서 이기종 시스템간의 데이터 통합이 가능하게 만드는 프레임워크라고 할 수 있겠다.