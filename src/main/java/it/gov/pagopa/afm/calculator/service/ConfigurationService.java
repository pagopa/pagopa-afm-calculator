package it.gov.pagopa.afm.calculator.service;

import it.gov.pagopa.afm.calculator.entity.Bundle;
import it.gov.pagopa.afm.calculator.entity.CiBundle;
import it.gov.pagopa.afm.calculator.entity.Touchpoint;
import it.gov.pagopa.afm.calculator.model.PaymentMethod;
import it.gov.pagopa.afm.calculator.model.configuration.Configuration;
import it.gov.pagopa.afm.calculator.repository.BundleRepository;
import it.gov.pagopa.afm.calculator.repository.CiBundleRepository;
import it.gov.pagopa.afm.calculator.repository.TouchpointRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ConfigurationService {

    @Autowired
    BundleRepository bundleRepository;

    @Autowired
    CiBundleRepository ciBundleRepository;

    @Autowired
    TouchpointRepository touchpointRepository;

    @Autowired
    ModelMapper modelMapper;

    @Transactional
    public void save(Configuration configuration) {
        // erase tables
        bundleRepository.deleteAll();
        ciBundleRepository.deleteAll();
        touchpointRepository.deleteAll();

        List<Touchpoint> touchpoints = configuration.getTouchpoints().parallelStream().map(
                touchpoint -> modelMapper.map(touchpoint, it.gov.pagopa.afm.calculator.entity.Touchpoint.class)
        ).collect(Collectors.toList());

        List<Bundle> bundles = configuration.getBundles();
        // set any to null to simplify query during calculation
        bundles.parallelStream().forEach(bundle -> {
            if (bundle.getTouchpoint().getName().equals("ANY")) {
                bundle.setTouchpoint(null);
            }

            if (bundle.getPaymentMethod().equals(PaymentMethod.ANY)) {
                bundle.setPaymentMethod(null);
            }
        });

        // save
        touchpointRepository.saveAllAndFlush(touchpoints);
        List<Bundle> bundleList = bundleRepository.saveAllAndFlush(bundles);
        List<Bundle> bundleListToSave = new ArrayList<>();
        List<CiBundle> ciBundleList = configuration.getCiBundles().parallelStream().map(ciBundleM -> {
            CiBundle ciBundleE = modelMapper.map(ciBundleM, CiBundle.class);

            Optional<Bundle> optBundle = bundleList.parallelStream().filter(bundle ->
                    bundle.getId().equals(ciBundleE.getBundle().getId())
            ).findFirst();
            if (optBundle.isPresent()) {
                Bundle bundle = optBundle.get();
                bundle.getCiBundles().add(ciBundleE);
                bundleListToSave.add(bundle);
            }
            return ciBundleE;
        }).collect(Collectors.toList());
        ciBundleRepository.saveAllAndFlush(ciBundleList);
        bundleRepository.saveAll(bundleListToSave);
    }

    public Configuration get() {
        return Configuration.builder()
                .bundles(bundleRepository.findAll())
                .ciBundles(ciBundleRepository.findAll().parallelStream().map(ciBundle -> modelMapper.map(ciBundle, it.gov.pagopa.afm.calculator.model.configuration.CiBundle.class)).collect(Collectors.toList()))
                .touchpoints(touchpointRepository.findAll().parallelStream().map(touchpoint -> modelMapper.map(touchpoint, it.gov.pagopa.afm.calculator.model.Touchpoint.class)).collect(Collectors.toList()))
                .build();
    }
}
