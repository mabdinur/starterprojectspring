package starterproject.foodfinder.opentelemetry;

//import java.util.concurrent.atomic.AtomicLong;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import io.opentelemetry.context.Scope;
//import io.opentelemetry.trace.Span;
//import io.opentelemetry.trace.Tracer;
//
//@RestController
//public class SomeController {
//    private final AtomicLong counter = new AtomicLong();
//
//    @Autowired
//    Tracer tracer;
//
//    @RequestMapping("/api/1")
//    public Greeting greeting(@RequestParam(value="name", defaultValue="level1Default") String name) {
//            Span span = tracer.spanBuilder("api/1").startSpan();
//            span.addEvent("Controller Entered");
//            span.setAttribute("counter.id", counter.get());
//
//            try(Scope scope = tracer.withSpan(span)){
//                return new Greeting(counter.incrementAndGet(), getContent());
//            }
//            catch(Exception e){
//                span.addEvent("error");
//                span.setAttribute("error", true);
//                return new Greeting(0l, "critical failure");
//            }
//            finally{
//                span.end();
//            }
//
//    }
//
//    public String getContent(){
//        Span span = tracer.spanBuilder("getContent").startSpan();
//        span.setAttribute("content.type", "text");
//        span.end();
//        return "Some content";
//    }
//
//}