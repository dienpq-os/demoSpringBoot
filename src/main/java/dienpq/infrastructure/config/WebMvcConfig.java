package dienpq.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
                // Static resources
                registry.addResourceHandler("/static/**")
                                .addResourceLocations("classpath:/static/");

                registry.addResourceHandler("/images/**")
                                .addResourceLocations("file:uploads/images/");

                registry.addResourceHandler("/css/**")
                                .addResourceLocations("classpath:/static/css/");

                registry.addResourceHandler("/js/**")
                                .addResourceLocations("classpath:/static/js/");
        }
}