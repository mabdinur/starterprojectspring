package starterproject.foodfinder.controllers;


import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.opencensus.trace.Span;
import io.opencensus.trace.SpanBuilder;
import io.opencensus.trace.Status;
import io.opencensus.trace.Tracing;
import io.opencensus.common.Scope;
import io.opencensus.trace.Tracer;
import starterproject.foodfinder.data.VendorInventory;
import starterproject.foodfinder.opencensus.SpanUtils;
import starterproject.foodfinder.services.FoodFinderService;

/**
 * Serves vendor ingredient data
 */ 
@RestController
@RequestMapping(value = "/foodfinder/ingredient")
public class FoodFinderController
{
	private static final Tracer TRACER = Tracing.getTracer();
	private static final Logger LOG = Logger.getLogger(FoodFinderController.class.getName()); 
	
	@Autowired
	private FoodFinderService foodFinderService;
	
	@GetMapping
	public VendorInventory[] getVendorsByIngredient(@RequestParam String ingredientName)
	{
		Span span = TRACER.getCurrentSpan();
		span.addAnnotation("FoodFinderController.getVendorsByIngredient");
        LOG.info("FoodFinder Span created");
        
        VendorInventory[] vendorInventory = null;
        try{
        	vendorInventory = foodFinderService.getIngredient(ingredientName);
        }catch(Exception e) {
        	span.setStatus(Status.ABORTED);
            span.addAnnotation("Error while calling service");
            LOG.severe(String.format("Error while calling service: %s", e.getMessage()));
		}
        
        return vendorInventory;
	}
	
	@PutMapping
	@RequestMapping(value = "error")
	public String error()
	{
		Span span = TRACER.getCurrentSpan();
        
		try{
        	throw new Exception("Throw Test Exception. Test error request sent");
        } catch(Exception e) {
        	span.setStatus(Status.ABORTED);
        	span.addAnnotation("ERROR THROWN in /error");
        	LOG.severe(String.format("Error while calling service: %s", e.getMessage()));
        }
        
        return "ERROR";
	}
}