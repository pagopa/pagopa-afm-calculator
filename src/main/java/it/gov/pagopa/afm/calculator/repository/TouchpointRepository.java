package it.gov.pagopa.afm.calculator.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import it.gov.pagopa.afm.calculator.entity.Touchpoint;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TouchpointRepository extends CosmosRepository<Touchpoint, String> {

    Optional<Touchpoint> findByName(String name);
}
