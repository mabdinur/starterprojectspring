package starterproject.foodvendor.controllers;


import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import starterproject.foodvendor.data.Vendor;
import starterproject.foodvendor.data.VendorInventory;
import starterproject.foodvendor.services.FoodVendorService;

/**
 * Serves vendor ingredient data
 */ 
@RestController
@RequestMapping(value = "/foodvendor/vendors")
public class FoodVendorController
{
	@Autowired
	private FoodVendorService foodVendorService;
	
	@PostMapping
	public List<VendorInventory> getVendorsByIngredient(@RequestBody List<Vendor> vendors, @RequestParam String ingredientName)
	{
		return foodVendorService.getIngredientFromVendors(vendors, ingredientName);
	}
	
}