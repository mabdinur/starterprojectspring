# OpenTelemetry Manual Instrumentation: Spring and SpringBoot

The goal of this project is to streamline the manual instrumentation process for Spring and Springboot to make it possible for users to enable tracing for requests, and database calls with a few edits to user code. The scope of this project is not fully automated instrumentation, it is providing users with better tools to instrument their own code. 

This integration for OpenTelemetry will follow in the footsteps of the existing Spring integration in Open Census with upgrades to the functionality and improved user experience. This project will specifically address tracing with the stretch goal of supporting metrics. 

## Current Manual Instrumentation Process

A sample user journey for manual instrumentation can be found on [lightstep](https://docs.lightstep.com/otel/getting-started-java-springboot). In this example we will create two spring web services using SpringBoot. Then we will trace the requests between these services using OpenTelemetry. Then we will discuss improvements that can be made to the process.  


### Create two Spring Projects

Using https://start.spring.io/ create two spring projects using maven, SpringBoot 2.3, Java, and the spring-web dependency. Name one project FoodFinder and the other FoodSupplier. After downloading the the two projects make sure to include the OpenTelemetry dependencies listed below. 


### Include Dependencies
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
<dependency>
	<groupId>io.opentelemetry</groupId>
	<artifactId>opentelemetry-exporters-logging</artifactId>
	<version>0.2.0</version>
</dependency>
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

```java
package com.package.name;

@Configuration
@EnableAutoConfiguration (exclude={DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class OtelConfig {

    @Bean
	public Tracer otelTracer() throws Exception{
        final Tracer tracer = OpenTelemetry.getTracerFactory().get("project.name");

        SpanProcessor logProcessor = SimpleSpansProcessor.newBuilder(new LoggingExporter()).build();

		OpenTelemetrySdk.getTracerFactory(). addSpanProcessor(logProcessor);

        return tracer;
    }
}
```

The file above configures an OpenTelemetry tracer which can be Autowired in a springboot project. It also adds a span processor to the OpenTelemetrySdk to export logs. The LoggingExporter will log spans to console, giving more visibility into the creation of spans. In a similar fashion, one could add other exporters such as a JaegerExporter to visualize traces on different back-ends. Similar to how the LogExporter is configured above the Jaeger configuration can be added to the OtelConfig class. 

Sample configuration for a JaegerExporter:

```java
SpanProcessor jaegerProcessor =
        SimpleSpansProcessor.newBuilder(JaegerGrpcSpanExporter.newBuilder()
            .setServiceName("otel_foodservices") 
            .setChannel(ManagedChannelBuilder.forAddress(
                    "localhost", 14250).usePlaintext().build())
            .build()).build();

OpenTelemetrySdk.getTracerFactory().addSpanProcessor(jaegerProcessor);
```

After creating and fully configuring the FoodFinder and FoodVendor projects you can view your traces on Jaeger. 

     
### Project Background

Here we will create a rest controller for the FoodFinder project. This controller will receive a request containing an ingredient name then send an http request to FoodSupplier.

FoodSupplier contains a list of a ingredients and maps it to different vendors. In this example FoodFinder will query FoodSupplier for the ingredient "milk" and FoodSupplier will return a list of suppliers who have this item. In this case "FooShop" and "BarShop" contain this item.  FoodSupplier will then returns a list containing the names "FooShop" and "BarShop". 



#### Setup FoodFinder spring project:

1. Add OpenTelemetry Dependencies
2. Add OpenTelemetry Configuration
3. Add SpringBoot main class 
4. Create a RestController for FoodFinder
5. Start a span to wrap the FoodFinderController
6. Configure HttpUtils.callEndpoint to inject span context into request
7. This is key to propagate the trace to the FoodSupplier


```java
@SpringBootApplication
public class FoodFinderApplication {

	public static void main(String[] args) throws IOException {
		SpringApplication.run(FoodFinderApplication.class, args);
	}
}
```


```java
@RestController
@RequestMapping(value = "/ingredient")
public class FoodFinderController
{
	@Autowired
	private Tracer tracer;
	
	@Autowired
	HttpUtils httpUtils;
	
    	private static String FS_URL = "localhost:8081/ingredient/milk";
		
	@GetMapping
	public String getMilk()
	{
		Span span = tracer.spanBuilder("ingredient").startSpan();
       span.addEvent("Controller Entered");
       span.setAttribute("ingredient.name", "milk");

       try(Scope scope = tracer.withSpan(span)){
           return httpUtils.callEndpoint(FS_URL, HttpMethod.GET);
       }
       catch(Exception e){
           span.addEvent("error");
           span.setAttribute("error", true);
           return "MILK VENDORS CAN'T BE FOUND";
       }
       finally{
           span.end();
       }
}
```

HttpUtils is a helper class which injects the span context into request headers. For this example I used Spring's RestTemplate to send requests from FoodFinder and FoodSupplier. A similar approach can be used with popular Java Web Clients such as okhttp and apache http client. The key is to override the put method in HttpTextFormat.Setter<?> to handle your request format. HttpTextFormat.inject will use this setter to set the traceparent and tracestate fields in your request. These values will be used to propagate your span context to external services.


```java
@Component
public class HttpUtils {
    
    @Autowired
    private Tracer tracer;
    
    private HttpTextFormat<SpanContext> textFormat;
    private HttpTextFormat.Setter<HttpHeaders> setter;
    private RestTemplate restTemplate;
    
    public HttpUtils(Tracer tracer) {
    	textFormat = tracer.getHttpTextFormat();
        setter = new HttpTextFormat.Setter<HttpHeaders>() {
            @Override
            public void put(HttpHeaders headers, String key, String value)
            {
            	headers.set(key, value);
            }
        };
        
        restTemplate  = new RestTemplate();
    }
   
    public String callEndpoint(String url) throws Exception {
	HttpHeaders headers = new HttpHeaders();
        
	Span currentSpan = tracer.getCurrentSpan();
        textFormat.inject(currentSpan.getContext(), headers, setter);
        
        HttpEntity<String> entity = new HttpEntity<String>(headers);
		
	ResponseEntity<String> response = restTemplate.
       		 exchange(url, HttpMethod.GET, entity, String.class);

	return response.getBody(); 
    }
}
```

#### Setup FoodSupplier spring project:

1. Add OpenTelemetry Dependencies
2. Add OpenTelemetry Configuration
3. Add SpringBoot main class 
4. Create a RestController for FoodSupplier
5. Start a span to wrap the FoodSupplierController

**Note: The default port for the Apache Tomcat is 8080. On localhost both FoodFinder and FoodSupplier services will attempt to run on this port raising an error. To avoid this add `server.port=8081` to the application.properties in the Springboot resource directory. Ensure the port used corresponds to port referenced by FoodFinderController.FS_URL. **
  

```java
@SpringBootApplication
public class FoodSupplierApplication {

	public static void main(String[] args) throws IOException {
		SpringApplication.run(FoodSupplierApplication.class, args);
	}
}
```


```java
@RestController
@RequestMapping(value = "/ingredient/milk")
public class FoodSupplierController
{
    @Autowired
    private Tracer tracer;
		
    @GetMapping
    public String getMilkVendors()
    {
       Span span = tracer.spanBuilder("ingredient").startSpan();
       span.addEvent("FoodSupplierController Entered");
       span.setAttribute("vendor.ingredient", "milk");

       try(Scope scope = tracer.withSpan(span)){
           return new String [] {"FooShop", "BarShop"}
       }
       finally{
           span.end();
       }
    }
}
```

#### Run FoodFinder and FoodSupplier:

***Ensure either LogExporter or Jaeger is configured in the OtelConfig.java file and is running*** 
 
Run FoodFinder and FoodSupplier from command line or using an IDE (ex. Eclipse). The end point for FoodFinder should be localhost:8080/ingredient and for FoodSupplier, localhost:8081/ingredient/milk. Entering `localhost:8080/ingredient` in a browser should call FoodFinder and then FoodVendor, creating a trace.
 
To view traces on the Jaeger UI add the Jaeger Exporter to both FoodFinder and FoodVendor projects. Deploy the Jaeger Exporter on localhost by runnning the command `docker run --rm -it --network=host jaegertracing/all-in-one` in terminal. Then send a sample request to the FoodFinder service. 


After running Jaeger locally, refresh the UI and view the exported traces from the two web services. Congrats, you created a distributed service with OpenTelemetry!


### otel-example 

In the otel-example/ directory you can find 3 food services (FoodFinder, FoodSupplier, and FoodVendor). This example is an extension of the example presented above. FoodFinder queries FoodSupplier and then queries FoodVendor to retrieve ingredient quantity, price and the vendor that carries the item. 

FoodSupplier retrieves the names of vendors with a specific ingredient return a list of Vendor names. FoodVendor then takes that list of vendor names and the desired ingredient. It then maps the vendor and ingredient names to item data (ex. quantity, price, currency) and returns the corresponding inventory for each vendor. The list of vendor inventories is then returned by FoodFinder. 

Unlike in the simplified example above, these services do not create spans in an individual controller instead a HandlerInterceptor is initialized which wraps all controllers in a span. To propagate the span context in a request HttpUtils is still used and this functionality was expanded to support PUT requests as well (adding a body to a request).  

FoodFinder is configured to run on port 8080, FoodSupplier on 8081, and FoodVendor on 8082. You can download and run the three services.  

Sample request: localhost:8080/foodfinder/ingredient?ingredientName=item3

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

Note: This data is read from a json file. Ingredient data is stored in [foodvendor](foodvendor/src/main/resources/vendors.json) and vendors data is stored in [foodsupplier](foodsupplier/src/main/resources/suppliers.json).


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
@ConfigTracer 
@SpringBootApplication
public class FoodSupplierApplication {

	public static void main(String[] args) throws IOException {
		SpringApplication.run(FoodSupplierApplication.class, args);
	}
}
```

This annotation will use this method:
`OpenTelemetry.getTracerFactory().get("tracerName")`
 
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

This annotation adds wraps all RestControllers in a span using HandlerInterceptors. An example of initializing HandlerInterceptors can be seen [here](foodfinder/src/main/java/starterproject/foodfinder/telemetry/InterceptorConfig.java). 

This annotation will contain the @ConfigTracer functionality. ***I am not sure if this will cause confusion, please advise?*** 

Example Usage:

```java
@TraceControllers 
@SpringBootApplication
public class FoodSupplierApplication {

	public static void main(String[] args) throws IOException {
		SpringApplication.run(FoodSupplierApplication.class, args);
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

To replace the HttpUtils class and abstract the injection of the span context into request headers. Similar to the proposed @TraceControllers, this annotation will use an interceptor. Using the ClientHttpRequestInterceptor I will propagate the current span context to  all external requests which use RestTemplate. The core of the proposed functionality can be seen here: (RestClientConfig)[foodfinder/src/main/java/starterproject/foodfinder/telemetry/RestClientConfig.java] and (RestTemplateHeaderModifierInterceptor.java)[foodfinder/src/main/java/starterproject/foodfinder/telemetry/RestTemplateHeaderModifierInterceptor.java]

This is a first attempt and I will experiment with scaling this functionality to handle other web clients such as Apache Http Client, and gRPC. ***Please advice on this approach***


@TraceRestTemplate fields: 
- No proposed Fields as of now


### Alternative to @TraceRestTemplate

One challenge to improving user experience is context propagation. As of now OpenTelemetry does not support popular web clients such as gRPC, Spring's RestTemplate or Apache Http Client. The only documentation I can find on context propagation involves manually injecting a span context into a request header. I used this approach in an example in the helper method HttpUtils.call_endpoint to propagate the span context from FoodFinder to FoodSupplier. In my proposed @TraceRestTemplate annotation I will provide this functionality to users. However it will only support one framework in a limited fashion.

Open Tracing has library instrumentation for injecting and extracting the span context in to a payload as can be seen below:

(ava-web-servlet-filter)[https://github.com/opentracing-contrib/java-web-servlet-filter]  (inject)

java-okhttp: https://github.com/opentracing-contrib/java-okhttp (extract)
java-apache-httpclient: https://github.com/opentracing-contrib/java-apache-httpclient (extract)
java-asynchttpclient: https://github.com/opentracing-contrib/java-asynchttpclient (extract)
Spring’s RestTemplate: https://github.com/opentracing-contrib/java-spring-web/tree/master/opentracing-spring-web (extract)

Support for one or two of these Web Clients should be added in OpenTelemetry as well. This would improve the user friendliness of span propagation. This approach would make my proposed @TraceRestTemplate obsolete, however it seems to be a better approach. 

I can pick up this work after adding the annotations. Unless someone is already working on this. If so, I can pitch in to help :)
