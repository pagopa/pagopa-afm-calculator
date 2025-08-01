package it.gov.pagopa.afm.calculator.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import it.gov.pagopa.afm.calculator.entity.PaymentType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static it.gov.pagopa.afm.calculator.util.Constant.DEFAULT_CACHE_KEY;

@Repository
public interface PaymentTypeRepository extends CosmosRepository<PaymentType, String> {
    @Cacheable(value = DEFAULT_CACHE_KEY)
    Optional<PaymentType> findByName(String name);
}
