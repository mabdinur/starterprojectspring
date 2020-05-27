package starterproject.foodsupplier;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import starterproject.foodsupplier.opencensus.OpenCensusGCP;


@SpringBootApplication
public class FoodSupplierApplication {

	public static void main(String[] args) throws IOException {
		OpenCensusGCP.createAndRegisterGoogleCloudPlatform();
		SpringApplication.run(FoodSupplierApplication.class, args);
	}

}
