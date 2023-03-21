package it.gov.pagopa.afm.utils.service;

import com.azure.cosmos.implementation.apachecommons.lang.StringUtils;
import feign.FeignException;
import it.gov.pagopa.afm.utils.entity.CDI;
import it.gov.pagopa.afm.utils.entity.Detail;
import it.gov.pagopa.afm.utils.entity.ServiceAmount;
import it.gov.pagopa.afm.utils.entity.StatusType;
import it.gov.pagopa.afm.utils.exception.AppError;
import it.gov.pagopa.afm.utils.exception.AppException;
import it.gov.pagopa.afm.utils.model.bundle.BundleRequest;
import it.gov.pagopa.afm.utils.model.bundle.BundleResponse;
import it.gov.pagopa.afm.utils.model.bundle.BundleType;
import it.gov.pagopa.afm.utils.model.bundle.PaymentMethodType;
import it.gov.pagopa.afm.utils.model.bundle.TouchpointType;
import it.gov.pagopa.afm.utils.repository.CDICollectionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Slf4j
public class CDIService {
  @Autowired private CDICollectionRepository cdisRepository;

  @Autowired private MarketPlaceClient marketPlaceClient;

  public CDI updateCDI(CDI cdiEntity) {
    return cdisRepository.save(cdiEntity);
  }

  public void deleteCDI(CDI cdiEntity) {
    cdisRepository.delete(cdiEntity);
  }

  public void saveCDIs(List<CDI> cdis) {
    cdisRepository.saveAll(cdis);
    CompletableFuture.runAsync(() -> {
        turnCDIToBundles(cdis);
    });
  }

  public List<BundleResponse> syncCDI() {
    return this.turnCDIToBundles(cdisRepository.getWorkableCDIs());
  }

  private List<BundleResponse> turnCDIToBundles(List<CDI> cdis) {
    log.info(
        "CDIService - turnCDIToBundles executed at: "
            + LocalDateTime.now()
            + " for CDI list with size: "
            + cdis.size());
    List<BundleResponse> bundleResponses = new ArrayList<>();
    // the status of the items is put on PROCESSING to identify that these records are not in a
    // final state
    this.setCDIToProcessingStatus(cdis);
    for (CDI cdi : cdis) {
      // processed only cdis not in FAILED status
      if (null != cdi.getCdiStatus() && !cdi.getCdiStatus().equals(StatusType.FAILED)) {
        String idPsp = cdi.getIdPsp();
        List<BundleRequest> bundleRequestList = this.createBundlesByCDI(cdi);
        try {
          // create marketplace bundle
          Optional.ofNullable(this.createBundleByList(idPsp, bundleRequestList))
              .ifPresent(bundleResponses::addAll);
          // success -> delete the CDI
          this.deleteCDI(cdi);
          log.info(
              String.format("SUCCESS Bundles creation -> CDI deleted [idCdi=%s]", cdi.getIdCdi()));

        } catch (AppException e) {
          log.error(
              "Error during the creation of the MarketPlace Bundles [idPsp= "
                  + idPsp
                  + ", idCdi="
                  + cdi.getIdCdi()
                  + "]",
              e);
          // error -> set CDI status to failed
          cdi.setCdiStatus(StatusType.FAILED);
          cdi.setCdiErrorDesc(e.getMessage());
          CDI updated = this.updateCDI(cdi);
          log.info(
              String.format(
                  "CDI status updated [idCdi=%s, status=%s, errorDesc=%s]",
                  Optional.ofNullable(updated).map(result -> updated.getIdCdi()),
                  Optional.ofNullable(updated).map(result -> updated.getCdiStatus()),
                  Optional.ofNullable(updated).map(result -> updated.getCdiErrorDesc())));
        }
      }
    }
    return bundleResponses;
  }

  public List<BundleRequest> createBundlesByCDI(CDI cdi) {
    List<BundleRequest> bundleRequestList = new ArrayList<>();
    if (!CollectionUtils.isEmpty(cdi.getDetails())) {
      DateTimeFormatter dfDate = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
      BundleRequest bundleRequest = new BundleRequest();
      bundleRequest.setIdCdi(cdi.getIdCdi());
      bundleRequest.setAbi(cdi.getAbi());
      bundleRequest.setDigitalStamp(cdi.getDigitalStamp());
      bundleRequest.setDigitalStampRestriction(Boolean.FALSE);
      bundleRequest.setType(BundleType.GLOBAL);
      bundleRequest.setTransferCategoryList(null);
      bundleRequest.setValidityDateFrom(
          !StringUtils.isEmpty(cdi.getValidityDateFrom())
              ? LocalDate.parse(cdi.getValidityDateFrom(), dfDate)
              : null);
      bundleRequest.setValidityDateTo(null);
      for (Detail d : cdi.getDetails()) {
        bundleRequest.setIdChannel(d.getIdChannel());
        bundleRequest.setIdBrokerPsp(d.getIdBrokerPsp());
        bundleRequest.setName(d.getName());
        bundleRequest.setDescription(d.getDescription());
        bundleRequest.setPaymentType(d.getPaymentType());
        for (ServiceAmount sa : d.getServiceAmount()) {
          bundleRequest.setPaymentAmount(sa.getPaymentAmount());
          bundleRequest.setMinPaymentAmount(sa.getMinPaymentAmount());
          bundleRequest.setMaxPaymentAmount(sa.getMaxPaymentAmount());
          this.addBundleByTouchpoint(d, bundleRequestList, bundleRequest);
        }
      }
    }
    return bundleRequestList;
  }

  private void addBundleByTouchpoint(
      Detail d, List<BundleRequest> bundleRequestList, BundleRequest bundleRequest) {

    boolean isNullTouchPoint = true;

    if (d.getPaymentType().equalsIgnoreCase(PaymentMethodType.PO.name())) {
      isNullTouchPoint = false;
      BundleRequest bundleRequestClone = SerializationUtils.clone(bundleRequest);
      bundleRequestClone.setTouchpoint(TouchpointType.PSP.name());
      bundleRequestList.add(bundleRequestClone);
    }
    if ((d.getPaymentType().equalsIgnoreCase(PaymentMethodType.CP.name())
            && d.getChannelCardsCart()
            && d.getChannelApp().equals(Boolean.FALSE))
        || (d.getPaymentType()
                .matches(
                    "(?i)"
                        + PaymentMethodType.BBT
                        + "|"
                        + PaymentMethodType.BP
                        + "|"
                        + PaymentMethodType.MYBK
                        + "|"
                        + PaymentMethodType.AD)
            && d.getChannelApp().equals(Boolean.FALSE))
        || (!d.getPaymentType().equalsIgnoreCase(PaymentMethodType.PPAL.name())
            && d.getChannelApp())) {
      isNullTouchPoint = false;
      BundleRequest bundleRequestClone = SerializationUtils.clone(bundleRequest);
      bundleRequestClone.setTouchpoint(TouchpointType.WISP.name());
      bundleRequestList.add(bundleRequestClone);
    }
    if ((d.getPaymentType().equalsIgnoreCase(PaymentMethodType.CP.name())
            && d.getChannelCardsCart()
            && d.getChannelApp().equals(Boolean.FALSE))
        || (d.getPaymentType().equalsIgnoreCase(PaymentMethodType.PPAL.name())
            && d.getChannelApp())) {
      isNullTouchPoint = false;
      BundleRequest bundleRequestClone = SerializationUtils.clone(bundleRequest);
      bundleRequestClone.setTouchpoint(TouchpointType.IO.name());
      bundleRequestList.add(bundleRequestClone);
    }
    if ((d.getPaymentType().equalsIgnoreCase(PaymentMethodType.CP.name())
        && d.getChannelCardsCart()
        && d.getChannelApp().equals(Boolean.FALSE))) {
      isNullTouchPoint = false;
      BundleRequest bundleRequestClone = SerializationUtils.clone(bundleRequest);
      bundleRequestClone.setTouchpoint(TouchpointType.CHECKOUT.name());
      bundleRequestList.add(bundleRequestClone);
    }
    if (isNullTouchPoint) {
      // default bundle with null touchpoint value
      bundleRequestList.add(bundleRequest);
    }
  }

  private List<BundleResponse> createBundleByList(
      String idPsp, List<BundleRequest> bundleRequestList) throws AppException {
    List<BundleResponse> response = null;
    try {
      response =
          Optional.ofNullable(marketPlaceClient)
              .map(result -> marketPlaceClient.createBundleByList(idPsp, bundleRequestList))
              .orElseGet(() -> null);
    } catch (FeignException.BadRequest e) {
      throw new AppException(AppError.BUNDLE_REQUEST_DATA_ERROR, e.getMessage());
    } catch (FeignException.NotFound e) {
      throw new AppException(AppError.BUNDLE_NOT_FOUND_ERROR, e.getMessage());
    } catch (FeignException.Conflict e) {
      throw new AppException(AppError.BUNDLE_CONFLICT_ERROR, e.getMessage());
    } catch (FeignException.InternalServerError e) {
      throw new AppException(AppError.INTERNAL_SERVER_ERROR, e.getMessage());
    } catch (Exception e) {
      throw new AppException(AppError.UNKNOWN, e.getMessage());
    }
    return response;
  }

  // remove when @CosmosDBTrigger function will be used
  public void setCDIToProcessingStatus(List<CDI> items) {
    for (CDI cdi : items) {
      cdi.setCdiStatus(StatusType.PROCESSING);
      CDI updated = this.updateCDI(cdi);
      log.info(
          String.format(
              "CDI status updated [idCdi=%s, status=%s]",
              Optional.ofNullable(updated).map(result -> updated.getIdCdi()),
              Optional.ofNullable(updated).map(result -> updated.getCdiStatus())));
    }
  }
}
