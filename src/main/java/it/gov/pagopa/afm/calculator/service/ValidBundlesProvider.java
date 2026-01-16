package it.gov.pagopa.afm.calculator.service;

import com.azure.spring.data.cosmos.core.CosmosTemplate;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.stream.StreamSupport;

@Service
public class ValidBundlesProvider {
    private final CosmosTemplate cosmosTemplate;

    public ValidBundlesProvider(CosmosTemplate cosmosTemplate) {
        this.cosmosTemplate = cosmosTemplate;
    }

    @Cacheable(value = "validBundles")
    public List<ValidBundle> getAllValidBundles() {
        return StreamSupport.stream(
                        cosmosTemplate.findAll(ValidBundle.class).spliterator(), true)
                .toList();
    }
}
