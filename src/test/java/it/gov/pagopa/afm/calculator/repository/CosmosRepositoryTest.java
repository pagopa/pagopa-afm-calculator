package it.gov.pagopa.afm.calculator.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.spring.data.cosmos.core.CosmosTemplate;
import com.azure.spring.data.cosmos.core.query.CosmosQuery;
import com.azure.spring.data.cosmos.core.query.Criteria;
import com.azure.spring.data.cosmos.core.query.CriteriaType;
import it.gov.pagopa.afm.calculator.entity.ValidBundle;
import it.gov.pagopa.afm.calculator.model.PaymentNoticeItem;
import it.gov.pagopa.afm.calculator.model.PaymentOption;
import it.gov.pagopa.afm.calculator.model.PaymentOptionMulti;
import it.gov.pagopa.afm.calculator.model.TransferListItem;
import it.gov.pagopa.afm.calculator.service.UtilityComponent;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CosmosRepositoryTest {

    private static final String POSTE_PSP_ID = "BPPIITRRXXX";
    private static final List<String> POSTE_CHANNEL_IDS = List.of("CHANNEL_POSTE_1", "CHANNEL_POSTE_2");
    private static final List<String> POSTE_PAY_CHANNEL_IDS = List.of("CHANNEL_POSTE_PAY_1", "CHANNEL_POSTE_PAY_2");
    private static final String ID_PSP = "idPsp";
    private static final String ID_CHANNEL = "idChannel";

    @Mock
    private CosmosTemplate cosmosTemplate;
    @Mock
    private TouchpointRepository touchpointRepository;
    @Mock
    private PaymentTypeRepository paymentTypeRepository;
    @Mock
    private UtilityComponent utilityComponent;
    @Captor
    private ArgumentCaptor<CosmosQuery> captor;


    @Test
    void digitalStampFilter_skippedWhenNoTransfersAndNoStamp() {
        var result = CosmosRepository.digitalStampFilter(0, 0, ValidBundle.builder().build());
        assertTrue(result);
    }

    @Test
    void findByPaymentOption_allCcpTrue_newFilterEnabled_addsPostepayChannelNotInFilter() {
        when(cosmosTemplate.find(any(CosmosQuery.class), eq(ValidBundle.class), eq("validbundles")))
                .thenReturn(Collections.emptyList());

        CosmosRepository repository = buildRepository(Boolean.TRUE);
        repository.findByPaymentOption(minimalPaymentOption(), true);

        verify(cosmosTemplate).find(captor.capture(), eq(ValidBundle.class), eq("validbundles"));

        Criteria root = captor.getValue().getCriteria();
        Optional<Criteria> postepayFilter = findCriteria(root, CriteriaType.NOT_IN, ID_CHANNEL);
        assertTrue(postepayFilter.isPresent(),
            "When allCcp=true a NOT_IN filter on idChannel with Postepay channels must be applied");
        assertEquals(Collections.singletonList(POSTE_PAY_CHANNEL_IDS), postepayFilter.get().getSubjectValues());
        assertTrue(findCriteria(root, CriteriaType.NOT, ID_PSP).isEmpty(),
            "The legacy psp-based Poste filter must never be applied when allCcp=true");
    }

    @Test
    void findByPaymentOption_allCcpFalse_newFilterEnabled_addsChannelNotInFilter() {
        when(cosmosTemplate.find(any(CosmosQuery.class), eq(ValidBundle.class), eq("validbundles")))
                .thenReturn(Collections.emptyList());

        CosmosRepository repository = buildRepository(Boolean.TRUE);
        repository.findByPaymentOption(minimalPaymentOption(), false);

        verify(cosmosTemplate).find(captor.capture(), eq(ValidBundle.class), eq("validbundles"));

        Criteria root = captor.getValue().getCriteria();
        Optional<Criteria> posteFilter = findCriteria(root, CriteriaType.NOT_IN, ID_CHANNEL);
        assertTrue(posteFilter.isPresent(),
                "Expected NOT_IN filter on idChannel when new filter is enabled and allCcp=false");
        assertEquals(Collections.singletonList(POSTE_CHANNEL_IDS), posteFilter.get().getSubjectValues());
        assertTrue(findCriteria(root, CriteriaType.NOT, ID_PSP).isEmpty(),
                "Legacy psp-based filter must not be applied when the new filter is enabled");
    }

    @Test
    void findByPaymentOption_allCcpFalse_newFilterDisabled_addsLegacyPspFilter() {
        when(cosmosTemplate.find(any(CosmosQuery.class), eq(ValidBundle.class), eq("validbundles")))
                .thenReturn(Collections.emptyList());

        CosmosRepository repository = buildRepository(Boolean.FALSE);
        repository.findByPaymentOption(minimalPaymentOption(), false);

        verify(cosmosTemplate).find(captor.capture(), eq(ValidBundle.class), eq("validbundles"));

        Criteria root = captor.getValue().getCriteria();
        Optional<Criteria> posteFilter = findCriteria(root, CriteriaType.NOT, ID_PSP);
        assertTrue(posteFilter.isPresent(),
                "Expected NOT filter on idPsp when new filter is disabled and allCcp=false");
        assertEquals(Collections.singletonList(POSTE_PSP_ID), posteFilter.get().getSubjectValues());
        assertTrue(findCriteria(root, CriteriaType.NOT_IN, ID_CHANNEL).isEmpty(),
                "Channel-based filter must not be applied when the new filter is disabled");
    }

    @Test
    void findByPaymentOption_allCcpFalse_newFilterNull_fallsBackToLegacyPspFilter() {
        when(cosmosTemplate.find(any(CosmosQuery.class), eq(ValidBundle.class), eq("validbundles")))
                .thenReturn(Collections.emptyList());

        CosmosRepository repository = buildRepository(null);
        repository.findByPaymentOption(minimalPaymentOption(), false);

        verify(cosmosTemplate).find(captor.capture(), eq(ValidBundle.class), eq("validbundles"));

        Criteria root = captor.getValue().getCriteria();
        assertTrue(findCriteria(root, CriteriaType.NOT, ID_PSP).isPresent(),
                "A null new-filter flag must behave like 'disabled' (legacy psp filter applied)");
        assertTrue(findCriteria(root, CriteriaType.NOT_IN, ID_CHANNEL).isEmpty());
    }


    @Test
    void findByPaymentOptionMulti_allCcpTrue_newFilterEnabled_addsPostepayChannelNotInFilter() {
        when(cosmosTemplate.find(any(CosmosQuery.class), eq(ValidBundle.class), eq("validbundles")))
                .thenReturn(Collections.emptyList());

        CosmosRepository repository = buildRepository(Boolean.TRUE);
        repository.findByPaymentOption(minimalPaymentOptionMulti(), true);

        verify(cosmosTemplate).find(captor.capture(), eq(ValidBundle.class), eq("validbundles"));

        Criteria root = captor.getValue().getCriteria();
        Optional<Criteria> postepayFilter = findCriteria(root, CriteriaType.NOT_IN, ID_CHANNEL);
        assertTrue(postepayFilter.isPresent());
        assertEquals(Collections.singletonList(POSTE_PAY_CHANNEL_IDS), postepayFilter.get().getSubjectValues());
        assertTrue(findCriteria(root, CriteriaType.NOT, ID_PSP).isEmpty());
    }

    @Test
    void findByPaymentOptionMulti_allCcpFalse_newFilterEnabled_addsChannelNotInFilter() {
        when(cosmosTemplate.find(any(CosmosQuery.class), eq(ValidBundle.class), eq("validbundles")))
                .thenReturn(Collections.emptyList());

        CosmosRepository repository = buildRepository(Boolean.TRUE);
        repository.findByPaymentOption(minimalPaymentOptionMulti(), false);

        verify(cosmosTemplate).find(captor.capture(), eq(ValidBundle.class), eq("validbundles"));

        Criteria root = captor.getValue().getCriteria();
        Optional<Criteria> posteFilter = findCriteria(root, CriteriaType.NOT_IN, ID_CHANNEL);
        assertTrue(posteFilter.isPresent());
        assertEquals(Collections.singletonList(POSTE_CHANNEL_IDS), posteFilter.get().getSubjectValues());
        assertTrue(findCriteria(root, CriteriaType.NOT, ID_PSP).isEmpty());
    }

    @Test
    void findByPaymentOptionMulti_allCcpFalse_newFilterDisabled_addsLegacyPspFilter() {
        when(cosmosTemplate.find(any(CosmosQuery.class), eq(ValidBundle.class), eq("validbundles")))
                .thenReturn(Collections.emptyList());

        CosmosRepository repository = buildRepository(Boolean.FALSE);
        repository.findByPaymentOption(minimalPaymentOptionMulti(), false);

        verify(cosmosTemplate).find(captor.capture(), eq(ValidBundle.class), eq("validbundles"));

        Criteria root = captor.getValue().getCriteria();
        Optional<Criteria> posteFilter = findCriteria(root, CriteriaType.NOT, ID_PSP);
        assertTrue(posteFilter.isPresent());
        assertEquals(Collections.singletonList(POSTE_PSP_ID), posteFilter.get().getSubjectValues());
        assertTrue(findCriteria(root, CriteriaType.NOT_IN, ID_CHANNEL).isEmpty());
    }

    @Test
    void findByPaymentOption_allCcpTrue_newFilterDisabled_doesNotAddAnyPosteFilter() {
        when(cosmosTemplate.find(any(CosmosQuery.class), eq(ValidBundle.class), eq("validbundles")))
            .thenReturn(Collections.emptyList());

        CosmosRepository repository = buildRepository(Boolean.FALSE);
        repository.findByPaymentOption(minimalPaymentOption(), true);

        verify(cosmosTemplate).find(captor.capture(), eq(ValidBundle.class), eq("validbundles"));

        Criteria root = captor.getValue().getCriteria();
        assertTrue(findCriteria(root, CriteriaType.NOT_IN, ID_CHANNEL).isEmpty(),
            "No channel-based Poste filter must be applied when the flag is disabled and allCcp=true");
        assertTrue(findCriteria(root, CriteriaType.NOT, ID_PSP).isEmpty(),
            "No legacy psp-based Poste filter must be applied when the flag is disabled and allCcp=true");
    }

    @Test
    void findByPaymentOption_allCcpTrue_newFilterNull_doesNotAddAnyPosteFilter() {
        when(cosmosTemplate.find(any(CosmosQuery.class), eq(ValidBundle.class), eq("validbundles")))
            .thenReturn(Collections.emptyList());

        CosmosRepository repository = buildRepository(null);
        repository.findByPaymentOption(minimalPaymentOption(), true);

        verify(cosmosTemplate).find(captor.capture(), eq(ValidBundle.class), eq("validbundles"));

        Criteria root = captor.getValue().getCriteria();
        assertTrue(findCriteria(root, CriteriaType.NOT_IN, ID_CHANNEL).isEmpty(),
            "A null flag must behave like 'disabled': no Poste filter when allCcp=true");
        assertTrue(findCriteria(root, CriteriaType.NOT, ID_PSP).isEmpty());
    }

    @Test
    void findByPaymentOptionMulti_allCcpTrue_newFilterDisabled_doesNotAddAnyPosteFilter() {
        when(cosmosTemplate.find(any(CosmosQuery.class), eq(ValidBundle.class), eq("validbundles")))
            .thenReturn(Collections.emptyList());

        CosmosRepository repository = buildRepository(Boolean.FALSE);
        repository.findByPaymentOption(minimalPaymentOptionMulti(), true);

        verify(cosmosTemplate).find(captor.capture(), eq(ValidBundle.class), eq("validbundles"));

        Criteria root = captor.getValue().getCriteria();
        assertTrue(findCriteria(root, CriteriaType.NOT_IN, ID_CHANNEL).isEmpty(),
            "No channel-based Poste filter must be applied when the flag is disabled and allCcp=true (multi)");
        assertTrue(findCriteria(root, CriteriaType.NOT, ID_PSP).isEmpty(),
            "No legacy psp-based Poste filter must be applied when the flag is disabled and allCcp=true (multi)");
    }
    private CosmosRepository buildRepository(Boolean allCcpNewFilterEnabled) {
        return new CosmosRepository(
                cosmosTemplate,
                touchpointRepository,
                paymentTypeRepository,
                utilityComponent,
                allCcpNewFilterEnabled,
                POSTE_PSP_ID,
                POSTE_CHANNEL_IDS,
                POSTE_PAY_CHANNEL_IDS,
                Collections.emptyList()
        );
    }

    private PaymentOption minimalPaymentOption() {
        return PaymentOption.builder()
                .paymentAmount(100L)
                .primaryCreditorInstitution("77777777777")
                .transferList(List.of(TransferListItem.builder().digitalStamp(false).build()))
                .build();
    }

    private PaymentOptionMulti minimalPaymentOptionMulti() {
        PaymentNoticeItem notice = PaymentNoticeItem.builder()
                .paymentAmount(100L)
                .primaryCreditorInstitution("77777777777")
                .transferList(List.of(TransferListItem.builder().digitalStamp(false).build()))
                .build();
        return PaymentOptionMulti.builder()
                .paymentNotice(List.of(notice))
                .build();
    }

    /**
     * Recursively look for a Criteria node in the tree matching the given type and subject.
     */
    private Optional<Criteria> findCriteria(Criteria criteria, CriteriaType type, String subject) {
        if (criteria == null) {
            return Optional.empty();
        }
        if (type.equals(criteria.getType()) && subject.equals(criteria.getSubject())) {
            return Optional.of(criteria);
        }
        for (Criteria sub : criteria.getSubCriteria()) {
            Optional<Criteria> found = findCriteria(sub, type, subject);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }
}
