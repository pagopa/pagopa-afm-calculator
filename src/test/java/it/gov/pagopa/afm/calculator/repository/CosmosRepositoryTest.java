package it.gov.pagopa.afm.calculator.repository;

import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CosmosRepositoryTest {

    @Test
    void digitalStampFilter(){
        var result = CosmosRepository.digitalStampFilter(0,0, ValidBundle.builder().build());
        assertTrue(result);
    }

}
