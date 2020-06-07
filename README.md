#Instrumentation Starter Guide: Spring and SpringBoot

This package streamlines the manual instrumentation process in OpenTelemetry for Spring and SpringBoot. It will enable you to add traces to requests, and database calls with minimal changes to application code. This package will not fully automate your OpenTelemetry instrumentation, instead it will provide you with better tools to instrument your own code.
This contribution for OpenTelemetry will follow in the footsteps of the existing Spring integration in [OpenCensus](https://github.com/census-instrumentation/opencensus-java/tree/master/contrib/spring/src/main/java/io/opencensus/contrib/spring).

This starter guide contains 3 tutorials. The first tutorial will walk you through span creation and propagation using the JavaSDK and RestTemplate. 

The second tutorial will build on the first. It will walk you through implementing Spring's handler and interceptor interfaces to create traces without modifying existing application code. 

The third will detail how to use annotations and configurations defined in this package. This tutorial will equip you with new tools to streamline the configuration of OpenTelemetry on Spring and SpringBoot applications.

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
