package starterproject.foodvendor.data;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class VendorInventory {
	
	private Vendor vendor;
	private List <Ingredient> ingredients;

	public VendorInventory(Vendor vendor, Ingredient ingredient) {
		this.vendor = vendor;
		this.ingredients = new ArrayList<Ingredient>();
		this.ingredients.add(ingredient);
	}
	
	public VendorInventory(Vendor vendor, List<Ingredient> ingredients) {
		this.vendor = vendor;
		this.ingredients = ingredients;
	}
	
	public void addIngredient(Ingredient ingredient) {
		this.ingredients.add(ingredient);
	}
	
	public void removeIngredient(Ingredient ingredient) {
		this.ingredients.remove(ingredient);
	}
	
	public Ingredient getIngredient(String name) {
		return ingredients.stream().filter(ingredient -> 
			ingredient.getName() == name).findFirst().get();
	}
}
