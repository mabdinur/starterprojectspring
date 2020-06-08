# OpenTelemetry Instrumentation: Spring and SpringBoot

This package streamlines the manual instrumentation process in OpenTelemetry for Spring and SpringBoot. It will enable you to add traces to requests, and database calls with minimal changes to application code. This package will not fully automate your OpenTelemetry instrumentation, instead, it will provide you with better tools to instrument your own code.
This contribution to OpenTelemetry will follow in the footsteps of the existing Spring integration in [OpenCensus](https://github.com/census-instrumentation/opencensus-java/tree/master/contrib/spring/src/main/java/io/opencensus/contrib/spring).

This starter guide contains three tutorials. The first tutorial will walk you through span creation and propagation using the JavaSDK and the HTTP client RestTemplate. 

The second tutorial will build on the first. It will walk you through implementing spring-web handler and interceptor interfaces to create traces without modifying your application code. 

The third tutorial will walk you through the annotations and configurations defined in the opentelemetry-contrib-spring package. This tutorial will equip you with new tools to streamline the step up and configuration of OpenTelemetry on Spring and SpringBoot applications.

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

Examples and detailed explanations of the features below can be found in tutorial 3. 

### @ConfigTracer

This annotation initializes a global tracer for your application. It will create a Tracer Object which can be injected as a dependency.

@ConfigTracer fields:
- String: tracerName
 
### @TraceMethod  

This annotates a method definition. It wraps a method in a span or logs a method call as an event.

@TraceMethod fields: 
- String: name
- Boolean: isEvent 


### @TraceClass 

This annotates a class definition. It wraps all public methods in a span or logs all method calls as an event.

@TraceClass fields: 
- String: name
- Boolean: isEvent
- Boolean: includeMethodName (logs method signature)


### @TraceRestControllers

This annotates a class definition with @RestController annotation. It creates a new span for rest controllers when a request is received. 

No @TraceRestControllers fields: 
- Default span name: controllerClassName


### @InjectTraceRestTemplate

Inject span context to all requests using RestTemplate.

@InjectTraceRestTemplate fields: 
- Boolean: isEventLogged (default true) 

### @TraceHibernateDatabase

This annotates a hibernate entity. It wraps all database calls using the Hibernate framework in a span or logs it as an event.

@TraceHibernateDatabase fields: 
- String: name
- Boolean: isEvent 

