package it.gov.pagopa.afm.calculator.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@NoArgsConstructor
@Table(name = "TRANSFERCATEGORY", schema = "AFM_CALCULATOR")
@Slf4j
public class TransferCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

   private String name;

    public TransferCategory(String name) {
        super();
        this.name = name;
    }

}
