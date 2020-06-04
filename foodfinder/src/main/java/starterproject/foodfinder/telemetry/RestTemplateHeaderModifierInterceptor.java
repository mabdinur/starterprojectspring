package starterproject.foodfinder.telemetry;

import java.io.IOException;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;

import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.Tracer;

@Component
public class RestTemplateHeaderModifierInterceptor implements ClientHttpRequestInterceptor {
	
	private static final Logger LOG = Logger.getLogger(TraceInterceptor.class.getName()); 
	
	@Autowired
    private Tracer tracer;
	
	
	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body,
			ClientHttpRequestExecution execution) throws IOException {
		
		Span currentSpan = tracer.getCurrentSpan();
        currentSpan.setAttribute("client_http", "inject");
        currentSpan.addEvent("Internal request sent to food service");
        
        HttpTextFormat<SpanContext> textFormat = tracer.getHttpTextFormat();
        
        textFormat.inject(currentSpan.getContext(), request, new
        HttpTextFormat.Setter<HttpRequest>() {
            @Override
            public void put(HttpRequest request, String key, String value)
            {
            	request.getHeaders().set(key, value);
            }
        });
        
		ClientHttpResponse response = execution.execute(request, body);
		
		
		LOG.info(String.format("Response: %s", response.toString()));
		
		return response;
	}
}