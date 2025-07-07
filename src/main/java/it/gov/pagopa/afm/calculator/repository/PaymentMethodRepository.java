package it.gov.pagopa.afm.calculator.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.azure.spring.data.cosmos.repository.Query;
import it.gov.pagopa.afm.calculator.entity.PaymentMethod;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends CosmosRepository<PaymentMethod, String> {


    @Query("SELECT * FROM c WHERE ARRAY_CONTAINS(c.user_touchpoint, @touchpoint) AND ARRAY_CONTAINS(c.user_device, @device)")
    List<PaymentMethod> findByTouchpointAndDevice(@Param("touchpoint") String touchpoint, @Param("device") String device);

    List<PaymentMethod> findAll();

    @Query("SELECT * FROM c WHERE c.payment_method_id = @paymentMethodId")
    List<PaymentMethod> findByPaymentMethodId(@Param("paymentMethodId")  String paymentMethodId);

}
