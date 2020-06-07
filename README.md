# OpenTelemetry Instrumentation: Spring and SpringBoot

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

## Features (IN PROGRESS)

Examples and detailed explainations of the features below can be found in tutorial 3. 

### @ConfigTracer

Initializes a global tracer for your application which can be injected as a spring dependency.

@ConfigTracer fields:
- String: tracerName
 
### @TraceMethod  

Wraps a method in a span or logs it as an event

@TraceMethod fields: 
- String: name
- Boolean: isEvent 


### @TraceClass 

Wraps all public methods in a span or logs it as an event

@TraceClass fields: 
- String: name
- Boolean: isEvent
- Boolean: includeMethodName (logs method signature)


### @TraceRestControllers

Creates a new span for rest controllers when a request is received. 

No @TraceRestControllers fields: 
- Default span name: controllerClassName


### @InjectTraceRestTemplate

Inject span context to all requests using RestTemplate

@InjectTraceRestTemplate fields: 
- Boolean: isEventLogged (default true) 

### @TraceHibernateDatabase

Wraps all database calls using the Hibernate framework in a span or logs it as an event

@TraceHibernateDatabase fields: 
- String: name
- Boolean: isEvent 
