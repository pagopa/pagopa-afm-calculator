package it.gov.pagopa.afm.utils.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.azure.spring.data.cosmos.repository.Query;
import it.gov.pagopa.afm.utils.entity.CDI;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface CDICollectionRepository extends CosmosRepository<CDI, String> {
  @Query("select * from Items r where r.cdiStatus = 'NEW' or r.cdiStatus = 'FAILED'")
  List<CDI> getWorkableCDIs();
}
