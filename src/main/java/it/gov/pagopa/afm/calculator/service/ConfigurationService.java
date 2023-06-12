package it.gov.pagopa.afm.calculator.service;

import it.gov.pagopa.afm.calculator.entity.PaymentType;
import it.gov.pagopa.afm.calculator.entity.Touchpoint;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.repository.PaymentTypeRepository;
import it.gov.pagopa.afm.calculator.repository.TouchpointRepository;
import it.gov.pagopa.afm.calculator.repository.ValidBundleRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationService {

  @Autowired ValidBundleRepository validBundleRepository;

  @Autowired TouchpointRepository touchpointRepository;

  @Autowired PaymentTypeRepository paymentTypeRepository;

  public void addValidBundles(List<ValidBundle> validBundles) {
    validBundleRepository.saveAll(validBundles);
  }

  public void deleteValidBundles(List<ValidBundle> validBundles) {
    validBundleRepository.deleteAll(validBundles);
  }

  public void addTouchpoints(List<Touchpoint> touchpoints) {
    var filtered =
        touchpoints.stream()
            .filter(elem -> touchpointRepository.findByName(elem.getName()).isEmpty())
            .collect(Collectors.toList());
    touchpointRepository.saveAll(filtered);
  }

  public void deleteTouchpoints(List<Touchpoint> touchpoints) {
    var filtered =
        touchpoints.stream()
            .filter(elem -> touchpointRepository.findById(elem.getId()).isPresent())
            .collect(Collectors.toList());
    touchpointRepository.deleteAll(filtered);
  }

  public void addPaymentTypes(List<PaymentType> paymentTypes) {
    var filtered =
        paymentTypes.stream()
            .filter(elem -> paymentTypeRepository.findByName(elem.getName()).isEmpty())
            .collect(Collectors.toList());
    paymentTypeRepository.saveAll(filtered);
  }

  public void deletePaymentTypes(List<PaymentType> paymentTypes) {
    var filtered =
        paymentTypes.stream()
            .filter(elem -> paymentTypeRepository.findById(elem.getId()).isPresent())
            .collect(Collectors.toList());
    paymentTypeRepository.deleteAll(filtered);
  }
}
