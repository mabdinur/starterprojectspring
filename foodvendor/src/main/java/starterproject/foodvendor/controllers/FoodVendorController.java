package starterproject.foodvendor.controllers;

import java.util.List;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.Tracer;
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
	private static final Logger LOG = Logger.getLogger(FoodVendorController.class.getName());
	
	@Autowired
	private FoodVendorService foodVendorService;
	@Autowired
    Tracer tracer;
	
	@PostMapping
	public List<VendorInventory> getIngredientFromVendors(@RequestBody List<Vendor> vendors, @RequestParam String ingredientName)
	{
		Span span = tracer.getCurrentSpan();
		span.addEvent("FoodVendorController getIngredientFromVendors");
		LOG.info("FoodVendorController /foodvendor/vendors called span starts");
		
		List<VendorInventory> vendorInventory = null;
        try{
        	vendorInventory = foodVendorService.getIngredientFromVendors(vendors, ingredientName);
        } catch (Exception e) {
        	span.setStatus(Status.ABORTED);
            span.addEvent("Error while calling service");
            LOG.severe(String.format("Error while calling service: %s", e.getMessage()));
		}
       
        return vendorInventory;
	}
	
}