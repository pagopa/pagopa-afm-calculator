package it.gov.pagopa.afm.calculator.entity;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.GeneratedValue;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Container(containerName = "paymenttypes")
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class PaymentType {

    @Id
    @GeneratedValue
    @NotBlank
    private String id;

    @NotNull
    private String name;

    @CreatedDate
    private LocalDateTime createdDate;
}
