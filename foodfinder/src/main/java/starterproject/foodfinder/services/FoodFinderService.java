package starterproject.foodfinder.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import starterproject.foodfinder.data.Vendor;
import starterproject.foodfinder.data.VendorInventory;

/**
 * Finds the price and quantity of an ingredient from the first available vendor. Assumes at least one vendor has the
 * item in stock
 */
@Service
public class FoodFinderService implements FoodFinder
{
	@Autowired
	FoodService foodService;
	
	@Override
    public VendorInventory[] getIngredient(String ingredientName)
    {
		  ResponseEntity<Vendor[]> foodSupplierResponse = foodService.getVendorsByIngredient(ingredientName);
		  Vendor[] vendors = foodSupplierResponse.getBody();
		  ResponseEntity<VendorInventory[]> vendorInventories = foodService.getIngredientFromVendors(vendors, ingredientName);
        
		  return vendorInventories.getBody();
    }
}