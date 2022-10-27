package it.gov.pagopa.afm.calculator.repository;

import it.gov.pagopa.afm.calculator.entity.Touchpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TouchpointRepository  extends JpaRepository<Touchpoint, String> {
}
