package starterproject.mainservice;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class MainServiceApplication {

  public static void main(String[] args) throws IOException {
    SpringApplication.run(MainServiceApplication.class, args);
  }
  
  @RestController
  @RequestMapping(value = "/message")
  public class MainServiceController {
     private static final String TIME_SERVICE_URL = "http://localhost:8080/time";
     
     @Autowired
     private RestTemplate restTemplate;

     @GetMapping
     public String message() {
        return restTemplate.exchange(TIME_SERVICE_URL, HttpMethod.GET, null, String.class).getBody();
     }
    
    @Bean
    public RestTemplate restTemplate() {
  	  return new RestTemplate();
    }
  }
}