package starterproject.foodfinder;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import starterproject.foodfinder.opencensus.StackDriverGCP;


@SpringBootApplication
public class FoodFinderApplication {

	public static void main(String[] args) throws IOException {
		StackDriverGCP.createAndRegisterGoogleCloudPlatform();
		SpringApplication.run(FoodFinderApplication.class, args);
	}
}
