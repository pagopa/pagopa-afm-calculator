package it.gov.pagopa.afm.calculator.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import org.springframework.stereotype.Repository;

@Repository
public interface ValidBundleRepository extends CosmosRepository<ValidBundle, String> {
}
