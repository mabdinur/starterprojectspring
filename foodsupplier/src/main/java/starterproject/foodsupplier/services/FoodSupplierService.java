package starterproject.foodsupplier.services;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import starterproject.foodsupplier.data.Vendor;
import starterproject.foodsupplier.helpers.JSONReaderHelper;

/**
 * Maps vendors to available ingredients ingredients
 */ 
@Service
public class FoodSupplierService implements FoodSupplier
{
    private static final String SUPPLIER_FILE = "suppliers.json";
    
    private JSONObject vendorToItemsJson;

    public FoodSupplierService()
    {
        vendorToItemsJson = JSONReaderHelper.getData(SUPPLIER_FILE);
    }
    
    public List<Vendor> getVendorsByIngredient(String ingredientName)
    {
        List<Vendor> vendors = new ArrayList<Vendor>();

        for (Iterator<String> iterator = vendorToItemsJson.keySet().iterator(); iterator.hasNext();) {
            String vendorName = iterator.next();
            
            if (vendorHasItem(vendorName, ingredientName)) {
            	Vendor vendor = new Vendor(vendorName);
                vendors.add(vendor);
            }
        }

        return vendors;
    }

    private boolean vendorHasItem(String vendorName, String itemName)
    {
        JSONArray vendorItemsArray = (JSONArray) vendorToItemsJson.get(vendorName);
        return vendorItemsArray.contains(itemName);
    }
}
