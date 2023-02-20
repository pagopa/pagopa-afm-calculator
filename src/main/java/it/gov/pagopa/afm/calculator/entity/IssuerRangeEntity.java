package it.gov.pagopa.afm.calculator.entity;

import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.table.EntityProperty;
import com.microsoft.azure.storage.table.TableServiceEntity;
import java.util.HashMap;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class IssuerRangeEntity extends TableServiceEntity {

  private String lowRange;
  private String highRange;
  private String circuit;
  private String productCode;
  private String productType;
  private String productCategory;
  private String issuerId;
  private String abi;

  @Override
  public void readEntity(
      final HashMap<String, EntityProperty> properties, final OperationContext opContext) {
    this.lowRange = properties.get("LOW_RANGE").getValueAsString();
    this.highRange = properties.get("HIGH_RANGE").getValueAsString();
    this.circuit = properties.get("CIRCUIT").getValueAsString();
    this.productCode = properties.get("PRODUCT_CODE").getValueAsString();
    this.productType = properties.get("PRODUCT_TYPE").getValueAsString();
    this.productCategory = properties.get("PRODUCT_CATEGORY").getValueAsString();
    this.issuerId = properties.get("ISSUER_ID").getValueAsString();
    this.abi = properties.get("ABI").getValueAsString();
  }

  public IssuerRangeEntity(String bin, String id) {
    this.partitionKey = bin;
    this.rowKey = id;
    // https://docs.microsoft.com/en-us/dotnet/api/microsoft.azure.cosmos.table.tableentity.etag?view=azure-dotnet#microsoft-azure-cosmos-table-tableentity-etag
    this.etag = "*";
  }
}
