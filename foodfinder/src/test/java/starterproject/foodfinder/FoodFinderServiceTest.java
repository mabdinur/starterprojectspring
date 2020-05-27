package starterproject.foodfinder;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import starterproject.foodfinder.data.Vendor;
import starterproject.foodfinder.data.VendorInventory;
import starterproject.foodfinder.services.FoodFinderService;
import starterproject.foodfinder.services.FoodService;
import starterproject.foodfinder.data.Ingredient;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FoodFinderServiceTest {
	
	private static final String INGREDIENT_NAME = "item1";
    private static final String VENDOR_NAME = "shop1";
   
	@MockBean
	FoodService foodService;
	
	@Autowired
	FoodFinderService foodFinderService;
	
    @Test
    public void testGetVendorsByIngredient() throws Exception
    {
        Vendor [] vendors = { new Vendor(VENDOR_NAME) };
        	
        Mockito.when(foodService.getVendorsByIngredient(INGREDIENT_NAME)).thenReturn(vendors);
        
        Ingredient ingredient = new Ingredient(INGREDIENT_NAME);
		VendorInventory[] inventories = {new VendorInventory(vendors[0], ingredient)};
		
		Mockito.when(foodService.getIngredientFromVendors(vendors, INGREDIENT_NAME)).thenReturn(inventories);
        
		VendorInventory[] testInventories = foodFinderService.getIngredient(INGREDIENT_NAME);
        
        assertEquals(inventories[0], testInventories[0]);
    }

}
