package it.gov.pagopa.afm.calculator.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.afm.calculator.entity.PaymentType;
import it.gov.pagopa.afm.calculator.entity.Touchpoint;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.service.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@Tag(name = "Configuration", description = "Utility Services")
@RequestMapping(path = "/configuration")
public class ConfigurationController {

    private final ConfigurationService configurationService;

    @Autowired
    public ConfigurationController(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @PostMapping("/bundles/add")
    public ResponseEntity<Void> addValidBundles(@RequestBody List<ValidBundle> validBundles) {
        configurationService.addValidBundles(validBundles);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/bundles/delete")
    public ResponseEntity<Void> deleteValidBundles(@RequestBody List<ValidBundle> validBundles) {
        configurationService.deleteValidBundles(validBundles);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/touchpoint/add")
    public ResponseEntity<Void> addTouchpoints(@RequestBody List<Touchpoint> touchpoints) {
        configurationService.addTouchpoints(touchpoints);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/touchpoint/delete")
    public ResponseEntity<Void> deleteTouchpoints(@RequestBody List<Touchpoint> touchpoints) {
        configurationService.deleteTouchpoints(touchpoints);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/paymenttypes/add")
    public ResponseEntity<Void> addPaymentTypes(@RequestBody List<PaymentType> paymentTypes) {
        configurationService.addPaymentTypes(paymentTypes);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/paymenttypes/delete")
    public ResponseEntity<Void> deletePaymentTypes(@RequestBody List<PaymentType> paymentTypes) {
        configurationService.deletePaymentTypes(paymentTypes);
        return ResponseEntity.ok().build();
    }
}
