package it.gov.pagopa.afm.calculator.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.afm.calculator.model.paymentmethods.PaymentMethodRequest;
import it.gov.pagopa.afm.calculator.model.paymentmethods.PaymentMethodsResponse;
import it.gov.pagopa.afm.calculator.service.PaymentMethodsService;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController()
@Tag(name = "Payment Methods", description = "Everything about the payment methods")
@RequestMapping(path = "/payment-methods")
@AllArgsConstructor
public class PaymentMethodsController {

    private final PaymentMethodsService paymentMethodsService;


    @PostMapping(
            value = "/search",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<PaymentMethodsResponse> searchPaymentMethods(@RequestBody @Valid PaymentMethodRequest paymentMethodRequest) {
        return paymentMethodsService.searchPaymentMethods(paymentMethodRequest);
    }


}
