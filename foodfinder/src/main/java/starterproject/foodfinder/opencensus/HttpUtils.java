package starterproject.foodfinder.opencensus;

import io.opencensus.common.Scope;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.propagation.TextFormat;
import java.util.logging.Logger;

import org.apache.http.Consts;
import org.apache.http.protocol.HTTP;
import org.springframework.http.HttpMethod;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    private static final Tracer tracer = Tracing.getTracer();
    private static final TextFormat textFormat = Tracing.getPropagationComponent().getB3Format();
    @SuppressWarnings("rawtypes")
	private static final TextFormat.Setter setter = new TextFormat.Setter<HttpURLConnection>() {
        public void put(HttpURLConnection carrier, String key, String value) {
            carrier.setRequestProperty(key, value);
        }
    };

	public static String callEndpoint(String url, Serializable requestBody, HttpMethod method) throws Exception {
    	LOG.info(String.format("Calling endpoint: %s, method: %s", url, method));
        
        Span span = tracer.getCurrentSpan();
        span.addAnnotation("Request sent");

        try (Scope s = tracer.withSpan(span)) {
            HttpURLConnection conn = getConnectionWithSpanContext(url, method, span);
            
            if(requestBody != null)
            	addBodyToRequest(conn, requestBody);
            
            String result = readResponse(conn);
            LOG.info(String.format("Response: %s", result));
            
            return result;
        } finally {
        	span.end();
        }
    }

	@SuppressWarnings("unchecked")
	private static HttpURLConnection getConnectionWithSpanContext(String url, HttpMethod method, Span span)
			throws IOException, MalformedURLException, ProtocolException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		textFormat.inject(span.getContext(), conn, setter);
		conn.setRequestMethod(method.name());
		return conn;
	}
    
    private static void addBodyToRequest(URLConnection httpCon, Serializable requestObj) throws IOException {
    	httpCon.setDoOutput(true);
    	httpCon.setRequestProperty(HTTP.CONTENT_TYPE, "application/json; charset=utf8");
    	
    	OutputStream os = httpCon.getOutputStream();
    	OutputStreamWriter osw = new OutputStreamWriter(os, Consts.UTF_8); 
    	
    	String body = objectToJSON(requestObj);
    	osw.write(body);
    	
    	osw.flush();
    	osw.close();
    	os.close();
    }
    
    private static String objectToJSON(Serializable obj) throws JsonProcessingException {
    	ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.writeValueAsString(obj);
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
