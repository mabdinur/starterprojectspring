package starterproject.foodvendor.services;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

import starterproject.foodvendor.data.Ingredient;
import starterproject.foodvendor.data.VendorInventory;
import starterproject.foodvendor.data.Vendor;
import starterproject.foodvendor.helpers.JSONReaderHelper;

/**
 * Maps vendors to available ingredients ingredients
 */ 
@Service
public class FoodVendorService implements FoodVendor
{
    private static final String PRICE = "price";
    private static final String QUANTITY = "quantity";
    private static final String CURRENCY = "currency";
    private static final String VENDOR_FILE = "vendors.json";

    private JSONObject vendorsToItemsJson = new JSONObject();

    public FoodVendorService()
    {
        vendorsToItemsJson = JSONReaderHelper.getData(VENDOR_FILE);
    }
    
    @Override
    public List<VendorInventory> getIngredientFromVendors(List<Vendor> vendors, String itemName)
    {
    	List<VendorInventory> inventories = new ArrayList<VendorInventory>();
    	
    	for (Vendor vendor : vendors) {
    		Ingredient ingredient = getIngredient(itemName, vendor.getName());
    		VendorInventory vendorInventory = new VendorInventory(vendor, ingredient);
    		inventories.add(vendorInventory);
    	}
        
        return inventories;
    }

	private Ingredient getIngredient(String itemName, String vendorName) {
		JSONObject vendorItemsJson = (JSONObject) vendorsToItemsJson.get(vendorName);
		JSONObject itemJson = (JSONObject) vendorItemsJson.get(itemName);
    
		Long quantity = (Long) itemJson.get(QUANTITY);
		Long price = (Long) itemJson.get(PRICE);
		String currency = (String) itemJson.get(CURRENCY);
		
		Ingredient ingredient = new Ingredient(itemName, price, quantity, currency);
		return ingredient;
	}
}