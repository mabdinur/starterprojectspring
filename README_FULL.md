# OpenTelemetry Instrumentation: Spring and Spring Boot
<!-- ReadMe is in progress -->
<!-- TO DO: Add sections for starter guide -->


This package streamlines the manual instrumentation process in OpenTelemetry for [Spring](https://spring.io/projects/spring-framework) and [Spring Boot](https://spring.io/projects/spring-boot). It will enable you to add traces to requests, and database calls with minimal changes to application code. This package will not fully automate your OpenTelemetry instrumentation, instead, it will provide you with better tools to instrument your own code.

This starter guide contains three sections. In the first two sections you will use the "vanilla" Open Telemetry API. In the third section, we will explore features defined in this package and use these tools to better instrument your application. 

The [first section](#section-1-manual-instrumentation-with-java-sdk) will walk you through span creation and propagation using the JavaSDK and the HTTP web client, RestTemplate. 

The [second section](#section-2-using-spring-handlers-and-interceptors) will build on the first. It will walk you through implementing spring-web handler and interceptor interfaces to create traces without modifying your application code. 

The [third section](#section-3-instrumentation-using-opentelemetry-contrib-spring-in-progress) will walk you through the annotations and configurations defined in the opentelemetry-contrib-spring package. This section will equip you with new tools to streamline the step up and configuration of OpenTelemetry on Spring and Spring Boot applications.


# Manual Instrumentation Starter Guide

A sample user journey for manual instrumentation can be found here [lightstep](https://docs.lightstep.com/otel/getting-started-java-Spring Boot). In section one and two, you will create two spring web services using Spring Boot. you will then trace the requests between these services using OpenTelemetry. Finally, you will discuss improvements that can be made to the process. These improvements will be shown in section three.


## Create two Spring Projects

Using this [Spring Initializer](https://start.spring.io/), you will create two spring projects.  Before you download the projects, name one project FirstService and the other SecondService. Make sure to select maven, Spring Boot 2.3, Java, and add the spring-web dependency. After downloading the two projects include the OpenTelemetry dependencies listed below. 


## Setup for Section 1 and Section 2

Add the dependencies below to enable OpenTelemetry in FirstService and SecondService. The Jaeger and LoggerExporter packages are recommended but not required for this section. 

### Maven
 
#### OpenTelemetry
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

#### LoggerExporter
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
 

### Tracer Configuration

To enable tracing in your OpenTelemetry project configure a tracer bean. This bean will be autowired to controllers in your application to create and propagate spans. If you plan to use a trace exporter remember to include it in this configuration file. A sample OpenTelemetry configuration using LogExporter is shown below: 


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


The file above configures an OpenTelemetry tracer and a span processor which exports traces. The LoggingExporter will log spans, annotations, and events to console, providing more visibility to your application. Similarly, one could add another exporter, such as JaegerExporter, to visualize traces on different back-ends. Similar to how the LogExporter is configured, a Jaeger configuration can be added to the OtelConfig class above. 

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
     
### Project Background

Here you will create rest controllers for FirstService and SecondService.
FirstService will send a GET request to SecondService to get the current time. FirstService will return SecondSerivce's time and it will append a message. 


## Section 1: Manual Instrumentation with Java SDK

### Setup FirstService:

1. Ensure OpenTelemetry dependencies are included
2. Ensure an OpenTelemetry Tracer is configured
3. Ensure a Spring Boot main class was created by the Spring initializer
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

HttpUtils is a helper class that injects the current span context into request headers. This involves adding the parent trace id and the trace-state to the request header. For this example, I used RestTemplate to send requests from FirstService to SecondService. A similar approach can be used with popular Java Web Clients such as okhttp and apache http client. The key is this implementation is to override the put method in HttpTextFormat.Setter<?> to handle your request format. HttpTextFormat.inject will use this setter to set the traceparent and tracestate fields in your request. These values will be used to propagate your span context to external services.


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

### Setup SecondService:

1. Ensure OpenTelemetry dependencies are included
2. Ensure an OpenTelemetry Tracer is configured
3. Ensure a Spring Boot main class was created by the Spring initializer
4. Create a RestController for SecondService
5. Start a span to wrap the SecondServiceController
  

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

### Run FirstService and SecondService:

***Ensure either LogExporter or Jaeger is configured in the OtelConfig.java file. For LogExporter you can view traces on your console.*** 

To view traces on the Jaeger UI, deploy the Jaeger Exporter on localhost by runnning the command `docker run --rm -it --network=host jaegertracing/all-in-one` in terminal. Then send a sample request to the FirstService service. 

**Note: The default port for the Apache Tomcat is 8080. On localhost both FirstService and SecondService services will attempt to run on this port raising an error. To avoid this add `server.port=8081` to the resources/application.properties file. Ensure the port specified corresponds to port referenced by FirstServiceController.SS_URL. **
 
Run FirstService and SecondService from command line or using an IDE. The end point of for FirstService should be localhost:8080/message and  localhost:8081/time for SecondService. Entering `localhost:8080/time` in a browser should call FirstService and then SecondService, creating a trace.
 

After running Jaeger locally, refresh the UI and view the exported traces from the two web services. Congrats, you created a distributed service with OpenTelemetry!

## Section 2: Using Spring Handlers and Interceptors


Using section 1, create the FirstService and SecondService projects.  Add the required OpenTelemetry dependencies, configurations, and your chosen exporter to both projects. In this section, you will implement the Spring HandlerInerceptor interface to wrap all requests to FirstService and Second Service controllers in a span. 

You will also use the RestTemplate HTTP client to send requests from FirstService to SecondService. To propagate the trace in this request you will also implement the ClientHttpRequestInterceptor interface. This implementation is only required for FirstService since this will be the only project that sends outbound requests (SecondService only receive requests from an external service). 


### Setup SecondService:

Create a rest controller for SecondService. This controller will return a string to the FirstService:

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
    return "It's time to get a watch";
    }
}
```

Create ControllerTraceInterceptor.java to wrap all requests to SecondServiceController in a span. This class will call the preHandle method before the rest controller is entered and the postHandle method after a response is created. 

The preHandle method starts a span for each request and the postHandle method closes the span and adds the span context to the response header. This implementation is shown below:   

```java
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.Tracer;

@Component
public class ControllerTraceInterceptor implements HandlerInterceptor {

    @Autowired
    private Tracer tracer;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
	    throws Exception {
	HttpTextFormat<SpanContext> textFormat = tracer.getHttpTextFormat();
	Span span;
	try {

	    SpanContext spanContext = textFormat.extract(request, new HttpTextFormat.Getter<HttpServletRequest>() {
		@Override
		public String get(HttpServletRequest req, String key) {
		    return req.getHeader(key);
		}
	    });
	    span = tracer.spanBuilder(request.getRequestURI()).setParent(spanContext).startSpan();
	    span.setAttribute("handler", "pre");
	} catch (Exception e) {
	    span = tracer.spanBuilder(request.getRequestURI()).startSpan();
	    span.addEvent(e.toString());
	    span.setAttribute("error", true);
	}
	tracer.withSpan(span);

	return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
	    ModelAndView modelAndView) throws Exception {

	HttpTextFormat<SpanContext> textFormat = tracer.getHttpTextFormat();
	Span currentSpan = tracer.getCurrentSpan();
	currentSpan.setAttribute("handler", "post");
	textFormat.inject(currentSpan.getContext(), response, new HttpTextFormat.Setter<HttpServletResponse>() {
	    @Override
	    public void put(HttpServletResponse response, String key, String value) {
		response.addHeader(key, value);
	    }
	});
	currentSpan.end();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
	    Exception exception) throws Exception {
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
    public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(controllerTraceInterceptor);
    }
}
```

Now your SecondService application is complete. Create the FirstService application using the instructions below and then run your distributed service!

### Setup FirstService:

Create a rest controller for FirstService. This controller will send a request to SecondService and then return the response to the client:

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
    
    private RestTemplate restTemplate = new RestTemplate();

    @Autowired
    HttpUtils httpUtils;

    private static String SS_URL = "http://localhost:8081/time";

    @GetMapping
    public String firstTracedMethod() {

	ResponseEntity<String> response = restTemplate.exchange(SS_URL, HttpMethod.GET, null, String.class);
	String secondServiceTime = response.getBody();
	
	return "Second Service says: " + secondServiceTime;
	
    }
}
```



As seen in the setup of SecondService, create implement the TraceInterceptor interface to wrap requests to the SecondServiceController in a span. Then register this new handler by extending HandlerInterceptor. In effect, you will be taking a copy of InterceptorConfig.java and ControllerTraceInterceptor.java from SecondService and adding it to FirstService.


Next, you will configure the ClientHttpRequestInterceptor to intercept all client HTTP requests made using RestTemplate.

To propagate the span context from FirstService to SecondService you must inject the span context into the outgoing request. In section 1 this was done within the FirstServiceController using HttpUtils. In this section, you will implement the ClientHttpRequestInterceptor and register this interceptor in our application. 

Include the two classes below to your FirstService application to add this functionality:


```java
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;

import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.Tracer;

@Component
public class RestTemplateHeaderModifierInterceptor implements ClientHttpRequestInterceptor {

    @Autowired
    private Tracer tracer;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
	    throws IOException {

	Span currentSpan = tracer.getCurrentSpan();
	currentSpan.setAttribute("client_http", "inject");
	currentSpan.addEvent("Internal request sent to food service");

	HttpTextFormat<SpanContext> textFormat = tracer.getHttpTextFormat();

	textFormat.inject(currentSpan.getContext(), request, new HttpTextFormat.Setter<HttpRequest>() {
	    @Override
	    public void put(HttpRequest request, String key, String value) {
		request.getHeaders().set(key, value);
	    }
	});

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

By default Spring Boot runs a Tomcat on the port 8080. This section assumes FirstService runs on the default port (8080) and SecondService runs on port 8081. This is because of the SS_URL hard coded in FirstService and the test urls given below. To run SecondService on port 8081 include `server.port=8081` in the resources/application.properties file. 

Run both the FirstService and SecondService projects on localhost. The end point for FirstService should be http://localhost:8080/message and http://localhost:8081/time for SecondService. 

Enter `http:\\localhost:8080/time` in a browser to create a distributed trace. This trace should include a span for FirstService and a span for SecondService.

To visualize this trace add a trace exporter to one or both of your applications. Instructions on how to setup LogExporter and Jaeger can be seen in section 1. You can also follow your trace using a debugger and tracking the request headers. 

## Sample application with distributed tracing: otel-example 

In the otel-example/ directory you can find 3 services (FoodFinder, FoodSupplier, and FoodVendor) which create a distrubted trace using section 2. In this example FoodFinder queries FoodSupplier for a list of vendors which carry an ingredient and then queries FoodVendor to retrieve a list of ingredient prices, quantity, and their corresponding vendor. 

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

Note: This data is read from a json file. Ingredient data is stored in [FoodVendor](foodvendor/src/main/resources/vendors.json) and vendors data is stored in [FoodSupplier](foodsupplier/src/main/resources/suppliers.json).
 

## Section 3: Instrumentation using opentelemetry-contrib-spring (IN PROGRESS) 

### Dependencies

The dependencies below are required to use these [features](#features-in-progress) but are not required to use the "vanilla" OpenTelementry API. Examples illustrating their use can be found in [here](#example-usage). 

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

### Features (IN PROGRESS)

#### @ConfigTracer

This annotation initializes a global tracer for your application. It will create a Tracer Object which can be injected as a dependency.

@ConfigTracer fields:
- String: tracerName

#### @TraceMethod  

This annotates a method definition. It wraps a method in a span or logs a method call as an event.

@TraceMethod fields: 
- String: name
- Boolean: isEvent 


#### @TraceClass 

This annotates a class definition. It wraps all public methods in a span or logs all method calls as an event.

@TraceClass fields: 
- String: name
- Boolean: isEvent
- Boolean: includeMethodName (logs method signature)


#### @TraceRestControllers

This annotates a class definition with @RestController annotation. It creates a new span for rest controllers when a request is received. 

No @TraceRestControllers fields: 
- Default span name: controllerClassName


#### @InjectTraceRestTemplate

Inject span context to all requests using RestTemplate.

@InjectTraceRestTemplate fields: 
- Boolean: isEventLogged (default true) 

#### @TraceHibernateDatabase

This annotates a hibernate entity. It wraps all database calls using the Hibernate framework in a span or logs it as an event.

@TraceHibernateDatabase fields: 
- String: name
- Boolean: isEvent 


### Example Usage

 
#### @ConfigTracer

```java
@ConfigTracer(name="tracerName")
@SpringBootApplication
public class SecondServiceApplication {

    public static void main(String[] args) throws IOException {
	SpringApplication.run(SecondServiceApplication.class, args);
    }

}
```
 
#### @TraceMethod  


```java
@TraceMethod(name="methodName", isEvent=False)
@GetMapping
public String callSecondTracedMethod() {
return "It's time to get a watch";
}
```


#### Add @TraceClass 
 

```java
@TraceClass(name="className", isEvent=True)
@RestController
public class SecondServiceController {

    @GetMapping
    public String callSecondTracedMethod() {
    return "It's time to get a watch";
    }
}
```

#### @TraceRestControllers


```java
@TraceRestControllers 
@SpringBootApplication
public class SecondServiceApplication {

	public static void main(String[] args) throws IOException {
		SpringApplication.run(SecondServiceApplication.class, args);
	}
}
```


#### @InjectTraceRestTemplate


```java
@InjectTraceRestTemplate 
@SpringBootApplication
public class SecondServiceApplication {

	public static void main(String[] args) throws IOException {
		SpringApplication.run(SecondServiceApplication.class, args);
	}
}
```

#### @TraceHibernateDatabaseCalls

```java
@TraceDatabase(name="entityName") 
@Entity
public class Book {
 
    @Id
    @GeneratedValue
    private Long id;
    private String name;
 
    // standard constructors
 
    // standard getters and setters
}
```
