package starterproject.foodfinder.services;

import org.json.simple.JSONObject;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import starterproject.foodfinder.data.Vendor;
import starterproject.foodfinder.data.VendorInventory;
import starterproject.foodfinder.helpers.JSONReaderHelper;

@Service
public class FoodService {

	private static final String SERVICES_FILE = "remote_services.json";
	
	private static final String FOOD_SUPPLIER_IP = "foodsupplier_ip";
	private static final String FOOD_SUPPLIER_PATH = "foodsupplier_path";
	
	private static final String FOOD_VENDOR_IP = "foodvendor_ip";
	private static final String FOOD_VENDOR_PATH = "foodvendor_path";
	
	private static final String INGREDIENT_NAME_PARAM = "ingredientName";
	
    private final RestTemplate restTemplate;
    
    private JSONObject serviceToIP;

    public FoodService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
        this.serviceToIP = JSONReaderHelper.getData(SERVICES_FILE);
    }

    public ResponseEntity<Vendor[]> getVendorsByIngredient(String ingredientName) {
    	 String url = getUrl(FOOD_SUPPLIER_IP, FOOD_SUPPLIER_PATH, ingredientName);
        
         ResponseEntity<Vendor[]> response = restTemplate.
        		 exchange(url, HttpMethod.GET, null, Vendor[].class);
        		 
        return response; 
    }
    
    public ResponseEntity<VendorInventory[]> getIngredientFromVendors(Vendor[] vendors, String ingredientName) {
        String url = getUrl(FOOD_VENDOR_IP, FOOD_VENDOR_PATH, ingredientName);
        
        HttpEntity<Vendor[]> request = new HttpEntity<>(vendors);
        
        ResponseEntity<VendorInventory[]> response = restTemplate.
        		exchange(url, HttpMethod.POST, request, VendorInventory[].class);
        
        return response;
    }
    
    private String getUrl(String service_type, String path_name, String ingredientName) {
    	String ipAddress = (String) serviceToIP.get(service_type);
    	String path = (String) serviceToIP.get(path_name);
    	
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
			      .scheme("http").host(ipAddress).path(path)
			      .queryParam(INGREDIENT_NAME_PARAM, ingredientName);
    	
    	return builder.toUriString();
    }
}