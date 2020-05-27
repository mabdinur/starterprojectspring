package starterproject.foodsupplier.opencensus;

import io.opencensus.common.Scope;
import io.opencensus.trace.Span;
import io.opencensus.trace.SpanBuilder;
import io.opencensus.trace.SpanContext;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.propagation.SpanContextParseException;
import io.opencensus.trace.propagation.TextFormat;
import io.opencensus.trace.samplers.Samplers;

import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

public class TracingFilter extends OncePerRequestFilter {

	private static final Logger logger = Logger.getLogger(TracingFilter.class.getName());
    private static final Tracer tracer = Tracing.getTracer();
    private static final TextFormat textFormat = Tracing.getPropagationComponent().getB3Format();
    private static final TextFormat.Getter<HttpServletRequest> getter = new TextFormat.Getter<HttpServletRequest>() {
        @Override
        public String get(HttpServletRequest httpRequest, String s) {
            return httpRequest.getHeader(s);
        }
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        SpanContext spanContext;
        SpanBuilder spanBuilder;

        String spanName = request.getMethod() + " " + request.getRequestURI();

        try {
            spanContext = textFormat.extract(request, getter);
            spanBuilder = tracer.spanBuilderWithRemoteParent(spanName, spanContext);
        } catch (SpanContextParseException e) {
            spanBuilder = tracer.spanBuilder(spanName);
            logger.warning("Parent Span is not present");
        }

        Span span = spanBuilder.setRecordEvents(true)
                .setSampler(Samplers.alwaysSample()).startSpan();

        try (Scope s = tracer.withSpan(span)) {
            filterChain.doFilter(request, response);
        }finally {
        	span.end();
        }
    }
}
