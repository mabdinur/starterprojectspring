package starterproject.foodsupplier;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import starterproject.foodsupplier.data.Vendor;
import starterproject.foodsupplier.services.FoodSupplier;
import starterproject.foodsupplier.services.FoodSupplierService;


public class FoodSupplierServiceTest {

  private static final String VENDOR = "shop1";

  private static final String INGREDIENT_NAME = "item1";

  @Test
  public void testGetVendorsByIngredient() {
    FoodSupplier foodSupplier = new FoodSupplierService();
    Vendor test_vendor = new Vendor(VENDOR);
    List<Vendor> storesWithIngredient = foodSupplier.getVendorsByIngredient(INGREDIENT_NAME);
    assertTrue(storesWithIngredient.stream().anyMatch(vendor -> vendor.equals(test_vendor)));
  }
}
