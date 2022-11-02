package it.gov.pagopa.afm.calculator;

import it.gov.pagopa.afm.calculator.initializer.Initializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ContextConfiguration(initializers = {Initializer.class})
class ApplicationTest {

    @Test
    void contextLoads() {
        // check only if the context is loaded
        assertTrue(true);
    }
}
