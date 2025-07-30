package it.gov.pagopa.afm.calculator.service;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.table.CloudTable;
import com.microsoft.azure.storage.table.TableQuery;
import it.gov.pagopa.afm.calculator.entity.IssuerRangeEntity;
import it.gov.pagopa.afm.calculator.exception.AppError;
import it.gov.pagopa.afm.calculator.exception.AppException;
import it.gov.pagopa.afm.calculator.util.AzuriteStorageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static it.gov.pagopa.afm.calculator.util.Constant.ISSUER_RANGE_TABLE_CACHE_KEY;

@Service
@Slf4j
public class IssuersService {

    private String storageConnectionString;
    private String issuerRangeTable;

    public IssuersService(
            @Value("${azure.storage.connection}") String storageConnectionString,
            @Value("${table.issuer-range}") String issuerRangeTable) {
        super();
        this.storageConnectionString = storageConnectionString;
        this.issuerRangeTable = issuerRangeTable;
        try {
            AzuriteStorageUtil azuriteStorageUtil = new AzuriteStorageUtil(storageConnectionString);
            azuriteStorageUtil.createTable(issuerRangeTable);
        } catch (InvalidKeyException | URISyntaxException | StorageException e) {
            log.error("Error in environment initializing", e);
        }
    }

    @Cacheable(value = ISSUER_RANGE_TABLE_CACHE_KEY, unless = "#result == null")
    public List<IssuerRangeEntity> getIssuerRangeTableCached() {
        try {
            CloudTable table =
                    CloudStorageAccount.parse(storageConnectionString)
                            .createCloudTableClient()
                            .getTableReference(this.issuerRangeTable);

            Spliterator<IssuerRangeEntity> resultIssuerRangeEntityList = table.execute(TableQuery.from(IssuerRangeEntity.class)).spliterator();

            return StreamSupport.stream(resultIssuerRangeEntityList, false).collect(Collectors.toList());
        } catch (InvalidKeyException | URISyntaxException | StorageException e) {
            // unexpected error
            log.error("Error retrieving the issuer range table", e);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        }
    }

    public List<IssuerRangeEntity> getIssuersByBIN(String bin) {
        try {
            long paddedBin = Long.parseLong(StringUtils.rightPad(bin, 19, '0'));

            List<IssuerRangeEntity> resultIssuerRangeEntityList = getIssuerRangeTableCached();

            return resultIssuerRangeEntityList.parallelStream()
                    .filter(el -> Long.parseLong(el.getLowRange()) <= paddedBin && Long.parseLong(el.getHighRange()) >= paddedBin)
                    .collect(Collectors.toList());
        } catch (AppException e) {
            // unexpected error
            log.error("Error in processing get issuers by BIN [bin = {}]", bin, e);
            throw e;
        }
    }
}
