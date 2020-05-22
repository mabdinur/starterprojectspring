package starterproject.foodvendor.data;

import lombok.Data;

@Data
public class Vendor {
	
	private String name;
	
	public Vendor() {
		
	}
	
	public Vendor(String name) {
		this.name = name;
	}
}
