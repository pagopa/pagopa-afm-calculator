package it.gov.pagopa.afm.calculator.service;

import it.gov.pagopa.afm.calculator.entity.PaymentMethod;
import it.gov.pagopa.afm.calculator.model.PaymentOptionMulti;
import it.gov.pagopa.afm.calculator.model.calculatormulti.BundleOption;
import it.gov.pagopa.afm.calculator.model.paymentmethods.FeeRange;
import it.gov.pagopa.afm.calculator.model.paymentmethods.PaymentMethodRequest;
import it.gov.pagopa.afm.calculator.model.paymentmethods.PaymentMethodsItem;
import it.gov.pagopa.afm.calculator.model.paymentmethods.PaymentMethodsResponse;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.PaymentMethodDisabledReason;
import it.gov.pagopa.afm.calculator.model.paymentmethods.enums.PaymentMethodStatus;
import it.gov.pagopa.afm.calculator.repository.PaymentMethodRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class PaymentMethodsService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final CalculatorService calculatorService;

    public PaymentMethodsResponse searchPaymentMethods(PaymentMethodRequest request) {
        List<PaymentMethodsItem> paymentMethodsItems = new ArrayList<>();

        List<PaymentMethod> candidates = paymentMethodRepository
                .findByTouchpointAndDevice(request.getUserTouchpoint().name(), request.getUserDevice().name());


        for (PaymentMethod candidate : candidates) {
            PaymentMethodDisabledReason disabledReason = null;
            PaymentMethodStatus status = candidate.getStatus();

            // Target filtering
            List<String> targetRegex = candidate.getTarget();
            if (targetRegex != null && !targetRegex.contains(request.getTargetKey())) {
                disabledReason = PaymentMethodDisabledReason.TARGET_PREVIEW;
                status = PaymentMethodStatus.DISABLED;
            }

            // amount filtering
            if (request.getTotalAmount() < candidate.getRangeAmount().getMin() || request.getTotalAmount() > candidate.getRangeAmount().getMax()) {
                disabledReason = PaymentMethodDisabledReason.AMOUNT_OUT_OF_BOUND;
                status = PaymentMethodStatus.DISABLED;
            }

            // validity date filtering
            if (candidate.getValidityDateFrom().isAfter(LocalDate.now())) {
                disabledReason = PaymentMethodDisabledReason.NOT_YET_VALID;
                status = PaymentMethodStatus.DISABLED;
            }

            // disabled filtering
            if (candidate.getStatus() == PaymentMethodStatus.DISABLED) {
                disabledReason = PaymentMethodDisabledReason.METHOD_DISABLED;
            }

            // maintenance filtering
            if (candidate.getStatus() == PaymentMethodStatus.MAINTENANCE) {
                disabledReason = PaymentMethodDisabledReason.MAINTENANCE_IN_PROGRESS;
            }


            BundleOption bundles = calculatorService.calculateMulti(PaymentOptionMulti.builder()
                            .paymentMethod(candidate.getGroup().name())
                            .touchpoint(request.getUserTouchpoint().name())
                            .idPspList(null)
                            .paymentNotice(request.getPaymentNotice())
                            .build(),
                    Integer.MAX_VALUE, request.getAllCCp(), false, "fee");

            // filter by bundles
            FeeRange feeRange = null;
            if (bundles == null || bundles.getBundleOptions() == null || bundles.getBundleOptions().isEmpty()) {
                disabledReason = PaymentMethodDisabledReason.NO_BUNDLE_AVAILABLE;
                status = PaymentMethodStatus.DISABLED;
            } else {
                int last = bundles.getBundleOptions().size() - 1;
                Long minFee = bundles.getBundleOptions().get(0).getTaxPayerFee();
                Long maxFee = bundles.getBundleOptions().get(last).getTaxPayerFee();
                feeRange = FeeRange.builder()
                        .min(minFee)
                        .max(maxFee)
                        .build();
            }
            PaymentMethodsItem item = PaymentMethodsItem.builder()
                    .paymentMethodId(candidate.getPaymentMethodId())
                    .name(candidate.getName())
                    .description(candidate.getDescription())
                    .status(status)
                    .validityDateFrom(candidate.getValidityDateFrom())
                    .group(candidate.getGroup())
                    .paymentMethodTypes(candidate.getPaymentMethodTypes())
                    .metadata(candidate.getMetadata())
                    .feeRange(feeRange)
                    .disabledReason(disabledReason)
                    .paymentMethodAsset(candidate.getPaymentMethodAsset())
                    .methodManagement(candidate.getMethodManagement())
                    .paymentMethodsBrandAssets(candidate.getPaymentMethodsBrandAssets())
                    .build();
            paymentMethodsItems.add(item);
        }
        return PaymentMethodsResponse.builder()
                .paymentMethods(paymentMethodsItems)
                .build();
    }

}
