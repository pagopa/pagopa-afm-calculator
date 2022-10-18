package it.gov.pagopa.afm.calculator.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import it.gov.pagopa.afm.calculator.entity.CiBundle;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CiBundleRepository extends CosmosRepository<CiBundle, String> {
  List<CiBundle> findByIdBundle(String idBundle);

}
