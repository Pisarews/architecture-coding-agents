package org.archagents.fhir.web;

import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FhirWebConfig implements WebMvcConfigurer {

    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.addFirst(new FhirJsonMessageConverter(fhirContext()));
    }
}
