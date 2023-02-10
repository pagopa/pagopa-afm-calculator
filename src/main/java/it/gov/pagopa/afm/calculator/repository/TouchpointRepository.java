package it.gov.pagopa.afm.calculator.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import it.gov.pagopa.afm.calculator.entity.Touchpoint;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface TouchpointRepository extends CosmosRepository<Touchpoint, String> {

  Optional<Touchpoint> findByName(String name);
}
