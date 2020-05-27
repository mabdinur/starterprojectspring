package starterproject.foodvendor.controllers;

import java.util.List;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.opencensus.common.Scope;
import io.opencensus.trace.Span;
import io.opencensus.trace.Status;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
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
	private static final Tracer tracer = Tracing.getTracer();
	private static final Logger LOG = Logger.getLogger(FoodVendorController.class.getName());
	@Autowired
	private FoodVendorService foodVendorService;
	
	@PostMapping
	public List<VendorInventory> getIngredientFromVendors(@RequestBody List<Vendor> vendors, @RequestParam String ingredientName)
	{
		Span span = tracer.getCurrentSpan();
		span.addAnnotation("FoodVendorController getIngredientFromVendors");
		LOG.info("FoodVendorController /foodvendor/vendors called span starts");
		
        try (Scope ws = tracer.withSpan(span)) {
        	return foodVendorService.getIngredientFromVendors(vendors, ingredientName);
        } catch (Exception e) {
        	span.setStatus(Status.ABORTED);
            span.addAnnotation("Error while calling service");
            LOG.severe(String.format("Error while calling service: %s", e.getMessage()));
		}
        finally {
        	span.end();
        }
        return null;
	}
	
}