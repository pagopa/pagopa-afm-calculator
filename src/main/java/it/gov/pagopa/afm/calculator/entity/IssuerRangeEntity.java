package it.gov.pagopa.afm.calculator.entity;

import com.microsoft.azure.storage.table.TableServiceEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class IssuerRangeEntity extends TableServiceEntity{
	private Long   id;
	private String bin;
	private String lowRange;
	private String highRange;
	private String circuit;
	private String productCode;
	private String productType;
	private String productCategory;
	private String abi;

}
