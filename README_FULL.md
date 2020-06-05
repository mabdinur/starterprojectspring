# Manual Instrumentation Starter Guide: Spring and SpringBoot

This package will streamline the manual instrumentation process in OpenTelemetry for Spring and Springboot. This will enable you to add tracing to requests, and database calls with minimal changes to application code. The objective of this package is not fully automated instrumentation, it is providing you with better tools to instrument your own code. 

This contribution for OpenTelemetry will follow in the footsteps of the existing Spring integrations in [OpenCensus](https://github.com/census-instrumentation/opencensus-java/tree/master/contrib/spring/src/main/java/io/opencensus/contrib/spring).


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


## Manual Instrumentation with Java SDK

A sample user journey for manual instrumentation can be found on [lightstep](https://docs.lightstep.com/otel/getting-started-java-springboot). In this example we will create two spring web services using SpringBoot. Then we will trace the requests between these services using OpenTelemetry. Finally, we will discuss improvements that can be made to the process.


### Create two Spring Projects

Using https://start.spring.io/ create two spring projects using maven, SpringBoot 2.3, Java, and the spring-web dependency. Name one project FirstService and the other SecondService. After downloading the the two projects make sure to include the OpenTelemetry dependencies listed below. 


### Setup for FirstService and SecondService

Add the dependencies below to include OpenTelemetry. In the tutorial below we will use OpenTelemetry's Jaeger and Logging trace exporters. 

#### Maven
 
##### OpenTelemetry
```xml
<dependency>
	<groupId>io.opentelemetry</groupId>
	<artifactId>opentelemetry-api</artifactId>
	<version>0.2.0</version>
</dependency>
<dependency>
	<groupId>io.opentelemetry</groupId>
	<artifactId>opentelemetry-sdk</artifactId>
	<version>0.2.0</version>
</dependency>	
```

##### LoggerExporter
```xml
<dependency>
	<groupId>io.opentelemetry</groupId>
	<artifactId>opentelemetry-exporters-logging</artifactId>
	<version>0.2.0</version>
</dependency>
```

#### JaegerExporter
```xml
<dependency>
	<groupId>io.opentelemetry</groupId>
	<artifactId>opentelemetry-exporters-jaeger</artifactId>
	<version>0.2.0</version>
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

***Note: The packages opentelemetry-exporters-logging and opentelemetry-exporters-jaeger are used to export logs. If you plan to to use a different log exporter, adding these these dependencies is not required for this tutorial***
 

### Add OpenTelemetry Tracer to Configuration to Spring Project

To enable tracing in your OpenTelemetry project configure a tracer bean. This bean will be autowired to controllers in your application to create and propagate spans. Also include in this configuration your log exporter. An example of this is shown below: 


```java
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpansProcessor;
import io.opentelemetry.trace.Tracer;

import io.opentelemetry.exporters.logging.*;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

@Configuration
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
public class OtelConfig {
    private static final tracerName = "otel-example"	
    @Bean
    public Tracer otelTracer() throws Exception {
	final Tracer tracer = OpenTelemetry.getTracerFactory().get(tracerName);

	SpanProcessor logProcessor = SimpleSpansProcessor.newBuilder(new LoggingExporter()).build();

	OpenTelemetrySdk.getTracerFactory().addSpanProcessor(logProcessor);

	return tracer;
    }
}
```

The file above configures an OpenTelemetry tracer and a span processor to the OpenTelemetrySdk to export logs. The LoggingExporter will log spans, annotations and events to console, giving more visibility. In a similar fashion, one could add another exporter such as a JaegerExporter to visualize traces on different back-ends. Similar to how the LogExporter is configured above the Jaeger configuration can be added to the OtelConfig class. 

Sample configuration for a JaegerExporter:

```java
//import io.grpc.ManagedChannelBuilder;
//import io.opentelemetry.exporters.jaeger.JaegerGrpcSpanExporter;

	SpanProcessor jaegerProcessor = SimpleSpansProcessor.newBuilder(JaegerGrpcSpanExporter.newBuilder()
		.setServiceName("otel_FirstService")
		.setChannel(ManagedChannelBuilder.forAddress("localhost", 14250).usePlaintext().build()).build())
		.build();
OpenTelemetrySdk.getTracerFactory().addSpanProcessor(jaegerProcessor);
```

Adding this Jaeger configuration to the OtelConfig.java file above will enable you to view your traces on the Jaeger UI. Details on running Jaeger are shown below. 

     
### Project Background

Here we will create rest controllers for FirstService and SecondService.
FirstService will send a GET request to SecondService to get the current time. First Service will return SecondSerivce's time with a fun little message. 



#### Setup FirstService spring project:

1. Add OpenTelemetry Dependencies
2. Add OpenTelemetry Configuration
3. Add SpringBoot main class 
4. Create a RestController for FirstService
5. Start a span to wrap the FirstServiceController
6. Configure HttpUtils.callEndpoint to inject span context into request. This is key to propagate the trace to the SecondService


```java
@SpringBootApplication
public class FirstServiceApplication {

	public static void main(String[] args) throws IOException {
		SpringApplication.run(FirstServiceApplication.class, args);
	}
}
```


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
    @Autowired
    private Tracer tracer;

    @Autowired
    HttpUtils httpUtils;

    private static String SS_URL = "http://localhost:8081/time";

    @GetMapping
    public String firstTracedMethod() {
	Span span = tracer.spanBuilder("message").startSpan();
	span.addEvent("Controller Entered");
	span.setAttribute("what.are.you", "Je suis attribute");

	try (Scope scope = tracer.withSpan(span)) {
	    return "Second Service says: "  + httpUtils.callEndpoint(SS_URL);
	} catch (Exception e) {
	    span.setAttribute("error", e.toString());
	    span.setAttribute("error", true);
	    return "ERORR: I can't tell the time";
	} finally {
	    span.end();
	}
    }
}
```

HttpUtils is a helper class which injects the span context into request headers. For this example I used Spring's RestTemplate to send requests from FirstService and SecondService. A similar approach can be used with popular Java Web Clients such as okhttp and apache http client. The key is to override the put method in HttpTextFormat.Setter<?> to handle your request format. HttpTextFormat.inject will use this setter to set the traceparent and tracestate fields in your request. These values will be used to propagate your span context to external services.


```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.Tracer;

@Component
public class HttpUtils {

    @Autowired
    private Tracer tracer;

    private HttpTextFormat<SpanContext> textFormat;
    private HttpTextFormat.Setter<HttpHeaders> setter;

    public HttpUtils(Tracer tracer) {
	textFormat = tracer.getHttpTextFormat();
	setter = new HttpTextFormat.Setter<HttpHeaders>() {
	    @Override
	    public void put(HttpHeaders headers, String key, String value) {
		headers.set(key, value);
	    }
	};
    }

    public String callEndpoint(String url) throws Exception {
	HttpHeaders headers = new HttpHeaders();

	Span currentSpan = tracer.getCurrentSpan();
	textFormat.inject(currentSpan.getContext(), headers, setter);

	HttpEntity<String> entity = new HttpEntity<String>(headers);
	RestTemplate restTemplate = new RestTemplate();
	
	ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

	return response.getBody();
    }
}
```

#### Setup SecondService spring project:

1. Add OpenTelemetry Dependencies
2. Add OpenTelemetry Configuration
3. Add SpringBoot main class 
4. Create a RestController for SecondService
5. Start a span to wrap the SecondServiceController

**Note: The default port for the Apache Tomcat is 8080. On localhost both FirstService and SecondService services will attempt to run on this port raising an error. To avoid this add `server.port=8081` to the application.properties in the Springboot resource directory. Ensure the port used corresponds to port referenced by FirstServiceController.SS_URL. **
  

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
    public String callSecondTracedMethod() {
	Span span = tracer.spanBuilder("ingredient").startSpan();
	span.addEvent("SecondServiceController Entered");
	span.setAttribute("what.am.i", "Tu es une legume");

	try (Scope scope = tracer.withSpan(span)) {
	    return "It's time to get a watch";
	} finally {
	    span.end();
	}
    }
}
```

#### Run FirstService and SecondService:

***Ensure either LogExporter or Jaeger is configured in the OtelConfig.java file and is running*** 
 
Run FirstService and SecondService from command line or using an IDE (ex. Eclipse). The end point for FirstService should be localhost:8080/message and for SecondService, localhost:8081/time. Entering `localhost:8080/time` in a browser should call FirstService and then SecondService, creating a trace.
 
To view traces on the Jaeger UI add the Jaeger Exporter to both FirstService and SecondService projects. Deploy the Jaeger Exporter on localhost by runnning the command `docker run --rm -it --network=host jaegertracing/all-in-one` in terminal. Then send a sample request to the FirstService service. 


After running Jaeger locally, refresh the UI and view the exported traces from the two web services. Congrats, you created a distributed service with OpenTelemetry!


### otel-example 

In the otel-example/ directory you can find 3 services (FoodFinder, FoodSupplier, and FoodVendor). This example is an expansion to the the simplified example presented above. FoodFinder queries FoodSupplier and then queries FoodVendor to retrieve ingredient quantity, price and the vendor that carries the item. 

FoodSupplier retrieves the names of vendors with a specific ingredient return a list of Vendor names. FoodVendor then takes that list of vendor names and the desired ingredient. It then maps the vendor and ingredient names to item data (ex. quantity, price, currency) and returns the corresponding inventory for each vendor. The list of vendor inventories is then returned by FoodFinder. 

Unlike in the simplified example above, these services do not create spans in an individual controller instead a HandlerInterceptor is initialized which wraps all controllers in a span. To propagate the span context in a request HttpUtils is still used and this functionality was expanded to support PUT requests as well (adding a body to a request).  

FirstService is configured to run on port 8080, FoodSupplier on 8081, and FoodVendor on 8082. You can download and run the three services.  

Sample request: http://localhost:8080/foodfinder/ingredient?ingredientName=item3

Expect response: 

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

Trace: 

![Alt text](https://screenshot.googleplex.com/xi7hzNAukHv.png)

Note: This data is read from a json file. Ingredient data is stored in [FoodVendor](foodvendor/src/main/resources/vendors.json) and vendors data is stored in [FoodSupplier](foodsupplier/src/main/resources/suppliers.json).


## Manual Instrumentation using Spring Handlers/Interceptors

Using the FirstService and SecondService above we can extract  

## Proposed Improvements

### Create new spring contrib package for Open Telemetry  

```xml
 <dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-contrib-spring</artifactId>
    <version>VERSION</version>
 </dependency>
```
 
This new dependency can be added to string applications adopting OpenTelemetry to leverage the newly proposed manual instrumentation tools.
 
### Add @ConfigTracer

To use the annotations below you must place this annotation on the the main class of the project. This will create an OpenTelemetry tracer bean which can be autowired and injected as a dependency.  

@ConfigTracer Fields:
- String: tracerName

Example Usage:

```java
@ConfigTracer(name="tracerName")
@SpringBootApplication
public class SecondServiceApplication {

    public static void main(String[] args) throws IOException {
	SpringApplication.run(SecondServiceApplication.class, args);
    }

}
```

This annotation will use this method:
`OpenTelemetry.getTracerFactory().get("tracerName")`

The default tracer name will be the the name of the main class.
 
### Create @TraceMethod and @TraceClass annotations 

These new annotation with allow users to wrap methods or classes in a span. The TraceClass annotation will come with the ability to exclude private methods, and whether to include the name of the called method in the span  as an event. 

@TraceMethod fields: 
- String: name
- Boolean: isEvent (if true, creates event using method signature and adds it to a span)

@TraceClass fields: 
- String: name
- Boolean: includeMethodName (logs method signature)
- Boolean: includePrivateMethods


### Create @TraceControllers

This annotation adds wraps all RestControllers in a span using HandlerInterceptors. An example of initializing HandlerInterceptors can be seen [here](foodfinder/src/main/java/starterproject/FirstService/telemetry/InterceptorConfig.java). 

This annotation will contain the @ConfigTracer functionality. ***I am not sure if this will cause confusion, please advise?*** 

Example Usage:

```java
@TraceControllers 
@SpringBootApplication
public class SecondServiceApplication {

	public static void main(String[] args) throws IOException {
		SpringApplication.run(SecondServiceApplication.class, args);
	}
}
```

@TraceControllers fields: 
- Fields:
Boolean: methodIsLogged 

Creates new span for every request. Sets name to HTTPMethod + url. If logMethodCall is true. Event: Controller Name + Method Name, is added 


### Create @TraceDatabase

This will have similar functionality to @TracedMethod. When placed on a method or class with a call to `java.sql.PreparedStatement.execute*`, this annotation will wrap that database call in a span. 


### Create @TraceRestTemplate

To replace the HttpUtils class and abstract the injection of the span context into request headers. Similar to the proposed @TraceControllers, this annotation will use an interceptor. Using the ClientHttpRequestInterceptor I will propagate the current span context to  all external requests which use RestTemplate. The core of the proposed functionality can be seen here: (RestClientConfig)[foodfinder/src/main/java/starterproject/FirstService/telemetry/RestClientConfig.java] and (RestTemplateHeaderModifierInterceptor.java)[foodfinder/src/main/java/starterproject/FirstService/telemetry/RestTemplateHeaderModifierInterceptor.java]

This is a first attempt and I will experiment with scaling this functionality to handle other web clients such as Apache Http Client, and gRPC. ***Please advice on this approach***


@TraceRestTemplate fields: 
- No proposed Fields as of now


### Alternative to @TraceRestTemplate

One challenge to improving user experience is context propagation. As of now OpenTelemetry does not support popular web clients such as gRPC, Spring's RestTemplate or Apache Http Client. The only documentation I can find on context propagation involves manually injecting a span context into a request header. I used this approach in an example in the helper method HttpUtils.call_endpoint to propagate the span context from FirstService to SecondService. In my proposed @TraceRestTemplate annotation I will provide this functionality to users. However it will only support one framework in a limited fashion.

Open Tracing has library instrumentation for injecting and extracting the span context in to a payload as can be seen below:

(ava-web-servlet-filter)[https://github.com/opentracing-contrib/java-web-servlet-filter]  (inject)

java-okhttp: https://github.com/opentracing-contrib/java-okhttp (extract)
java-apache-httpclient: https://github.com/opentracing-contrib/java-apache-httpclient (extract)
java-asynchttpclient: https://github.com/opentracing-contrib/java-asynchttpclient (extract)
Spring’s RestTemplate: https://github.com/opentracing-contrib/java-spring-web/tree/master/opentracing-spring-web (extract)

Support for one or two of these Web Clients should be added in OpenTelemetry as well. This would improve the user friendliness of span propagation. This approach would make my proposed @TraceRestTemplate obsolete, however it seems to be a better approach. 

I can pick up this work after adding the annotations. Unless someone is already working on this. If so, I can pitch in to help :)
