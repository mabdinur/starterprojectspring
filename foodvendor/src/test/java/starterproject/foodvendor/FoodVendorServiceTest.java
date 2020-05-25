package starterproject.foodvendor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import starterproject.foodvendor.data.Ingredient;
import starterproject.foodvendor.data.Vendor;
import starterproject.foodvendor.data.VendorInventory;
import starterproject.foodvendor.services.FoodVendor;
import starterproject.foodvendor.services.FoodVendorService;

@RunWith(SpringRunner.class)
public class FoodVendorServiceTest {
 
	private static final String VENDOR_NAME = "shop1";
    private static final String INGREDIENT_NAME = "item1";

    private static final float PRICE = 10;
    private static final float QUANTITY = 1;

    @Test
    public void testGetVendorsByIngredient()
    {
        FoodVendor foodVendor = new FoodVendorService();
        List<Vendor> vendors = Arrays.asList( new Vendor(VENDOR_NAME));
        List<VendorInventory> inventories =
        		foodVendor.getIngredientFromVendors(vendors, INGREDIENT_NAME);
        
        VendorInventory inventory = inventories.get(0);
        Vendor vendor = inventory.getVendor();
        Ingredient ingredient = inventory.getIngredient(INGREDIENT_NAME);
        
        assertEquals(vendor.getName(), VENDOR_NAME);
        assertNotNull(ingredient);
        assertEquals(ingredient.getPrice(), PRICE, 0);
        assertEquals(ingredient.getQuantity(), QUANTITY, 0);
    } 
}
