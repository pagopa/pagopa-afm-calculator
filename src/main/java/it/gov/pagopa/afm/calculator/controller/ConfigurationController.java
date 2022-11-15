package it.gov.pagopa.afm.calculator.controller;

import io.swagger.v3.oas.annotations.Hidden;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.service.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController(value = "/configuration")
@Hidden
public class ConfigurationController {

    @Autowired
    private ConfigurationService configurationService;

    @PostMapping()
    public ResponseEntity<Void> addValidBundles(List<ValidBundle> validBundles){
        configurationService.addValidBundles(validBundles);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteValidBundles(List<ValidBundle> validBundles){
        configurationService.deleteValidBundles(validBundles);
        return ResponseEntity.ok().build();
    }
}
