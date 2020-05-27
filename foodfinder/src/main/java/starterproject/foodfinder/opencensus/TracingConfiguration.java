package starterproject.foodfinder.opencensus;


import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfiguration {

	@Bean
    public FilterRegistrationBean<TracingFilter> tracingFilter() {
        FilterRegistrationBean<TracingFilter> registrationBean = new FilterRegistrationBean<TracingFilter>();
        registrationBean.setFilter(new TracingFilter());
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }
}