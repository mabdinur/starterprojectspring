package starterproject.foodsupplier.controllers;


import java.util.List;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.Tracer;

import starterproject.foodsupplier.data.Vendor;
import starterproject.foodsupplier.services.FoodSupplierService;

/**
 * Maps vendors to available ingredients ingredients
 */
@RequestMapping("/foodsupplier/vendors")
@RestController
public class FoodSupplierController {
  private static final Logger LOG = Logger.getLogger(FoodSupplierController.class.getName());

  @Autowired
  private FoodSupplierService foodSupplierService;
  @Autowired
  Tracer tracer;

  @GetMapping
  public List<Vendor> getVendorsByIngredient(@RequestParam String ingredientName) {
    Span span = tracer.getCurrentSpan();
    span.addEvent("FoodSupplierController getVendorsByIngredient");
    LOG.info("FoodSupplierController /foodsupplier/vendors called span starts");

    List<Vendor> vendors = null;
    try {
      vendors = foodSupplierService.getVendorsByIngredient(ingredientName);
    } catch (Exception e) {
      span.setStatus(Status.ABORTED);
      span.addEvent("Error while calling service");
      LOG.severe(String.format("Error while calling service: %s", e.getMessage()));
    }

    return vendors;
  }
}
