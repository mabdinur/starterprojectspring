package starterproject.foodfinder.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class VendorInventory implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 9114048153619759816L;
  private Vendor vendor;
  private List<Ingredient> ingredients;

  public VendorInventory() {
    this.ingredients = new ArrayList<Ingredient>();
  }

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
}
