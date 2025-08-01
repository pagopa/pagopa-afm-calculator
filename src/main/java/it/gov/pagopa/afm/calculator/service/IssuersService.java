package it.gov.pagopa.afm.calculator.service;

import com.azure.data.tables.TableClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.afm.calculator.entity.IssuerRangeEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

import static it.gov.pagopa.afm.calculator.util.Constant.LONG_CACHE_KEY;

@Service
@Slf4j
public class IssuersService {

    private final TableClient tableClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public IssuersService(TableClient tableClient, ObjectMapper objectMapper) {
        this.tableClient = tableClient;
        this.objectMapper = objectMapper;
    }

    @Cacheable(value = LONG_CACHE_KEY, unless = "#result == null")
    public List<IssuerRangeEntity> getIssuerRangeTableCached() {
        return this.tableClient.listEntities().stream().parallel().map(el ->
                objectMapper.convertValue(el.getProperties(), IssuerRangeEntity.class)).toList();
    }

    @CacheEvict(value = LONG_CACHE_KEY)
    public void evictIssuerRangeTableCache() {
        // Issuer range table scheduled cache evict
    }
}
