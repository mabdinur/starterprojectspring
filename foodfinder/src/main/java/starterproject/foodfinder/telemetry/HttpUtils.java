package starterproject.foodfinder.telemetry;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.oxm.xstream.XStreamMarshaller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.Tracer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Adds opencensus span context to requests
 *
 */
public class HttpUtils {
    private static final Logger LOG = Logger.getLogger(HttpUtils.class.getName());
    
    @Autowired
    private static Tracer tracer;
    
    private static final HttpTextFormat<SpanContext> textFormat = tracer.getHttpTextFormat();
    private static final HttpTextFormat.Setter<HttpURLConnection> setter =
            new HttpTextFormat.Setter<HttpURLConnection>() {
      public void put(HttpURLConnection carrier, String key, String value) {
        carrier.setRequestProperty(key, value);
      }
    };

	public static String callEndpoint(String url, Serializable requestBody, HttpMethod method) throws Exception {
    	LOG.info(String.format("Calling endpoint: %s, method: %s", url, method));
    	
        Span span = tracer.getCurrentSpan();
        span.addEvent("Request sent to Microservice");
 
        HttpURLConnection conn = getConnectionWithSpanContext(url, method, span);
        
        if(requestBody != null)
        	addBodyToRequest(conn, requestBody);
        
        String result = readResponse(conn);
        LOG.info(String.format("Response: %s", result));
        
        return result;
    }

	private static HttpURLConnection getConnectionWithSpanContext(String url, HttpMethod method, Span span)
			throws IOException, MalformedURLException, ProtocolException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		textFormat.inject(span.getContext(), conn, setter);
		conn.setRequestMethod(method.name());
		return conn;
	}
    
    private static void addBodyToRequest(URLConnection httpCon, Serializable requestObj) throws IOException {
    	httpCon.setDoOutput(true);
    	httpCon.setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    	
    	OutputStream os = httpCon.getOutputStream(); 
    	OutputStreamWriter osw = new OutputStreamWriter(os, XStreamMarshaller.DEFAULT_ENCODING); 
    	
    	String body = objectToJSON(requestObj);
    	osw.write(body);
    	
    	osw.flush();
    	osw.close();
    	os.close();
    }
    
    private static String objectToJSON(Serializable obj) throws JsonProcessingException {
		return new ObjectMapper().writeValueAsString(obj);
    }
    
    private static String readResponse(HttpURLConnection conn) throws IOException {
		StringBuilder result = new StringBuilder();
		
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line;
		while ((line = rd.readLine()) != null) {
		    result.append(line);
		}
		rd.close();
		
		return result.toString();
	}
}
