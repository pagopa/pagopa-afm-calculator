package it.gov.pagopa.afm.calculator.service;

import it.gov.pagopa.afm.calculator.entity.Touchpoint;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.repository.TouchpointRepository;
import it.gov.pagopa.afm.calculator.repository.ValidBundleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConfigurationService {

    @Autowired
    ValidBundleRepository validBundleRepository;

    @Autowired
    TouchpointRepository touchpointRepository;

    public void addValidBundles(List<ValidBundle> validBundles) {
        validBundleRepository.saveAll(validBundles);
    }

    public void deleteValidBundles(List<ValidBundle> validBundles) {
        validBundleRepository.deleteAll(validBundles);
    }

    public void addTouchpoints(List<Touchpoint> touchpoints) {
        var filtered = touchpoints.stream()
                .filter(elem -> touchpointRepository.findByName(elem.getName()).isEmpty())
                .collect(Collectors.toList());
        touchpointRepository.saveAll(filtered);
    }

    public void deleteTouchpoints(List<Touchpoint> touchpoints) {
        touchpointRepository.deleteAll(touchpoints);
    }
}
