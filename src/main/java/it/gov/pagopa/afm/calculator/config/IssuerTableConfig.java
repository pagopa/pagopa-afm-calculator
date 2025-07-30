package it.gov.pagopa.afm.calculator.config;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.table.CloudTable;
import it.gov.pagopa.afm.calculator.util.AzuriteStorageUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;

@Configuration
public class IssuerTableConfig {

    @Bean
    CloudTable cloudTable(
            @Value("${azure.storage.connection}") String storageConnectionString,
            @Value("${table.issuer-range}") String issuerRangeTable
    ) throws URISyntaxException, InvalidKeyException, StorageException {
        AzuriteStorageUtil azuriteStorageUtil = new AzuriteStorageUtil(storageConnectionString);
        azuriteStorageUtil.createTable(issuerRangeTable);
        return CloudStorageAccount.parse(storageConnectionString)
                .createCloudTableClient()
                .getTableReference(issuerRangeTable);
    }

}
