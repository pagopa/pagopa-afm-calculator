package it.gov.pagopa.afm.calculator.entity;

import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.table.EntityProperty;
import com.microsoft.azure.storage.table.TableServiceEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
      final HashMap<String, EntityProperty> properties, final OperationContext opContext)
      throws StorageException {
    super.readEntity(properties, opContext);
    this.lowRange =
        Optional.ofNullable(properties.get("LOW_RANGE"))
            .map(EntityProperty::getValueAsString)
            .orElse(lowRange);
    this.highRange =
        Optional.ofNullable(properties.get("HIGH_RANGE"))
            .map(EntityProperty::getValueAsString)
            .orElse(highRange);
    this.circuit =
        Optional.ofNullable(properties.get("CIRCUIT"))
            .map(EntityProperty::getValueAsString)
            .orElse(circuit);
    this.productCode =
        Optional.ofNullable(properties.get("PRODUCT_CODE"))
            .map(EntityProperty::getValueAsString)
            .orElse(productCode);
    this.productType =
        Optional.ofNullable(properties.get("PRODUCT_TYPE"))
            .map(EntityProperty::getValueAsString)
            .orElse(productType);
    this.productCategory =
        Optional.ofNullable(properties.get("PRODUCT_CATEGORY"))
            .map(EntityProperty::getValueAsString)
            .orElse(productCategory);
    this.issuerId =
        Optional.ofNullable(properties.get("ISSUER_ID"))
            .map(EntityProperty::getValueAsString)
            .orElse(issuerId);
    this.abi =
        Optional.ofNullable(properties.get("ABI"))
            .map(EntityProperty::getValueAsString)
            .orElse(abi);
  }

  @Override
  public HashMap<String, EntityProperty> writeEntity(OperationContext operationContext)
      throws StorageException {
    super.writeEntity(operationContext);

    Map<String, EntityProperty> properties = new HashMap<>();

    properties.put("LOW_RANGE", new EntityProperty(this.lowRange));
    properties.put("HIGH_RANGE", new EntityProperty(this.highRange));
    properties.put("CIRCUIT", new EntityProperty(this.circuit));
    properties.put("PRODUCT_CODE", new EntityProperty(this.productCode));
    properties.put("PRODUCT_TYPE", new EntityProperty(this.productType));
    properties.put("PRODUCT_CATEGORY", new EntityProperty(this.productCategory));
    properties.put("ISSUER_ID", new EntityProperty(this.issuerId));
    properties.put("ABI", new EntityProperty(this.abi));

    return (HashMap<String, EntityProperty>) properties;
  }

  public IssuerRangeEntity(String bin, String id) {
    this.partitionKey = bin;
    this.rowKey = id;
    // https://docs.microsoft.com/en-us/dotnet/api/microsoft.azure.cosmos.table.tableentity.etag?view=azure-dotnet#microsoft-azure-cosmos-table-tableentity-etag
    this.etag = "*";
  }
}
