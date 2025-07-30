package it.gov.pagopa.afm.calculator.service;

import it.gov.pagopa.afm.calculator.entity.PaymentType;
import it.gov.pagopa.afm.calculator.entity.Touchpoint;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.repository.PaymentTypeRepository;
import it.gov.pagopa.afm.calculator.repository.TouchpointRepository;
import it.gov.pagopa.afm.calculator.repository.ValidBundleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfigurationService {
    private final ValidBundleRepository validBundleRepository;
    private final TouchpointRepository touchpointRepository;
    private final PaymentTypeRepository paymentTypeRepository;
    private final IssuersService issuersService;

    @Autowired
    public ConfigurationService(
            ValidBundleRepository validBundleRepository,
            TouchpointRepository touchpointRepository,
            PaymentTypeRepository paymentTypeRepository,
            IssuersService issuersService
    ) {
        this.validBundleRepository = validBundleRepository;
        this.touchpointRepository = touchpointRepository;
        this.paymentTypeRepository = paymentTypeRepository;
        this.issuersService = issuersService;
    }

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
                        .toList();
        touchpointRepository.saveAll(filtered);
    }

    public void deleteTouchpoints(List<Touchpoint> touchpoints) {
        var filtered =
                touchpoints.stream()
                        .filter(elem -> touchpointRepository.findById(elem.getId()).isPresent())
                        .toList();
        touchpointRepository.deleteAll(filtered);
    }

    public void addPaymentTypes(List<PaymentType> paymentTypes) {
        var filtered =
                paymentTypes.stream()
                        .filter(elem -> paymentTypeRepository.findByName(elem.getName()).isEmpty())
                        .toList();
        paymentTypeRepository.saveAll(filtered);
    }

    public void deletePaymentTypes(List<PaymentType> paymentTypes) {
        var filtered =
                paymentTypes.stream()
                        .filter(elem -> paymentTypeRepository.findById(elem.getId()).isPresent())
                        .toList();
        paymentTypeRepository.deleteAll(filtered);
    }

    public void refreshIssuerRangeTableCache() {
        issuersService.evictIssuerRangeTableCache();
        issuersService.getIssuerRangeTableCached();
    }
}
