package it.gov.pagopa.afm.calculator.config;

import com.azure.spring.data.cosmos.core.CosmosTemplate;
import it.gov.pagopa.afm.calculator.repository.PaymentTypeRepository;
import it.gov.pagopa.afm.calculator.repository.TouchpointRepository;
import it.gov.pagopa.afm.calculator.repository.ValidBundleRepository;
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

  @Bean
  @Primary
  public ValidBundleRepository validBundleRepository() {
    return Mockito.mock(ValidBundleRepository.class);
  }

  @Bean
  @Primary
  public TouchpointRepository touchpointRepository() {
    return Mockito.mock(TouchpointRepository.class);
  }

  @Bean
  @Primary
  public PaymentTypeRepository paymentTypeRepository() {
    return Mockito.mock(PaymentTypeRepository.class);
  }
}
