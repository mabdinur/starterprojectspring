package starterproject.foodsupplier.data;

import lombok.Data;

@Data
public class Vendor {

  private String name;

  public Vendor(String name) {
    this.name = name;
  }
}
