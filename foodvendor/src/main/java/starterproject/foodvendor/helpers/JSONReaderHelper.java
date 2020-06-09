package starterproject.foodvendor.helpers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

/**
 * 
 * Read JSON file
 *
 */
public class JSONReaderHelper {
  public static JSONObject getData(String resourceName) {
    JSONObject dataJson = new JSONObject();
    try {
      Resource resource = new ClassPathResource(resourceName);
      String jsonString =
          StreamUtils.copyToString(resource.getInputStream(), Charset.defaultCharset());

      JSONParser jsonParser = new JSONParser();
      dataJson = (JSONObject) jsonParser.parse(jsonString);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ParseException e) {
      e.printStackTrace();
    }

    return dataJson;
  }
}
