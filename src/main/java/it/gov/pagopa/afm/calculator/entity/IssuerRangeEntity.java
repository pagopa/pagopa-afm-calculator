package it.gov.pagopa.afm.calculator.entity;

import com.microsoft.azure.storage.table.TableServiceEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class IssuerRangeEntity extends TableServiceEntity{
	
	private String lowRange;
	private String highRange;
	private String circuit;
	private String productCode;
	private String productType;
	private String productCategory;
	private String issuerId;
	private String abi;
	
	public IssuerRangeEntity(String bin, String id) {
        this.partitionKey = bin;
        this.rowKey = id;
        // https://docs.microsoft.com/en-us/dotnet/api/microsoft.azure.cosmos.table.tableentity.etag?view=azure-dotnet#microsoft-azure-cosmos-table-tableentity-etag
        this.etag = "*";
    }
}