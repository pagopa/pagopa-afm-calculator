package it.gov.pagopa.afm.calculator.config;

import com.azure.spring.data.cosmos.core.CosmosTemplate;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MockitoConfig {

    @Bean
    @Primary
    public CosmosTemplate cosmosTemplate() {
        return Mockito.mock(CosmosTemplate.class);
    }

}
