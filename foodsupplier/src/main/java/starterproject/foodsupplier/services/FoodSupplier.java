package starterproject.foodsupplier.services;

import java.util.List;

import starterproject.foodsupplier.data.Vendor;

/**
 * Gets the names of all vendors with an ingredient
 */
public interface FoodSupplier
{
    public List<Vendor> getVendorsByIngredient(String ingredientName);
}
