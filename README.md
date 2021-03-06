# OpenTelemetry Instrumentation: Spring and Spring Boot
<!-- ReadMe is in progress -->
<!-- TO DO: Add sections for starter guide -->

This package streamlines the manual instrumentation process of OpenTelemetry for [Spring](https://spring.io/projects/spring-framework) and [Spring Boot](https://spring.io/projects/spring-boot) applications. It will enable you to add traces to requests and database calls with minimal changes to application code. This package will not fully automate your OpenTelemetry instrumentation, instead, it will provide you with better tools to instrument your own code. 

The [first section](#manual-instrumentation-with-java-sdk) will walk you through span creation and propagation using the OpenTelemetry Java API and [Spring's RestTemplate Http Web Client](https://spring.io/guides/gs/consuming-rest/). This approach will use the "vanilla" OpenTelemetry API to make explicit tracing calls within an application's controller. 

The [second section](#manual-instrumentation-using-handlers-and-interceptors)  will build on the first. It will walk you through implementing spring-web handler and interceptor interfaces to create traces with minimal changes to existing application code. Using the OpenTelemetry API, this approach involves copy and pasting files and a significant amount of manual configurations. 

The [third section](#instrumentation-using-opentelemetry-instrumentation-spring-in-progress) will walk you through the annotations and configurations defined in the opentelemetry-instrumentation-spring package. This section will equip you with new tools to streamline the setup and instrumentation of OpenTelemetry on Spring and Spring Boot applications. With these tools you will be able to setup distributed tracing with little to no changes to existing configurations and easily customize traces with minor additions to application code.

In this guide we will be using a running example. In section one and two, we will create two spring web services using Spring Boot. We will then trace the requests between these services using two different approaches. Finally, in section three we will explore tools in the opentelemetry-instrumentation-spring package which can improve this process.

# Manual Instrumentation Guide

## Create two Spring Projects

Using the [spring project initializer](https://start.spring.io/), we will create two spring projects.  Name one project `FirstService` and the other `SecondService`. In this example `FirstService` will be a client of `SecondService` and they will be dealing with time. Make sure to select maven, Spring Boot 2.3, Java, and add the spring-web dependency. After downloading the two projects include the OpenTelemetry dependencies and configuration listed below. 

## Setup for Manual Instrumentation

Add the dependencies below to enable OpenTelemetry in `FirstService` and `SecondService`. The Jaeger and LoggingExporter packages are recommended for exporting traces but are not required. As of May 2020, Jaeger, Zipkin, OTLP, and Logging exporters are supported by opentelemetry-java. Feel free to use whatever exporter you are most comfortable with. 

### Maven
 
#### OpenTelemetry
```xml
<dependency>
   <groupId>io.opentelemetry</groupId>
   <artifactId>opentelemetry-api</artifactId>
   <version>0.5.0</version>
</dependency>
<dependency>
   <groupId>io.opentelemetry</groupId>
   <artifactId>opentelemetry-sdk</artifactId>
   <version>0.5.0</version>
</dependency>   
<dependency>
   <groupId>io.grpc</groupId>
   <artifactId>grpc-context</artifactId>
   <version>1.24.0</version>
</dependency>

```

#### LoggingExporter
```xml
<dependency>
   <groupId>io.opentelemetry</groupId>
   <artifactId>opentelemetry-exporters-logging</artifactId>
   <version>0.5.0</version>
</dependency>
```

#### JaegerExporter
```xml
<dependency>
   <groupId>io.opentelemetry</groupId>
   <artifactId>opentelemetry-exporters-jaeger</artifactId>
   <version>0.5.0</version>
</dependency>
<dependency>
   <groupId>io.grpc</groupId>
   <artifactId>grpc-protobuf</artifactId>
   <version>1.27.2</version>
</dependency>
<dependency>
   <groupId>io.grpc</groupId>
   <artifactId>grpc-netty</artifactId>
   <version>1.27.2</version>
</dependency>
```

### Gradle
 
#### OpenTelemetry
```gradle
compile "io.opentelemetry:opentelemetry-api:0.5.0"
compile "io.opentelemetry:opentelemetry-sdk:0.5.0"
compile "io.grpc:grpc-context:1.24.0"
```

#### LoggingExporter
```gradle
compile "io.opentelemetry:opentelemetry-exporters-logging:0.5.0"
```

#### JaegerExporter
```gradle
compile "io.opentelemetry:opentelemetry-exporters-jaeger:0.5.0"
compile "io.grpc:grpc-protobuf:1.27.2"
compile "io.grpc:grpc-netty:1.27.2"
```

### Tracer Configuration

To enable tracing in your OpenTelemetry project configure a Tracer Bean. This bean will be auto wired to controllers to create and propagate spans. This can be seen in the `Tracer otelTracer()` method below. If you plan to use a trace exporter remember to also include it in this configuration class. In section 3 we will use an annotation to set up this configuration.

A sample OpenTelemetry configuration using LoggingExporter is shown below: 

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.trace.Tracer;
import io.opentelemetry.exporters.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporters.logging.*;

@Configuration
public class OtelConfig {
   private final static tracerName = "fooTracer"; 
   @Bean
   public Tracer otelTracer() throws Exception {
      final Tracer tracer = OpenTelemetry.getTracer(tracerName);

      SpanProcessor logProcessor = SimpleSpanProcessor.newBuilder(new LoggingSpanExporter()).build();
      OpenTelemetrySdk.getTracerProvider().addSpanProcessor(logProcessor);
      
      return tracer;
   }
}
```


The file above configures an OpenTelemetry tracer and a span processor. The span processor builds a log exporter which will output spans to the console. Similarly, one could add another exporter, such as the `JaegerExporter`, to visualize traces on a different back-end. Similar to how the `LoggingExporter` is configured, a Jaeger configuration can be added to the `OtelConfig` class above. 

Sample configuration for a Jaeger Exporter:

```java

SpanProcessor jaegerProcessor = SimpleSpanProcessor
            .newBuilder(JaegerGrpcSpanExporter.newBuilder().setServiceName(tracerName)
                  .setChannel(ManagedChannelBuilder.forAddress("localhost", 14250).usePlaintext().build())
                  .build())
            .build();
OpenTelemetrySdk.getTracerProvider().addSpanProcessor(jaegerProcessor);
```
       
### Project Background

Here we will create RestControllers for `FirstService` and `SecondService`.
`FirstService` will send a GET request to `SecondService` to retrieve the current time. After this request is resolved, `FirstService` then will append a message to time and return a string to the client. 

## Manual Instrumentation with Java SDK

### Add OpenTelemetry to FirstService and SecondService

Required dependencies and configurations for FirstService and SecondService projects can be found [here](#setup-for-manual-instrumentation).

### Instrumentation of Receiving Service: FirstService

1. Ensure OpenTelemetry dependencies are included
2. Ensure an OpenTelemetry Tracer is configured

3. Ensure a Spring Boot main class was created by the Spring initializer

```java
@SpringBootApplication
public class FirstServiceApplication {

   public static void main(String[] args) throws IOException {
      SpringApplication.run(FirstServiceApplication.class, args);
   }
}
```

4. Create a RestController for FirstService
5. Create a span to wrap FirstServiceController.firstTracedMethod()

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.opentelemetry.context.Scope;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;

import HttpUtils;

@RestController
@RequestMapping(value = "/message")
public class FirstServiceController {
   private static int requestCount = 1;
   
   @Autowired
   private Tracer tracer;

   @Autowired
   private final HttpUtils httpUtils;

   private static final String secondServiceUrl = "http://localhost:8081/time";

   @GetMapping
   public String firstTracedMethod() {
      Span span = tracer.spanBuilder("message").startSpan();
      span.addEvent("Controller Entered");
      span.setAttribute("firstservicecontroller.request.count", requestCount++);

      try (Scope scope = tracer.withSpan(span)) {
         return "Second Service says: " + httpUtils.callEndpoint(secondServiceUrl);
      } catch (Exception e) {
         span.setAttribute("error", true);
         return "ERROR: I can't tell the time";
      } finally {
         span.addEvent("Exit Controller");
         span.end();
      }
   }
}
```

6. Configure `HttpUtils.callEndpoint` to inject span context into request. This is key to propagate the trace to the SecondService

HttpUtils is a helper class that injects the current span context into outgoing requests. This involves adding the tracer id and the trace-state to a request header. For this example, we used `RestTemplate` to send requests from `FirstService` to `SecondService`. A similar approach can be used with popular Java Web Clients such as [okhttp](https://square.github.io/okhttp/) and [apache http client](https://www.tutorialspoint.com/apache_httpclient/apache_httpclient_quick_guide.htm). The key to this implementation is to override the put method in `HttpTextFormat.Setter<?>` to handle your request format. `HttpTextFormat.inject` will use this setter to set `traceparent` and `tracestate` headers in your requests. These values will be used to propagate your span context to external services.


```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import io.grpc.Context;

import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.Tracer;

@Component
public final class HttpUtils {

   private static final HttpTextFormat.Setter<HttpHeaders> setter = new HttpTextFormat.Setter<HttpHeaders>() {
         @Override
         public void set(HttpHeaders headers, String key, String value) {
            headers.set(key, value);
         }
      };
      
   @Autowired
   private Tracer tracer;

   private HttpTextFormat<SpanContext> textFormat;

   public HttpUtils(Tracer tracer) {
      textFormat = tracer.getHttpTextFormat();
   }

   public String callEndpoint(String url) throws Exception {
      HttpHeaders headers = new HttpHeaders();

      textFormat.inject(Context.current(), headers, setter);

      HttpEntity<String> entity = new HttpEntity<String>(headers);
      RestTemplate restTemplate = new RestTemplate();

      ResponseEntity<String> response =
            restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

      return response.getBody();
   }
}
```
### Instrumentation of Client Service: SecondService

1. Ensure OpenTelemetry dependencies are included
2. Ensure an OpenTelemetry Tracer is configured
3. Ensure a Spring Boot main class was created by the Spring initializer

```java
import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SecondServiceApplication {

   public static void main(String[] args) throws IOException {
      SpringApplication.run(SecondServiceApplication.class, args);
   }
}
```

4. Create a RestController for SecondService
5. Start a span to wrap SecondServiceController.secondTracedMethod()

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.opentelemetry.context.Scope;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;

@RestController
@RequestMapping(value = "/time")
public class SecondServiceController {
   @Autowired
   private Tracer tracer;

   @GetMapping
   public String secondTracedMethod() {
      Span span = tracer.spanBuilder("time").startSpan();
      span.addEvent("SecondServiceController Entered");
      span.setAttribute("what.am.i", "Tu es une legume");

      try{
         return "It's time to get a watch";
      } finally {
         span.end();
      }
   }
}
```

### Run FirstService and SecondService

***To view your distributed traces ensure either LogExporter or Jaeger is configured in the OtelConfig.java file*** 

To view traces on the Jaeger UI, deploy a Jaeger Exporter on localhost by running the command in terminal:

`docker run --rm -it --network=host jaegertracing/all-in-one` 

After running Jaeger locally, navigate to the url below. Make sure to refresh the UI to view the exported traces from the two web services:

`http://localhost:16686`

Run FirstService and SecondService from command line or using an IDE. The end point of interest for FirstService is `http://localhost:8080/message` and  `http://localhost:8081/time` for SecondService. Entering `localhost:8080/message` in a browser should call FirstService and then SecondService, creating a trace. 

***Note: The default port for the Apache Tomcat is 8080. On localhost both FirstService and SecondService services will attempt to run on this port raising an error. To avoid this add `server.port=8081` to the resources/application.properties file. Ensure the port specified corresponds to port referenced by FirstServiceController.secondServiceUrl. ***

Congrats, we just created a distributed service with OpenTelemetry!

## Manual Instrumentation using Handlers and Interceptors

In this section, we will implement the Spring HandlerInerceptor interface to wrap all requests to FirstService and Second Service controllers in a span. 

We will also use the RestTemplate HTTP client to send requests from FirstService to SecondService. To propagate the trace in this request we will also implement the ClientHttpRequestInterceptor interface. This implementation is only required for projects that send outbound requests. In this example it is only required for FirstService. 

### Setup FirstService and SecondService

Using the earlier instructions [create two example projects](#create-two-spring-projects) and add the required [dependencies and configurations](#setup-for-manual-instrumentation). 

### Instrumentation of Client Service: SecondService

Ensure the main method in SecondServiceApplication is defined. This will be the entry point to the SecondService project. This file should be created by the Spring Boot project initializer.

```java
import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SecondServiceApplication {

   public static void main(String[] args) throws IOException {
      SpringApplication.run(SecondServiceApplication.class, args);
   }
}
```

Add the RestController below to your SecondService project. This controller will return a string when SecondServiceController.secondTracedMethod is called:

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.opentelemetry.context.Scope;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;

@RestController
@RequestMapping(value = "/time")
public class SecondServiceController {
   @Autowired
   private Tracer tracer;

   @GetMapping
   public String secondTracedMethod() {
      return "It's time to get a watch";
   }
}
```

#### Create Controller Interceptor

Add the class below to wrap all requests to the SecondServiceController in a span. This class will call the preHandle method before the RestController is entered and the postHandle method after a response is created. 

The preHandle method starts a span for each request. The postHandle method closes the span and adds the span context to the response header. This implementation is shown below:    

```java

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import io.grpc.Context;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;
import io.opentelemetry.trace.TracingContextUtils;

@Component
public class ControllerTraceInterceptor implements HandlerInterceptor {

   private static final HttpTextFormat.Getter<HttpServletRequest> getter =
         new HttpTextFormat.Getter<HttpServletRequest>() {
            public String get(HttpServletRequest req, String key) {
               return req.getHeader(key);
            }
         };

   private static final HttpTextFormat.Setter<HttpServletResponse> setter =
         new HttpTextFormat.Setter<HttpServletResponse>() {
            @Override
            public void set(HttpServletResponse response, String key, String value) {
               response.addHeader(key, value);
            }
         };

   @Autowired
   private Tracer tracer;

   @Override
   public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
         throws Exception {
     
      Span span = createSpanWithParent(request, context);
      span.setAttribute("handler", "pre");
      tracer.withSpan(span);

      return true;
   }

   @Override
   public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
         ModelAndView modelAndView) throws Exception {

      Span currentSpan = tracer.getCurrentSpan();
      currentSpan.setAttribute("handler", "post");
      OpenTelemetry.getPropagators().getHttpTextFormat().inject(Context.current(), response, setter);
      currentSpan.end();
   }

   @Override
   public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
         Object handler, Exception exception) throws Exception {}
   
   private Span createSpanWithParent(HttpServletRequest request, Context context) {
      Span parentSpan = TracingContextUtils.getSpan(context);

      if (parentSpan.getContext().isValid()) {
         return  tracer.spanBuilder(request.getRequestURI()).setParent(parentSpan).startSpan();
      }

      Span span = tracer.spanBuilder(request.getRequestURI()).startSpan();
      span.addEvent("Parent Span Not Found");

      return span;
   }
}

```

The final step is to register an instance of the ControllerTraceInterceptor:

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Component
public class InterceptorConfig extends WebMvcConfigurationSupport {

   @Autowired
   ControllerTraceInterceptor controllerTraceInterceptor;

   @Override
   public void addInterceptors(InterceptorRegistry registry) {
      registry.addInterceptor(controllerTraceInterceptor);
   }
}
```

Now your SecondService application is complete. Create the FirstService application using the instructions below and then run your distributed service!

### Instrumentation of Receiving Service: FirstService

Ensure the main method in FirstServiceApplication is defined. This will be the entry point to the FirstService project. This file should be created by the Spring Boot project initializer.

```java
@SpringBootApplication
public class FirstServiceApplication {

   public static void main(String[] args) throws IOException {
      SpringApplication.run(FirstServiceApplication.class, args);
   }
}
```

Create a RestController for FirstService. This controller will send a request to SecondService and then return the response to the client:

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value = "/message")
public class FirstServiceController {
   @Autowired
   private Tracer tracer;
	
   @Autowired
   private RestTemplate restTemplate;

   @Autowired
   HttpUtils httpUtils;

   private static String secondServiceUrl = "http://localhost:8081/time";

   @GetMapping
   public String firstTracedMethod() {

      ResponseEntity<String> response =
            restTemplate.exchange(secondServiceUrl, HttpMethod.GET, null, String.class);
      String secondServiceTime = response.getBody();

      return "Second Service says: " + secondServiceTime;

   }
}
```

As seen in the setup of SecondService, create implement the TraceInterceptor interface to wrap requests to the SecondServiceController in a span. Then register this new handler by extending the HandlerInterceptor. In effect, we will be taking a copy of the InterceptorConfig.java and ControllerTraceInterceptor.java defined in SecondService and adding it to FirstService. These files are referenced [here](#create-controller-interceptor).

#### Create Client Http Request Interceptor

Next, we will configure the ClientHttpRequestInterceptor to intercept all client HTTP requests made using RestTemplate.

To propagate the span context from FirstService to SecondService we must inject the trace id and trace state into the outgoing request header. In section 1 this was done using the helper class HttpUtils. In this section, we will implement the ClientHttpRequestInterceptor interface and register this interceptor in our application. 

Include the two classes below to your FirstService project to add this functionality:


```java

import java.io.IOException;

import io.grpc.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;

import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;

@Component
public class RestTemplateHeaderModifierInterceptor implements ClientHttpRequestInterceptor {

   @Autowired
   private Tracer tracer;

   private static final HttpTextFormat.Setter<HttpRequest> setter =
         new HttpTextFormat.Setter<HttpRequest>() {
            @Override
            public void set(HttpRequest carrier, String key, String value) {
               carrier.getHeaders().set(key, value);
            }
         };


   @Override
   public ClientHttpResponse intercept(HttpRequest request, byte[] body,
         ClientHttpRequestExecution execution) throws IOException {

      Span currentSpan = tracer.getCurrentSpan();
      currentSpan.setAttribute("client_http", "inject");
      currentSpan.addEvent("Request sent to SecondService");

      OpenTelemetry.getPropagators().getHttpTextFormat().inject(Context.current(), request, setter);

      ClientHttpResponse response = execution.execute(request, body);

      return response;
   }
}

```

```java
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

   @Autowired
   RestTemplateHeaderModifierInterceptor restTemplateHeaderModifierInterceptor;

   @Bean
   public RestTemplate restTemplate() {
      RestTemplate restTemplate = new RestTemplate();

      List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
      if (interceptors.isEmpty()) {
         interceptors = new ArrayList<>();
      }
      interceptors.add(restTemplateHeaderModifierInterceptor);
      restTemplate.setInterceptors(interceptors);

      return restTemplate;
   }
}
```

### Create a distributed trace 

By default Spring Boot runs a Tomcat server on port 8080. This tutorial assumes FirstService runs on the default port (8080) and SecondService runs on port 8081. This is because we hard coded the SecondService end point in FirstServiceController.SecondServiceUrl. To run SecondServiceApplication on port 8081 include `server.port=8081` in the resources/application.properties file. 

Run both the FirstService and SecondService projects in terminal or using an IDE (ex. Eclipse). The end point for FirstService should be `http://localhost:8080/message` and `http://localhost:8081/time` for SecondService. Type both urls in a browser and ensure you receive a 200 response. 

To visualize this trace add a trace exporter to one or both of your applications. Instructions on how to setup LogExporter and Jaeger can be seen [above](#tracer-configuration). 

To create a sample trace enter `localhost:8080/message` in a browser. This trace should include a span for FirstService and a span for SecondService.

### Sample application with distributed tracing: otel-example 

In the otel-example/ directory you can find 3 services (FoodFinder, FoodSupplier, and FoodVendor) which create a distrubted trace using techniques shown in section 2. In this example FoodFinder queries FoodSupplier for a list of vendors which carry an ingredient. Using the list of vendors FoodFinder then queries FoodVendor to retrieve a list of ingredient prices, quantity, and their corresponding vendor. This list of ingredient data is the returned to the client.

FirstService is configured to run on port 8080, FoodSupplier on 8081, and FoodVendor on 8082.This example uses both the Jaeger and LogExporters. You can download the three services here: [FoodFinder](foodfinder), [FoodSupplier](foodsupplier_, and [FoodVendor](foodvendor). 

#### Setup

1. Download the three services
2. Build and run these services on localhost using terminal or an IDE (ex. Eclipse)
3. Run Jaegar exporter in terminal using docker: 

`docker run --rm -it --network=host jaegertracing/all-in-one`

4. Send a sample request using your browser (ex. Chrome):   

`http://localhost:8080/foodfinder/ingredient?ingredientName=item3`

5. Expect response: 

```json
[
      {
            "vendor": {
                  "name": "shop2"
            },
            "ingredients": [
                  {
                        "name": "item3",
                        "price": 1.0,
                        "quantity": 10.0,
                        "currency": "CAD"
                  }
            ]
      },
      {
            "vendor": {
                  "name": "shop1"
            },
            "ingredients": [
                  {
                        "name": "item3",
                        "price": 30.0,
                        "quantity": 3.0,
                        "currency": "CAD"
                  }
            ]
      }
]
```
Note: This data is read from a json file. Ingredient data is stored in [FoodVendor](foodvendor/src/main/resources/vendors.json) and vendors data is stored in [FoodSupplier](foodsupplier/src/main/resources/suppliers.json).

6. Inspect trace on the Jaeger UI: `http://localhost:16686`

## Instrumentation using opentelemetry-instrumentation-spring (IN PROGRESS) 

### Dependencies

The dependencies below are required to use these [features](#features-in-progress) but are not required to use the "vanilla" OpenTelementry API. Examples illustrating their use can be found in [here](#example-usage). 

#### Maven
```xml
   <dependency>
      <groupId>io.opentelemetry</groupId>
      <artifactId>opentelemetry-instrumentation-spring</artifactId>
      <version>VERSION</version>
   </dependency>
 
   <!-- spring aspects -->
   <dependency>
      <groupId>org.springframework</groupId>
      <artifactId>spring-aspects</artifactId>
      <version>SPRING_VERSION</version>
      <scope>runtime</scope>
   </dependency>
```

#### Gradle 
```gradle
compile "io.opentelemetry:opentelemetry-instrumentation-spring:VERSION"
runtime 'org.springframework:spring-aspects:SPRING_VERSION'
```

### Setup

#### Using XML Configurations

Add the beans below to your xml configuration file. The functionality provided by each bean is discussed in the feature section [below](#features-in-progress).

```
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.2.xsd http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop.xsd">

   <aop:aspectj-autoproxy/>
   
   <!-- global tracer -->
   <bean id="tracer" class="io.opentelemetry.instrumentation.spring.OtelTracer" factory-method="getTracer/>
      <!-- tracer name -->
      <constructor-arg type="java.lang.String" value="otel-tracer-name"/>     <!-- include LoggingExporter() -->
      <constructor-arg index="1" type="boolean" value="true"/>
   </bean>
   
   <!-- traces explicit calls to @TraceMethod and @TracedClass -->
   <bean id="otAspect" class="io.opentelemetry.instrumentation.spring.OTSpingAspect">
      <constructor-arg ref="tracer"/>
   </bean>
   
   
    <!-- wraps requests to spring web RestControllers in a span -->
   <bean id="interceptorConfigRC" class="io.opentelemetry.instrumentation.spring.InterceptorConfigRC">
      <constructor-arg ref="tracer"/>
   </bean>
   
   <!-- Wraps outgoing requests using Spring's RestTemplate in a span -->
   <bean id="restTemplateConfig" class="io.opentelemetry.instrumentation.spring.HandlerConfigRestTemplate">
      <constructor-arg ref="tracer"/>
      <!-- Optional constructor arg, can add interceptor to existing RestTemplate -->
      <!-- <constructor-arg ref="restTemplate"/> -->
   </bean>

</beans>

```

#### Using Annotations

As an alternative to using xml configuration this package supplies the `@ConfigTracer` annotation. This annotation enables component scanning of the `io.opentelemetry.instrumentation.spring` project and it supplies all the beans listed in the [xml configurations](#using-xml-configurations) above. Usage of this annotation is shown [here](#configtracer).


### Features (IN PROGRESS)


#### Global Open Telemetry Tracer

The method `io.opentelemetry.instrumentation.spring.OtelTracer.getTracer` generates a global tracer for your application. This bean can be autowired to project components to enable manual instrumentation. By default OpenTelemetry's lightweight LoggingExporter is enabled to log span events to console. This logger can be disabled.


#### @TraceMethod 

This annotates a method definition. It wraps a method in a span or logs a method call as an event. To use the @TraceMethod annotation to instrument you application include the OTSpingAspect bean in your configuration file or use the [config tracer](#configtracer) annotation in your application.

@TraceMethod fields: 
- String: name
- Boolean: isEvent (default false)

#### @TraceClass 

This annotates a class definition. It wraps all public methods in a span or logs all method calls as an event.

To use the @TraceMethod annotation to instrument you application include the OTSpingAspect bean in your configuration file or use the [config tracer](#configtracer) annotation in your application.

@TraceClass fields: 
- String: name
- Boolean: isEvent
- Boolean: includeMethodName (logs method signature, default false)



#### Instrument RestControllers

This package instruments spring-web RestControllers by creating and registering a Spring HandlerInterceptor. This wraps all controllers in the context in a new span. To enable this functionality include the InterceptorConfigRC bean in your configuration file or use the [config tracer](#configtracer) annotation in your application. 


#### Instrument RestTemplate

Inject span context to all requests using Spring's RestTemplate HTTP client. To enable this functionality include the HandlerConfigRestTemplate bean in your configuration file or use the [config tracer](#configtracer) annotation in your application. 

#### @TraceHibernateDatabase

This annotates a hibernate entity. It wraps all database calls using the Hibernate framework in a span or logs it as an event.

@TraceHibernateDatabase fields: 
- String: name
- Boolean: isEvent (default false)


### Example Usage


#### ConfigTracer 

```
SpringBootProject
├────src/main/java
│    └───springbootproject
│        │ SpringBootProjectApplication.java
│        └───config
│              OtelConfig.java      
```

```java
import  io.otel.instrumentation.spring.tools.annotations.ConfigTracer

@ConfigTracer
class OtelConfig {

}
```

#### Manual Instrumentation with the Global Tracer

```java
@AutoWired
Tracer tracer;

public String secondTracedMethod() {
   Span span = tracer.spanBuilder("start").startSpan();
   
   try{
      return "It's time to get a watch";
   } finally {
      span.end();
   }
}
```
 
#### @TraceMethod   

```java
@TraceMethod(name = "methodName", isEvent = True)
@GetMapping
public String callSecondTracedMethod() {
   return "It's time to get a watch";
}
```

#### @TraceClass 
 

```java
@TraceClass(name = "className", isEvent = True, includeMethodName = True)
@RestController
public class SecondServiceController {

   @GetMapping
   public String callSecondTracedMethod() {
      return "It's time to get a watch";
   }
}
```

#### @TraceHibernateDatabaseCalls

```java
@TraceHibernateDatabaseCalls(name = "r/uwaterloo")
@Entity
public class WaterlooStudents {

   @Id
   @GeneratedValue
   private Long id;
   private String gooseName;

   // standard constructors

   // standard getters and setters
}
```
