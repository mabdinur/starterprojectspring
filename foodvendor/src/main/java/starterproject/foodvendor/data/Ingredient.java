package starterproject.foodvendor.data;

import lombok.Data;

@Data
public class Ingredient {

  private String name;
  private float price;
  private float quantity;
  private String currency;

  public Ingredient() {

  }

  public Ingredient(String name, float price, float quantity, String currency) {
    this.name = name;
    this.price = price;
    this.quantity = quantity;
    this.currency = currency;
  }
}
