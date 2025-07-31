package it.gov.pagopa.afm.calculator.config;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IssuerTableConfig {

    @Bean
    TableClient tableClient(
            @Value("${azure.storage.connection}") String storageConnectionString,
            @Value("${table.issuer-range}") String issuerRangeTable
    ) {
        return new TableClientBuilder()
                .connectionString(storageConnectionString)
                .tableName(issuerRangeTable)
                .buildClient();
    }

}
