package starterproject.foodvendor;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import starterproject.foodvendor.opencensus.OpenCensusGCP;

@SpringBootApplication
public class FoodVendorApplication {

	public static void main(String[] args) throws IOException {
		OpenCensusGCP.createAndRegisterGoogleCloudPlatform();
		SpringApplication.run(FoodVendorApplication.class, args);
	}

}
