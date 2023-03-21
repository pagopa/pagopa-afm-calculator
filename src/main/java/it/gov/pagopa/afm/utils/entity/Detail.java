package it.gov.pagopa.afm.utils.entity;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Detail {

  private String idBrokerPsp;
  private String idChannel;
  private String name;
  private String description;
  private String paymentType;
  private Boolean channelApp;
  private Boolean channelCardsCart;
  private List<ServiceAmount> serviceAmount;
}
