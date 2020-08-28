package starterproject.timeservice;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import io.opentelemetry.context.Scope;
import io.opentelemetry.extensions.auto.annotations.WithSpan;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;

@SpringBootApplication
public class TimeServiceApplication {

  public static void main(String[] args) throws IOException {
    SpringApplication.run(TimeServiceApplication.class, args);
  }

  @RestController
  @RequestMapping(value = "/time")
  public class TimeServiceController {
     @Autowired
     private Tracer tracer;

     @GetMapping
     public String time() {
        withSpanMethod();

        Span span = tracer.spanBuilder("time").startSpan();
        try (Scope scope = tracer.withSpan(span)) {
           span.addEvent("TimeServiceController Entered");
           span.setAttribute("what.am.i", "Tu es une legume");
           return "It's time to get a watch";
        } finally {
           span.end();
        }
     }
     
     @WithSpan(kind=Span.Kind.SERVER)
     public void withSpanMethod() {}
  }
  
  @Bean
  public RestTemplate restTemplate() {
	  return new RestTemplate();
  }
}
