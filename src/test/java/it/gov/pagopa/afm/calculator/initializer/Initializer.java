package it.gov.pagopa.afm.calculator.initializer;


import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import org.junit.ClassRule;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.RetryNoRetry;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.table.CloudTable;
import com.microsoft.azure.storage.table.CloudTableClient;
import com.microsoft.azure.storage.table.TableRequestOptions;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext>{

	public static String issuerRangeTable = "issuerrangetable";
	
	public static CloudTable table = null;
	
	@ClassRule @Container
	public static GenericContainer<?> azurite =
	new GenericContainer<>(
			DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:latest"))
	.withExposedPorts(10001, 10002, 10000);

	public static String storageConnectionString;

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		azurite.start();
		
		storageConnectionString =
				String.format(
						"DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;TableEndpoint=http://%s:%s/devstoreaccount1;QueueEndpoint=http://%s:%s/devstoreaccount1;BlobEndpoint=http://%s:%s/devstoreaccount1",
						azurite.getHost(),
						azurite.getMappedPort(10002),
						azurite.getHost(),
						azurite.getMappedPort(10001),
						azurite.getHost(),
						azurite.getMappedPort(10000));
		
		try {
			CloudStorageAccount cloudStorageAccount = CloudStorageAccount.parse(storageConnectionString);

			CloudTableClient cloudTableClient = cloudStorageAccount.createCloudTableClient();
			TableRequestOptions tableRequestOptions = new TableRequestOptions();
			tableRequestOptions.setRetryPolicyFactory(RetryNoRetry.getInstance());
			cloudTableClient.setDefaultRequestOptions(tableRequestOptions);
			table = cloudTableClient.getTableReference(issuerRangeTable);
			table.createIfNotExists();

			TestPropertyValues.of(
					"azure.storage.connection=" + storageConnectionString,
					"table.issuer-range=" + issuerRangeTable
					).applyTo(applicationContext.getEnvironment());

		} catch (InvalidKeyException | URISyntaxException | StorageException e) {
			log.error("Error in environment initializing", e);
		}
	}
}
