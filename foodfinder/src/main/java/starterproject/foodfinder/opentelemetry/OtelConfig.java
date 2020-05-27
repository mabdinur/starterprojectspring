package starterproject.foodfinder.opentelemetry;

//import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
//import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import io.grpc.ManagedChannelBuilder;
//import io.opentelemetry.OpenTelemetry;
//import io.opentelemetry.sdk.OpenTelemetrySdk;
//import io.opentelemetry.sdk.trace.SpanProcessor;
//import io.opentelemetry.sdk.trace.export.SimpleSpansProcessor;
//import io.opentelemetry.trace.Tracer;
//import io.opentelemetry.exporters.jaeger.JaegerGrpcSpanExporter;
//import io.opentelemetry.exporters.logging.*;
//
//import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
//
//@Configuration
//@EnableAutoConfiguration (exclude={DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
//public class OtelConfig {
//
//    @Bean
//    public Tracer otelTracer() throws Exception{
//        final Tracer tracer = OpenTelemetry.getTracerFactory().get("com.forrest.levelone");
//        SpanProcessor jaegerProcessor =
//            SimpleSpansProcessor.newBuilder(JaegerGrpcSpanExporter.newBuilder()
//            .setServiceName("otel_spring")
//            .setChannel(ManagedChannelBuilder.forAddress(
//            "localhost", 14250).usePlaintext().build())
//            .build()).build();
//
//        SpanProcessor logProcessor = SimpleSpansProcessor.newBuilder(new LoggingExporter()).build();
//
//        OpenTelemetrySdk.getTracerFactory().addSpanProcessor(logProcessor);
//        OpenTelemetrySdk.getTracerFactory().addSpanProcessor(jaegerProcessor);
//
//        return tracer;
//    }
//}