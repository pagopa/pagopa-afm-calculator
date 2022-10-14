package it.gov.pagopa.afm.calculator.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class UpdateTouchpointTask {

    @Scheduled(fixedRate = 5000)
    public void updateTouchpoint() {
        log.info("Updating touchpoint list");
    }
}