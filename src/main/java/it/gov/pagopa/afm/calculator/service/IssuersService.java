package it.gov.pagopa.afm.calculator.service;

import com.microsoft.azure.storage.table.CloudTable;
import com.microsoft.azure.storage.table.TableQuery;
import it.gov.pagopa.afm.calculator.entity.IssuerRangeEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Spliterator;
import java.util.stream.StreamSupport;

import static it.gov.pagopa.afm.calculator.util.Constant.ISSUER_RANGE_TABLE_CACHE_KEY;

@Service
@Slf4j
public class IssuersService {

    private final CloudTable cloudTable;

    @Autowired
    public IssuersService(CloudTable cloudTable) {
        this.cloudTable = cloudTable;
    }

    @Cacheable(value = ISSUER_RANGE_TABLE_CACHE_KEY, unless = "#result == null")
    public List<IssuerRangeEntity> getIssuerRangeTableCached() {
        Spliterator<IssuerRangeEntity> resultIssuerRangeEntityList = this.cloudTable.execute(TableQuery.from(IssuerRangeEntity.class)).spliterator();

        return StreamSupport.stream(resultIssuerRangeEntityList, false).toList();
    }

    @CacheEvict(value = ISSUER_RANGE_TABLE_CACHE_KEY)
    public void evictIssuerRangeTableCache() {
        // Issuer range table scheduled cache evict
    }
}
