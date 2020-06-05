# Manual Instrumentation: Spring and SpringBoot

The goal of this project is to streamline the manual instrumentation process in OpenTelemetry for Spring and Springboot. This package will enable spring users to add tracing to requests, and database calls with minimal changes to application code. The scope of this project is not fully automated instrumentation, it is providing users with better tools to instrument their own code. 

This contribution for OpenTelemetry will follow in the footsteps of the existing Spring integrations in (Open Census)[https://github.com/census-instrumentation/opencensus-java/tree/master/contrib/spring/src/main/java/io/opencensus/contrib/spring] with upgrades to the functionality and improved user experience. 

This starter guide contains 3 tutorials. The first tutorial with will walk you through span creation and propagating requests through the Spring web client, RestTemplate. This second tutorial will show case how to implement Spring's handler and interceptor interfaces to add spans to rest controllers and propagate a span context to external services without direct edits to existing application code. The third and final tutorial will detail how to use annotations and XML configurations defined in this package to leverages techniques from the first two tutorials. This tutorial will equip new users with new tools to streamline the configuration of OpenTelemetry on Spring and SpringBoot.
