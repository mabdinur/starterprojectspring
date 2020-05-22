package starterproject.foodfinder.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import starterproject.foodfinder.data.VendorInventory;
import starterproject.foodfinder.services.FoodFinderService;

/**
 * Serves vendor ingredient data
 */ 
@RestController
@RequestMapping(value = "/foodfinder/ingredient")
public class FoodFinderController
{
	@Autowired
	private FoodFinderService foodFinderService;
	
	@GetMapping
	public VendorInventory[] getVendorsByIngredient(@RequestParam String ingredientName)
	{
		return foodFinderService.getIngredient(ingredientName);
	}
	
}