package starterproject.foodfinder.data;

import java.io.Serializable;

import lombok.Data;

@Data
public class Ingredient implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = -8006170266287388020L;
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

  public Ingredient(String name) {
    this.name = name;
  }
}
