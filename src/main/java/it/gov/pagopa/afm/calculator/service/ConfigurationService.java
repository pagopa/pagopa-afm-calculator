package it.gov.pagopa.afm.calculator.service;

import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.repository.ValidBundleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfigurationService {

    @Autowired
    ValidBundleRepository validBundleRepository;

    public void addValidBundles(List<ValidBundle> validBundles) {
        validBundleRepository.saveAll(validBundles);
    }

    public void deleteValidBundles(List<ValidBundle> validBundles) {
        validBundleRepository.deleteAll(validBundles);
    }
}
