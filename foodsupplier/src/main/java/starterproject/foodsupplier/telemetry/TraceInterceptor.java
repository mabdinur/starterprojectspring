package starterproject.foodsupplier.telemetry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.Tracer;

@Component
public class TraceInterceptor implements HandlerInterceptor {

  @Autowired
  Tracer tracer;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    HttpTextFormat<SpanContext> textFormat = tracer.getHttpTextFormat();
    Span span;
    try {

      SpanContext spanContext =
          textFormat.extract(request, new HttpTextFormat.Getter<HttpServletRequest>() {
            @Override
            public String get(HttpServletRequest req, String key) {
              return req.getHeader(key);
            }
          });
      span = tracer.spanBuilder(request.getRequestURI()).setParent(spanContext).startSpan();
      span.setAttribute("handler", "pre");
    } catch (Exception e) {
      span = tracer.spanBuilder(request.getRequestURI()).startSpan();
      span.setAttribute("handler", "pre");

      span.addEvent(e.toString());
      span.setAttribute("error", true);
    }
    tracer.withSpan(span);

    System.out.println("Pre Handle Called");
    return true;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
      ModelAndView modelAndView) throws Exception {

    HttpTextFormat<SpanContext> textFormat = tracer.getHttpTextFormat();
    Span currentSpan = tracer.getCurrentSpan();
    currentSpan.setAttribute("handler", "post");
    textFormat.inject(currentSpan.getContext(), response,
        new HttpTextFormat.Setter<HttpServletResponse>() {
          @Override
          public void put(HttpServletResponse response, String key, String value) {
            response.addHeader(key, value);
          }
        });
    currentSpan.end();
    System.out.println("Post Handler Called");
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception exception) throws Exception {}
}
