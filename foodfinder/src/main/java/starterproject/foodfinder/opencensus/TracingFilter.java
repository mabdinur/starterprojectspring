package starterproject.foodfinder.opencensus;

import io.opencensus.common.Scope;
import io.opencensus.trace.*;

import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

public class TracingFilter extends OncePerRequestFilter {

	private static final Logger LOG = Logger.getLogger(TracingFilter.class.getName());
    private static final Tracer TRACER = Tracing.getTracer();
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String spanName = "START " + request.getMethod() + " " + request.getRequestURI();
        SpanBuilder spanBuilder = SpanUtils.buildSpan(TRACER, spanName);
        Span span = spanBuilder.startSpan();
        LOG.info("FoodFinder Span created");

        try (Scope s = TRACER.withSpan(span)) {
            filterChain.doFilter(request, response);
        }
        finally {
        	span.end();
        }
    }
}
