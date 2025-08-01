package it.gov.pagopa.afm.calculator.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import it.gov.pagopa.afm.calculator.entity.Touchpoint;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static it.gov.pagopa.afm.calculator.util.Constant.TOUCHPOINT_CACHE_KEY;

@Repository
public interface TouchpointRepository extends CosmosRepository<Touchpoint, String> {

    @Cacheable(value=TOUCHPOINT_CACHE_KEY)
    Optional<Touchpoint> findByName(String name);
}
