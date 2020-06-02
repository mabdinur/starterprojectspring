package starterproject.foodfinder.controllers;


import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.Tracer;
import starterproject.foodfinder.data.VendorInventory;
import starterproject.foodfinder.services.FoodFinderService;

/**
 * Serves vendor ingredient data
 */ 
@RestController
@RequestMapping(value = "/foodfinder/ingredient")
public class FoodFinderController
{
	private static final Logger LOG = Logger.getLogger(FoodFinderController.class.getName()); 
	
	@Autowired
	private FoodFinderService foodFinderService;
	@Autowired
    Tracer tracer;
	
	@GetMapping
	public VendorInventory[] getVendorsByIngredient(@RequestParam String ingredientName)
	{
		Span span = tracer.getCurrentSpan();
		span.addEvent("FoodFinderController.getVendorsByIngredient");
        LOG.info("FoodFinder Span created");
        
        VendorInventory[] vendorInventory = null;
        try{
        	vendorInventory = foodFinderService.getIngredient(ingredientName);
        }catch(Exception e) {
        	span.setStatus(Status.ABORTED);
            span.addEvent("Error while calling service");
            LOG.severe(String.format("Error while calling service: %s", e.getMessage()));
		}
        
        return vendorInventory;
	}
	
	@PutMapping
	@RequestMapping(value = "error")
	public String error()
	{
		Span span = tracer.getCurrentSpan();
        
		try{
        	throw new Exception("Throw Test Exception. Test error request sent");
        } catch(Exception e) {
        	span.setStatus(Status.ABORTED);
        	span.addEvent("ERROR THROWN in /error");
        	LOG.severe(String.format("Error while calling service: %s", e.getMessage()));
        }
        
        return "ERROR";
	}
}