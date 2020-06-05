# Manual Instrumentation Starter Guide: Spring and SpringBoot

This package will streamline the manual instrumentation process in OpenTelemetry for Spring and Springboot. This will enable you to add tracing to requests, and database calls with minimal changes to application code. The objective of this package is not fully automated instrumentation, it is providing you with better tools to instrument your own code. 

This contribution for OpenTelemetry will follow in the footsteps of the existing Spring integrations in (Open Census)[https://github.com/census-instrumentation/opencensus-java/tree/master/contrib/spring/src/main/java/io/opencensus/contrib/spring].


This starter guide contains 3 tutorials. The first tutorial with will walk you through span creation and propagating requests through the Spring web client, RestTemplate. This second tutorial will show case how to implement Spring's handler and interceptor interfaces to add spans to rest controllers and propagate a span context to external services. This tutorial will NOT involve direct edits to existing application code. The third and final tutorial will detail how to use annotations and XML configurations defined in this package to leverages techniques from the first two tutorials. This tutorial will equip you with new tools to streamline the configuration of OpenTelemetry on Spring and SpringBoot.


To use the tools included in this package include the dependency below in your spring application:

#### Maven
```xml
 <dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-contrib-spring</artifactId>
    <version>VERSION</version>
 </dependency>
```

#### Gradle 
```gradle
compile "io.opentelemetry:opentelemetry-contrib-spring:VERSION"
```
