# OpenTelemetry Manual Instrumentation: Spring and SpringBoot

The goal of this project is to streamline the manual instrumentation process for Spring and Springboot to make it possible for users to enable tracing for requests, and database calls with a few edits to user code. The scope of this project is not fully automated instrumentation, it is providing users with better tools to instrument their own code. 

This integration for OpenTelemetry will follow in the footsteps of the existing Spring integration in Open Census with upgrades to the functionality and improved user experience. This project will specifically address tracing with the stretch goal of supporting metrics. 

## Current Manual Instrumentation Process

A sample user journey for manual instrumentation can be found on [lightstep](https://docs.lightstep.com/otel/getting-started-java-springboot). In this example we will create two spring web services using SpringBoot and trace the requests between these services using OpenTelemetry. As you can see from the example below, this process involves adding tracers into the application code and making  


### Create two Spring Projects

Using https://start.spring.io/ create two spring projects, one named FoodFinder and the other named FoodSupplier. Before downloading the project make sure to add spring-web as a dependency. 


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

The dependencies listed above need to be added to the pom.xml file to enable OpenTelemtry tracing and metrics in your application   

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

        OpenTelemetrySdk.getTracerFactory().addSpanProcessor(logProcessor);

        return tracer;
    }
}
```

The file above contains configures an OpenTelemetry tracer which can be AutoWired to components in a spring project. It also adds a span processor to export logs to the console. The LoggingExporter will print spans to console, giving more visibility to the creation of a spans. In a similar fashion, one could add other exporters such as a JaegarExporter to visualize traces on different backends. 

Sample configuration for a JaegerExporter:

```java
SpanProcessor jaegerProcessor =
        SimpleSpansProcessor.newBuilder(JaegerGrpcSpanExporter.newBuilder()
            .setServiceName("otel_foodservices") 
            .setChannel(ManagedChannelBuilder.forAddress(
                    "34.66.234.123", 14250).usePlaintext().build())
            .build()).build();

OpenTelemetrySdk.getTracerFactory().addSpanProcessor(jaegerProcessor);
```

Use the command `docker run -d -p 6831:6831/udp -p 16686:16686 -p 14250:14250 jaegertracing/all-in-one:latest` to run the Jaeger Exporter. After creating and running the FoodFinder and FoodVendor services you can view your traces at localhost:6831 using Jaeger's UI.

     
### Add Rest Controllers to Spring Applications

Here I will add a Rest Controller called FoodFinder. This controller receives a request containing an ingredient name then send another http request to an external service. We will call this second service FoodSupplier. 

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
    private static Tracer tracer;
    private static String FS_URL = "localhost:8081/ingredient/milk";
		
	@GetMapping
	public String getMilk()
	{
		Span span = tracer.spanBuilder("ingredient").startSpan();
       span.addEvent("Controller Entered");
       span.setAttribute("ingredient.name", "milk");

       try(Scope scope = tracer.withSpan(span)){
           return HttpUtils.callEndpoint(FS_URL, HttpMethod.GET);
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
}
```

```
public class HttpUtils {
    
    @Autowired
    private static Tracer tracer;
    
    private static final HttpTextFormat<SpanContext> textFormat = tracer.getHttpTextFormat();
    private static final HttpTextFormat.Setter<HttpURLConnection> setter =
            new HttpTextFormat.Setter<HttpURLConnection>() {
      public void put(HttpURLConnection carrier, String key, String value) {
        carrier.setRequestProperty(key, value);
      }
    };

	public static String callEndpoint(String url, Serializable requestBody, HttpMethod method) throws Exception {
 
        Span span = tracer.getCurrentSpan();
        span.addEvent("Request sent to Microservice");
 
        HttpURLConnection conn = getConnectionWithSpanContext(url, method, span);
        return readResponse(conn);
    }

	private static HttpURLConnection getConnectionWithSpanContext(String url, HttpMethod method, Span span)
			throws IOException, MalformedURLException, ProtocolException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		textFormat.inject(span.getContext(), conn, setter);
		conn.setRequestMethod(method.name());
		return conn;
	}
    
    private static String readResponse(HttpURLConnection conn) throws IOException {
		StringBuilder result = new StringBuilder();
		
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line;
		while ((line = rd.readLine()) != null) {
		    result.append(line);
		}
		rd.close();
		
		return result.toString();
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
    private static Tracer tracer;
		
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
 
Run FoodFinder and FoodSupplier from command line or using an IDE (ex. Eclipse). The end point for FoodFinder is localhost:8080/ingredient and for FoodSupplier, localhost:8081/ingredient/milk. Entering localhost:8080/ingredient should call FoodFinder and then FoodVendor, creating a trace.

After running jaeger locally, refresh the UI and view the exported traces from the two web services. Congrats, you created a distributed service with OpenTelemetry!


### otel-example 

In the otel-example/ directory you can find 3 food services (FoodFinder, FoodSupplier, and FoodVendor). This example is an extension of the example presented above. FoodFinder queries FoodSupplier and then FoodVendor to retrieve ingredient quantity, price and the vendor that carries the item. FoodSupplier retrieves the names of vendors with a specific ingredient return a list of Vendor names. FoodVendor then takes that list of vendors and the desired ingredient and then returns the corresponding inventory from available vendors. The list of vendor inventories is then returned by FoodFinder. 

Unlike in the simplified example above, these services do not create spans in an individual controller instead a HandlerInterceptor is initialized which wraps all controllers in a span. To propagate the span context in a request HttpUtils is still used, however, the functionality was expanded to support PUT requests (adding a body to a request).  

FoodFinder is configured to run on port 8080, FoodSupplier on 8081, and FoodVendor on 8082. Download and run the three services from the SpringApplication main class. 

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

Ingredient data is stored in [foodvendor](foodvendor/src/main/resources/vendors.json) and vendors data is stored in [foodsupplier](foodsupplier/src/main/resources/suppliers.json).


## Proposed Improvements

### Create new spring contrib package for Open Telemetry  

```xml
 <dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-contrib-spring</artifactId>
    <version>VERSION</version>
 </dependency>
```
 
This new dependency can be added to string applications adopting opentelemetry to leverage the newly proposed manual instrumentation tools.
 
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

These new annotation with allow users to wrap methods or classes in a span. The TraceClass annotation will come with the ability to exclude private methods, and whether to include a method name in the span name/events. 

@TraceMethod fields: 
- String: name
- Boolean: isEvent (if true, creates event using method signature and adds it to a span)

@TraceClass fields: 
- String: name
- Boolean: includeMethodName (logs method signature)
- Boolean: includePrivateMethods


### Create @TraceControllers

This annotation adds wraps all RestControllers in a span using HandlerInterceptors. An example of initializing HandlerInterceptors can be seen [here](otel-example/foodfinder/src/main/java/starterproject/foodfinder/telemetry/InterceptorConfig.java). 

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

creates new span for every request. Sets name to HTTPMethod + url. If logMethodCall is true. Event: Controller Name + Method Name, is added 


### Create @TraceDatabase

This will have similar functionality to @TracedMethod. When placed on a method or class with a call to `java.sql.PreparedStatement.execute*`, this annotation will wrap that database call in a span. 



## Challenges 

One challenge that maybe beyong the scope of this package is context propagation. As of now OpenTelemetry does not support popular web clients such as gRPC or Spring's RestTemplate (which leverages the Java Servlet API). The only documentation I can find on context propagation involves using a carrier such as HttpURLConnection and injecting the span context to this connection. I used this approach in the example above where I used the helper class HttpUtils.call_endpoint to create an HttpURLConnection to propagate the span context from FoodFinder to FoodSupplier. A example show casing this usage is shown below:

```java
public class HttpUtils {
    
    @Autowired
    private static Tracer tracer;
    
    private static final HttpTextFormat<SpanContext> textFormat = tracer.getHttpTextFormat();
    private static final HttpTextFormat.Setter<HttpURLConnection> setter =
            new HttpTextFormat.Setter<HttpURLConnection>() {
      public void put(HttpURLConnection carrier, String key, String value) {
        carrier.setRequestProperty(key, value);
      }
    };

	private static HttpURLConnection addSpanContextToConnection(String 	url) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		
		Span span = tracer.getCurrentSpan();
		textFormat.inject(span.getContext(), conn, setter);
		
		return conn;
	}
    

```

Open Tracing contains support for injecting and extracting the span context in to a payload as can be seen in these contributions:

https://github.com/opentracing-contrib/java-web-servlet-filter  (injection)
java-okhttp: https://github.com/opentracing-contrib/java-okhttp (extraction)
java-apache-httpclient: https://github.com/opentracing-contrib/java-apache-httpclient (extraction)
java-asynchttpclient: https://github.com/opentracing-contrib/java-asynchttpclient (extraction)
Spring’s RestTemplate: https://github.com/opentracing-contrib/java-spring-web/tree/master/opentracing-spring-web (extraction)

Support for one or two of these Web Clients can be added in OpenTelemetry. This would improve the user friendliness of enabling span propagation. 


### Another Approach (seems easier and easier to apply to different frameworks)

Ideally users will not need to create a carrier and manually inject the span context into a request. I did some digging and found that `io.opentelemetry.propagation.HttpTraceContext.java` sets the "traceparent" to parent_trace_id and the "tracestate" to a list of trace states in the header of a request. By exposing the functionality of HttpTraceContext.inject() such that otel adopters can retrieve these key-value pairs and set these values in request headers. 

The goal would be to add a ClientHttpRequestInterceptor to otel spring projects to propagate a span context. ***I'm not sure if this approach is desirable, please advice***  


```java
@Component
public class RestTemplateHeaderModifierInterceptor implements ClientHttpRequestInterceptor {
	
	@Autowired
    Tracer tracer;
	
	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body,
			ClientHttpRequestExecution execution) throws IOException {
		
		String traceId = tracer.getCurrentSpan().getContext().getTraceId().toLowerBase16();
		
		request.getHeaders().add("traceparent", traceId);
		request.getHeaders().add("tracestate", "state1,state2");
		
		ClientHttpResponse response = execution.execute(request, body);
		
		return response;
	}
}
```
 
















