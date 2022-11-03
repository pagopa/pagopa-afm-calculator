package it.gov.pagopa.afm.calculator.repository;

import it.gov.pagopa.afm.calculator.entity.Touchpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TouchpointRepository  extends JpaRepository<Touchpoint, String> {
    Optional<Touchpoint> findByName(String name);
}
