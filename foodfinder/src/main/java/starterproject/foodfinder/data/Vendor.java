package starterproject.foodfinder.data;

import java.io.Serializable;

import lombok.Data;

@Data
public class Vendor implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 4561900419540632184L;
  private String name;

  public Vendor() {

  }

  public Vendor(String name) {
    this.name = name;
  }
}
