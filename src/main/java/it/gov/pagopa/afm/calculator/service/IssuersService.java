package it.gov.pagopa.afm.calculator.service;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.table.CloudTable;
import com.microsoft.azure.storage.table.TableQuery;
import com.microsoft.azure.storage.table.TableQuery.QueryComparisons;
import it.gov.pagopa.afm.calculator.entity.IssuerRangeEntity;
import it.gov.pagopa.afm.calculator.exception.AppError;
import it.gov.pagopa.afm.calculator.exception.AppException;
import it.gov.pagopa.afm.calculator.util.AzuriteStorageUtil;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

  public List<IssuerRangeEntity> getIssuersByBIN(String bin) {
    Spliterator<IssuerRangeEntity> resultIssuerRangeEntityList = null;
    try {
      CloudTable table =
          CloudStorageAccount.parse(storageConnectionString)
              .createCloudTableClient()
              .getTableReference(this.issuerRangeTable);

      String paddedBin = StringUtils.rightPad(bin, 19, '0');

      String filters =
          TableQuery.combineFilters(
              TableQuery.generateFilterCondition(
                  "LOW_RANGE", QueryComparisons.LESS_THAN_OR_EQUAL, paddedBin),
              TableQuery.Operators.AND,
              TableQuery.generateFilterCondition(
                  "HIGH_RANGE", QueryComparisons.GREATER_THAN_OR_EQUAL, paddedBin));

      resultIssuerRangeEntityList =
          table.execute(TableQuery.from(IssuerRangeEntity.class).where(filters)).spliterator();

    } catch (InvalidKeyException | URISyntaxException | StorageException e) {
      // unexpected error
      log.error("Error in processing get issuers by BIN [bin = " + bin + "]", e);
      throw new AppException(AppError.INTERNAL_SERVER_ERROR);
    }
    return StreamSupport.stream(resultIssuerRangeEntityList, false).collect(Collectors.toList());
  }
}
