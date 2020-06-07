#OpenTelemetry Instrumentation: Spring and SpringBoot

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


# Manual Instrumentation starter guide

A sample user journey for manual instrumentation can be found on [lightstep](https://docs.lightstep.com/otel/getting-started-java-springboot). In this example we will create two spring web services using SpringBoot. Then we will trace the requests between these services using OpenTelemetry. Finally, we will discuss improvements that can be made to the process.


### Create two Spring Projects

Using https://start.spring.io/ create two spring projects. Select maven, SpringBoot 2.3, Java, and add the spring-web dependency. Name one project FirstService and the other SecondService. After downloading the two projects make sure to include the OpenTelemetry dependencies listed below. 


### Setup for FirstService and SecondService

Add the dependencies below to enable OpenTelemetry in FirstService and SecondService. The Jaeger and LoggerExporter packages are recommended but not required for this tutorial. 

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

***Note: The packages opentelemetry-exporters-logging and opentelemetry-exporters-jaeger are used to export logs. If you plan to to use a different  exporter include those packages instead***
 

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

The file above configures an OpenTelemetry tracer and a span processor which exports traces. The LoggingExporter will log spans, annotations and events to console, providing more visibility to your application. In a similar fashion, one could add another exporter such as a JaegerExporter to visualize traces on different back-ends. Similar to how the LogExporter is configured, a Jaeger configuration can be added to the OtelConfig class. 

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

Adding this Jaeger configuration to the OtelConfig.java file will enable you to view your traces on the Jaeger UI. 

     
### Project Background

Here we will create rest controllers for FirstService and SecondService.
FirstService will send a GET request to SecondService to get the current time. FirstService will return SecondSerivce's time and it will append a message. 


### Tutorial  1: Manual Instrumentation with Java SDK

#### Setup FirstService spring project:

1. Add OpenTelemetry Dependencies
2. Add OpenTelemetry Tracer Configuration
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

### Tutorial  2: Using Spring Handlers and Interceptors


Using Tutorial 1 create the FirstService and SecondService projects. Then add the required OpenTelemetry dependencies, configurations, and your chosen exporter to this. In this tutorial you will implement the Spring HandlerInerceptor interface to wrap all requests to FirstService and Second Service controllers in a span. In this tutorial we will use Spring's RestTemplate to send requests from FirstService to SecondService. To propagate the trace from FirstService to SecondService you will also implement Spring's ClientHttpRequestInterceptor interface. This implementation is only required for FirstService since this will be the only project send outbound requests (SecondService only receive requests from an external service). 


#### Setup SecondService spring project:

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

Create a ControllerTraceInterceptor class to wrap the SecondServiceController in a span. This class will call the preHandle method before the rest controller is entered and the postHandle method after. The preHandle method creates a span for the request and the postHandle method closes the span and adds the span context to the response header. This implementation is shown below:  

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

The final step is to register the ControllerTraceInterceptor in your application:


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

Now your SecondService application is complete. Create the FirstService application using the instructions below and then run your distrubted service!

#### Setup the FirstService spring project:

Create a rest controller for FirstService. This controller will a request to SecondService and then return the response to the client:

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

As seen in the setup of SecondService, create implement the TraceInterceptor interface to wrap incoming requests in a span. Then register this new handler by extending WebMvcConfigurationSupport.
In effect include copy InterceptorConfig.java and ControllerTraceInterceptor from SecondService and add it to FirstService.


In this tutorial we used Spring's RestTemplate to send requests from FirstService to SecondService. We will configure the ClientHttpRequestInterceptor to intercept all client http requests made using this framework.

To propagate the span context from FirstService to SecondService you must inject the span context into the outgoing request. In tutorial 1 this was done within the FirstServiceController. In this tutorial we will implement Spring's ClientHttpRequestInterceptor and register this interceptor in our application. Include the two classes below to your FirstService application to add this functionality.

Implement ClientHttpRequestInterceptor:

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

Register restTemplateHeaderModifierInterceptor:

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

#### Create a distributed trace 

By default SpringBoot runs a Tomcat on the default port 8080. This tutorial assumes FirstService runs on the default port (8080) and SecondService runs on port 8081. This is because the SS_URL hard coded in FirstService and the test url given below. To run SecondService on port 8081 include `server.port=8081` in your resources/application.properties file. 

Run both the FirstService and SecondService projects on localhost. The end point for FirstService should be http://localhost:8080/message and http://localhost:8081/time for SecondService. 

Enter `http:\\localhost:8080/time` in a browser to create a distributed trace. This trace should include a call to FirstService and then SecondService.

To visualize this trace add a trace exporter to one or both of your applications. Instructions on how to setup LogExporter and Jaeger can be seen in tutorial 1. You can also follow your trace using a debugger and a Java IDE. 

### Sample application with distributed tracing: otel-example 

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

Note: This data is read from a json file. Ingredient data is stored in [FoodVendor](foodvendor/src/main/resources/vendors.json) and vendors data is stored in [FoodSupplier](foodsupplier/src/main/resources/suppliers.json).
 

## Tutorial 3: Instrumentation using opentelemetry-contrib-spring (IN PROGRESS) 

### Create new spring contrib package for Open Telemetry  

This add the opentelemetry-contrib-spring package to use the annotations below:

```xml
 <dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-contrib-spring</artifactId>
    <version>VERSION</version>
 </dependency>
```

 
### @ConfigTracer

To use the other annotations below you must place @ConfigTracer on the main class of the project. This will create a tracer bean which can be injected into your components.  

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
 
### @TraceMethod  

This annotation with allow you to wrap methods in a span or event.

Example Usage:

```java
@TraceMethod
@GetMapping
public String callSecondTracedMethod() {
return "It's time to get a watch";
}
```


### @TraceClass 
 
This annotation wraps all methods in a class in a span. You can configure whether to include the name of methods in the span or whether to log the method call as an event. 

Example Usage:

```java
@TraceClass 
@RestController
public class SecondServiceController {

    @GetMapping
    public String callSecondTracedMethod() {
    return "It's time to get a watch";
    }
}
```

### @TraceRestControllers

This annotation also contains the @ConfigTracer functionality. This annotation wraps all RestControllers in a span. It also creates new span for every request and sets name to HTTPMethod + url. If the field logMethodCall is set to true the event named `controllerName + methodName`, is added to the span. 


Example Usage:

```java
@TraceRestControllers 
@SpringBootApplication
public class SecondServiceApplication {

	public static void main(String[] args) throws IOException {
		SpringApplication.run(SecondServiceApplication.class, args);
	}
}
```


### @InjectTraceRestTemplate

This annotation supports the Spring RestTemplate framework. It injects the current span context into requests to external services. If a span doesn't exist it creates one using the name field. If the name is null the default name is the http method of the request + url.


The core of the proposed functionality can be seen in tutorial 2 in the SecondService.

Example Usage:

```java
@InjectTraceRestTemplate 
@SpringBootApplication
public class SecondServiceApplication {

	public static void main(String[] args) throws IOException {
		SpringApplication.run(SecondServiceApplication.class, args);
	}
}
```

### @TraceHibernateDatabaseCalls

This annotation has similar functionality to @TracedMethod. When placed on a method or class with database calls using a Hibernate database, this will wrap that database call in a span. This span logs the status of the executed transaction as a span event.

Example usage:

```java
@TraceDatabase(name="traceName") 
public static getRecordFromDb(String sqlStatement) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection con = DriverManager.getConnection("jdbc:mysql://db.com:3306/core","username","password");
        Statement stmt = con.createStatement();
        stmt.execute(sqlStatement);
}
```


### Alternative to @InjectTraceRestTemplate (Note to self and other contributors)

One challenge to improving user experience is context propagation. As of now OpenTelemetry does not support popular web clients such as gRPC, RestTemplate or Apache Http Client. The only documentation I can find on context propagation using spring involves manually injecting a span context into a request header. In my proposed @InjectTraceRestTemplate annotation I will provide this functionality to users. However it will only support one framework and in a limited fashion.

Open Tracing has library instrumentation for injecting and extracting the span context into a payload as can be seen below:

[ava-web-servlet-filter](https://github.com/opentracing-contrib/java-web-servlet-filter)  (inject)

[java-okhttp](https://github.com/opentracing-contrib/java-okhttp) (extract)

[java-apache-httpclient](https://github.com/opentracing-contrib/java-apache-httpclient) (extract)

[java-asynchttpclient](https://github.com/opentracing-contrib/java-asynchttpclient)

[Spring’s RestTemplate](https://github.com/opentracing-contrib/java-spring-web/tree/master/opentracing-spring-web) (extract)


Support for one or two of these Web Clients should be added in OpenTelemetry as well. This would improve the user friendliness of span propagation. This approach would make my proposed @InjectTraceRestTemplate obsolete. 

I can pick up this work after adding the annotations. Unless someone is already working on this. If so, I can pitch in to help :)
