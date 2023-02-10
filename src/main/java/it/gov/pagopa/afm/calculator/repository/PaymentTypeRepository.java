package it.gov.pagopa.afm.calculator.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import it.gov.pagopa.afm.calculator.entity.PaymentType;
import java.util.Optional;

public interface PaymentTypeRepository extends CosmosRepository<PaymentType, String> {
  Optional<PaymentType> findByName(String name);
}
