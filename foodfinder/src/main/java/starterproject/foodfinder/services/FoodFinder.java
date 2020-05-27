package starterproject.foodfinder.services;

import starterproject.foodfinder.data.VendorInventory;

/**
 * Gets the names of all vendors with an ingredient
 */
public interface FoodFinder {
    
    public VendorInventory[] getIngredient(String ingredient) throws Exception;
}
