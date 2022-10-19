//package it.gov.pagopa.afm.calculator.service;
//
//import com.azure.spring.data.cosmos.core.CosmosTemplate;
//import com.azure.spring.data.cosmos.core.query.CosmosQuery;
//import it.gov.pagopa.afm.calculator.TestUtil;
//import it.gov.pagopa.afm.calculator.entity.Bundle;
//import it.gov.pagopa.afm.calculator.model.configuration.Configuration;
//import it.gov.pagopa.afm.calculator.repository.CiBundleRepository;
//import org.hibernate.engine.config.spi.ConfigurationService;
//import org.junit.jupiter.api.Test;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Captor;
//import org.mockito.InjectMocks;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//
//import java.io.IOException;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//
//@SpringBootTest
//class ConfigurationServiceTest {
//
//    @Autowired
//    @InjectMocks
//    ConfigurationService configurationService;
//
//    @MockBean
//    CosmosTemplate cosmosTemplate;
//
//    @MockBean
//    CiBundleRepository ciBundleRepository;
//
//    @Captor
//    ArgumentCaptor<List<Bundle>> bundleArgument;
//
//
//    @Test
//    void save() throws IOException {
//        var configuration = TestUtil.readObjectFromFile("requests/setConfiguration.json", Configuration.class);
//
//        configurationService.save(configuration);
//
//        verify(bundleRepository, times(1)).saveAllAndFlush(bundleArgument.capture());
//        assertEquals(1, bundleArgument.getValue().size());
//    }
//
//}
