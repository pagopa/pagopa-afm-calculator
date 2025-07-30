package it.gov.pagopa.afm.calculator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.afm.calculator.entity.PaymentType;
import it.gov.pagopa.afm.calculator.entity.Touchpoint;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.model.ProblemJson;
import it.gov.pagopa.afm.calculator.service.ConfigurationService;
import it.gov.pagopa.afm.calculator.service.IssuersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController()
@Tag(name = "Configuration", description = "Utility Services")
@RequestMapping(path = "/configuration")
public class ConfigurationController {

    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private IssuersService issuersService;

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

    @Operation(summary = "Refresh issuer range table cache")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Ok",
                            content = @Content(schema = @Schema())),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema())),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Too many requests",
                            content = @Content(schema = @Schema())),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Service unavailable",
                            content =
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ProblemJson.class)))
            })
    @PostMapping("/refresh/issuers")
    public ResponseEntity<Void> refreshIssuerRangeTableCache() {
        issuersService.getIssuerRangeTableCached();
        return ResponseEntity.ok().build();
    }
}
