package starterproject.foodvendor.services;

import java.util.List;

import starterproject.foodvendor.data.VendorInventory;
import starterproject.foodvendor.data.Vendor;

/**
 * Gets the names of all vendors with an ingredient
 */
public interface FoodVendor {

  public List<VendorInventory> getIngredientFromVendors(List<Vendor> vendors, String itemName);
}
