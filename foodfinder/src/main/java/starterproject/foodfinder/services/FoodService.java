package starterproject.foodfinder.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import starterproject.foodfinder.data.Vendor;
import starterproject.foodfinder.data.VendorInventory;

/**
 * Sends requests to foodsupplier and foodvendor microservices
 *
 */
@Service
public class FoodService {
	private static final String SCHEME = "http";
	
	private static final String FOOD_SUPPLIER_ADDRESS = "foodsupplier.address";
	private static final String FOOD_SUPPLIER_PATH = "foodsupplier.path";

	private static final String FOOD_VENDOR_ADDRESS = "foodvendor.address";
	private static final String FOOD_VENDOR_PATH = "foodvendor.path";
	
	private static final String INGREDIENT_NAME_PARAM = "ingredientName";
    
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
    private Environment env;
    
    public Vendor[] getVendorsByIngredient(String ingredientName) throws Exception {
    	 String url = getUrl(FOOD_SUPPLIER_ADDRESS, FOOD_SUPPLIER_PATH, ingredientName);
        
    	 ResponseEntity<Vendor[]> response = restTemplate.
        		 exchange(url, HttpMethod.GET, null, Vendor[].class);
    	 
    	 return response.getBody(); 
    }
    
    public VendorInventory[] getIngredientFromVendors(Vendor[] vendors, String ingredientName) throws Exception {
        String url = getUrl(FOOD_VENDOR_ADDRESS, FOOD_VENDOR_PATH, ingredientName);
        
        HttpEntity<Vendor[]> request = new HttpEntity<>(vendors);
        
        ResponseEntity<VendorInventory[]> response = restTemplate.
        		exchange(url, HttpMethod.POST, request, VendorInventory[].class);
 
        return response.getBody();
    }
    
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
       return builder.build();
    }
    
    private String getUrl(String service_type, String path_name, String ingredientName) {
    	String ipAddress = env.getProperty(service_type);
    	String path = env.getProperty(path_name);
    	
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
			      .scheme(SCHEME).host(ipAddress).path(path)
			      .queryParam(INGREDIENT_NAME_PARAM, ingredientName);
    	
    	return builder.toUriString();
    }
}