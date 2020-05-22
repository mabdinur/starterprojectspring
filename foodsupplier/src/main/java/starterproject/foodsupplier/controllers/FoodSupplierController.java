package starterproject.foodsupplier.controllers;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import starterproject.foodsupplier.data.Vendor;
import starterproject.foodsupplier.services.FoodSupplierService;

/**
 * Maps vendors to available ingredients ingredients
 */ 
@RequestMapping("/foodsupplier/vendors")
@RestController
public class FoodSupplierController
{
	@Autowired
	private FoodSupplierService foodSupplierService;
  
	@GetMapping
    public List<Vendor> getVendorsByIngredient(@RequestParam String ingredientName)
    {
        return foodSupplierService.getVendorsByIngredient(ingredientName);
    }
}
