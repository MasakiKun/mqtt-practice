# MQTT-Practice

최근 진행중인 프로젝트에서 MQTT를 이용하고 있는데, MQTT를 자주 안쓰다보니 자주 까먹고,
무엇보다도 코드에서 바로 MQTT를 사용하기 보다는 Spring Integration + MQTT 의 조합을 많이
사용하는 느낌인데, Spring Integration을 이용해서 MQTT를 구독하거나 메시지 발행하는건
필요할 때만 되면 절묘하게 기억이 안나기 때문에(...) 이번에 예제를 만들어 보았다.

# Prerequisite

이 프로젝트 및 문서는 MQTT에 대해서는 따로 설명하지 않는다. 오로지 MQTT 관련 예제를 남겨놓기 위한 저장소다.
별도로 MQTT의 개요에 관해서 살펴볼만하다 싶은 문서는 별도로 하단에 리퍼런스를 표기할 예정.

다만 개인적으로 따로 기억할 필요가 있다 싶은 부분은 별도로 명시를 해둔다.

이 문서는 MQTT Broker로 [EMQX](https://www.emqx.io/)를 사용하였는데, 선정에 별다른 이유는 없다.
그저 현재 프로젝트에서 사용중인 Broker가 이놈이기 떄문에 사용했다.

다만 Broker와는 별개로 테스트를 위한 MQTT 클라이언트가 필요한데, 인터넷에는 MQTT.fx 를 이용한 예제가 많다.
하지만 MQTT.fx는 Softblade에서 오픈소스 채로 인수해간 이후로는 완전한 유료 소프트웨어가 되어서 사용하기 곤란하다.

개인적으로는 EMQ에서 추천하는 클라이언트인 MQTT X의 CLI 버전을 사용했는데, 기능이 MQTT.fx에 비하면 일부 부족한 부분은 있지만
테스트 용도로는 전혀 부족함 없이 사용할 수 있었다.

MQTT X GUI 버전도 예전에는 MQTT.fx보다 기능상 부족한 부분이 있었는데, 지금은 어떨지 잘 모르겠다.

# 프로젝트 목록

## Paho-Client-Basic

인터넷에서 자주 보이는, [Eclipse Paho Java Client](https://www.eclipse.org/paho/clients/java/)를 이용해서 MQTT로 메시지를 전송하는 예제.
간단한 예제이므로, 별다른 주석은 작성하지 않았다. 그냥 한번 보면 알 수 있는 수준.

## Paho-Pub-and-Sub

인터넷에서 Paho Client로 메시지를 발행하는건 발에 채일 정도로 검색이 되는데, 구독하는건 이상하게 검색해도 잘 안나와서(...)
Paho Client를 이용해서 메시지를 구독하고 발행하는 예제를 작성해 보았다.

Publish 클래스와 Subscribe 클래스가 분리된 것을 제외하고는 Paho-Client-Basic 프로젝트와 크게 다른점은 없다.

## using-Spring-Integration

현재 프로젝트에서 사용중인 Spring Boot + Spring Integration + MQTT 조합의 예제 코드.

Spring Integration에 익숙하다면 어렵지 않을 코드일 것 같은데, [EIP](https://www.enterpriseintegrationpatterns.com/)에 익숙하지 않아서
이해하고 예제를 작성하는데에 애를 좀 먹었다.

실제 코드를 분석해보면서 Spring Integration에 대해서 의식의 흐름대로(...) 분석한 내용을 해당 프로젝트에 문서로 작성해 두었다.

## using-Spring-Integration-inbound-single-thread-output-multiple-thread-each-topic-name

Spring Boot + Spring Integration + MQTT 조합에서, MQTT 토픽명에 따라 별도의 스레드를 할당해서 처리하도록 구현된 코드.

MQTT를 통해서 전달받는 메시지의 양이 점점 커짐에 따라, 시스템이 그 메시지 양을 다 처리하지 못해서 구상한 코드.

다만 해당 프로젝트의 README 파일에도 있지만, 실제 프로덕션 레벨에서 쓰이지는 못한 코드라서 생각지도 못한 문제가 있을수 있다.

사용하지 못한 코드라서 그 외에도 몇가지 고민사항이 있는데, 마찬가지로 프로젝트의 README 파일에 정리해 두었다.