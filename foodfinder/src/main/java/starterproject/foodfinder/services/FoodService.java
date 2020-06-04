package starterproject.foodfinder.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

//import com.fasterxml.jackson.databind.ObjectMapper;

import starterproject.foodfinder.data.Vendor;
import starterproject.foodfinder.data.VendorInventory;
import starterproject.foodfinder.telemetry.HttpUtils;

/**
 * Sends requests to foodsupplier and foodvendor microservices
 *
 */
@Service
public class FoodService {
	private static final String SCHEME = "http";
	
	private static final String FOOD_SUPPLIER_ADDRESS = "foodsupplier.address";
	private static final String FOOD_SUPPLIER_PORT = "foodsupplier.port";
	private static final String FOOD_SUPPLIER_PATH = "foodsupplier.path";

	private static final String FOOD_VENDOR_ADDRESS = "foodvendor.address";
	private static final String FOOD_VENDOR_PORT = "foodvendor.port";
	private static final String FOOD_VENDOR_PATH = "foodvendor.path";
	
	private static final String INGREDIENT_NAME_PARAM = "ingredientName";
    
	
	@Autowired
    private Environment env;
    
    public Vendor[] getVendorsByIngredient(String ingredientName) throws Exception {
    	 String url = getUrl(FOOD_SUPPLIER_ADDRESS, FOOD_SUPPLIER_PORT, FOOD_SUPPLIER_PATH, ingredientName);
        
    	 String responseJson = HttpUtils.callEndpoint(url, null, HttpMethod.GET);
    	 Vendor[] vendors = new ObjectMapper().readValue(responseJson, Vendor[].class);
    	 
    	 return vendors; 
    }
    
    public VendorInventory[] getIngredientFromVendors(Vendor[] vendors, String ingredientName) throws Exception {
        String url = getUrl(FOOD_VENDOR_ADDRESS, FOOD_VENDOR_PORT, FOOD_VENDOR_PATH, ingredientName);
        
        String responseJson = HttpUtils.callEndpoint(url, vendors, HttpMethod.POST);
        VendorInventory[] vendorInventory = new ObjectMapper().readValue(responseJson, VendorInventory[].class);
        
        return vendorInventory;
    }
    
    private String getUrl(String service_type, String port_name, String path_name, String ingredientName) {
    	String ipAddress = env.getProperty(service_type);
    	String port = env.getProperty(port_name);
    	String path = env.getProperty(path_name);
    	
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
			      .scheme(SCHEME).host(ipAddress).path(path).port(port)
			      .queryParam(INGREDIENT_NAME_PARAM, ingredientName);
    	
    	return builder.toUriString();
    }
}