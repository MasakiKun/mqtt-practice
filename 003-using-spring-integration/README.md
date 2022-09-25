# Spring Integration + MQTT

Spring Integration을 사용해서 MQTT의 토픽 구독 및 발행을 해야 하는데, Spring Integration 자체도 잘 이해가 가지 않아서
관련 코드를 작성하는데 애를 많이 먹었다.

Spring Integration은 [Enterprise Integration Pattern](https://www.enterpriseintegrationpatterns.com/)을 구현해서 스프링에
적절하게 녹여낸 프레임워크라 그런지, EIP를 모르고 사용하기엔 굉장히 까다로운 느낌이다.

이 문서에서는 내가 의식의 흐름대로(...) Spring Integration 예제 코드를 분석하면서 의식의 흐름대로(......) 익힌
Spring Integration 관련 내용을 정리한다. 의식의 흐름대로 야매로 익힌 내용이기 때문에, 실제 내용과는 다른 부분이 있을 수 있다.

## Spring Integration?

Spring Integration은 기업 통합 패턴(Enterprise Integration Pattern)을 구현한 프레임워크이다.

인터넷을 살펴보거나 관련 책자를 살펴보면 대부분 설명이 여기서 딱 끊겨있어서-_- 기업 통합 패턴이 무엇인지 알지 못해 고생했는데,
서로 다른 이기종 시스템을 통합하기 위한 패턴으로 이해했다.

