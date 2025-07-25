package it.gov.pagopa.afm.calculator.entity;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.gov.pagopa.afm.calculator.model.BundleType;
import lombok.*;
import lombok.experimental.SuperBuilder;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Container(containerName = "bundles")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Bundle {

    @Id
    private String id;
    @PartitionKey
    private String idPsp;

    private String abi;
    private String pspBusinessName;

    private String name;
    private String description;

    private Long paymentAmount;
    private Long minPaymentAmount;
    private Long maxPaymentAmount;

    private String paymentType;

    private String touchpoint;

    @Enumerated(EnumType.STRING)
    private BundleType type;

    private List<String> transferCategoryList;

    private String idChannel;

    private String idBrokerPsp;

    @NotNull
    private Boolean digitalStamp;

    // true if bundle must be used only for digital stamp
    @NotNull
    private Boolean digitalStampRestriction;

    // useful only if paymentType = CP
    private Boolean onUs;

    // useful to see if a bundle available for carts
    private Boolean cart;
}
