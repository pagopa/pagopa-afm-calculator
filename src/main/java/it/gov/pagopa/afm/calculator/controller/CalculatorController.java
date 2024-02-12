package it.gov.pagopa.afm.calculator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.afm.calculator.model.PaymentOption;
import it.gov.pagopa.afm.calculator.model.PaymentOptionByPsp;
import it.gov.pagopa.afm.calculator.model.PaymentOptionByPspMulti;
import it.gov.pagopa.afm.calculator.model.PaymentOptionMulti;
import it.gov.pagopa.afm.calculator.model.ProblemJson;
import it.gov.pagopa.afm.calculator.model.PspSearchCriteria;
import it.gov.pagopa.afm.calculator.model.calculator.BundleOption;
import it.gov.pagopa.afm.calculator.service.CalculatorService;
import java.util.List;
import javax.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@Tag(name = "Calculator", description = "Everything about Calculator business logic")
public class CalculatorController {

  @Autowired CalculatorService calculatorService;

  @Operation(
      summary = "Get taxpayer fees of the specified idPSP",
      security = {@SecurityRequirement(name = "ApiKey")},
      tags = {"Calculator"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Ok",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = BundleOption.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemJson.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema())),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemJson.class))),
        @ApiResponse(
            responseCode = "422",
            description = "Unable to process the request",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemJson.class))),
        @ApiResponse(
            responseCode = "429",
            description = "Too many requests",
            content = @Content(schema = @Schema())),
        @ApiResponse(
            responseCode = "500",
            description = "Service unavailable",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemJson.class)))
      })
  @PostMapping(
      value = "/psps/{idPsp}/fees",
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public BundleOption getFeesByPsp(
      @Parameter(description = "PSP identifier", required = true) @PathVariable("idPsp")
          String idPsp,
      @RequestBody @Valid PaymentOptionByPsp paymentOptionByPsp,
      @RequestParam(required = false, defaultValue = "10") Integer maxOccurrences,
      @RequestParam(required = false, defaultValue = "true")
          @Parameter(
              description =
                  "Flag for the exclusion of Poste bundles: false -> excluded, true or null ->"
                      + " included")
          String allCcp) {
    PaymentOption paymentOption =
        PaymentOption.builder()
            .paymentAmount(paymentOptionByPsp.getPaymentAmount())
            .primaryCreditorInstitution(paymentOptionByPsp.getPrimaryCreditorInstitution())
            .paymentMethod(paymentOptionByPsp.getPaymentMethod())
            .touchpoint(paymentOptionByPsp.getTouchpoint())
            .idPspList(
                List.of(
                    PspSearchCriteria.builder()
                        .idPsp(idPsp)
                        .idChannel(paymentOptionByPsp.getIdChannel())
                        .idBrokerPsp(paymentOptionByPsp.getIdBrokerPsp())
                        .build()))
            .transferList(paymentOptionByPsp.getTransferList())
            .bin(paymentOptionByPsp.getBin())
            .build();
    return calculatorService.calculate(
        paymentOption, maxOccurrences, StringUtils.isBlank(allCcp) || Boolean.parseBoolean(allCcp));
  }

  @Operation(
      summary = "Get taxpayer fees of all or specified idPSP",
      security = {@SecurityRequirement(name = "ApiKey")},
      tags = {"Calculator"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Ok",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = BundleOption.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemJson.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema())),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemJson.class))),
        @ApiResponse(
            responseCode = "422",
            description = "Unable to process the request",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemJson.class))),
        @ApiResponse(
            responseCode = "429",
            description = "Too many requests",
            content = @Content(schema = @Schema())),
        @ApiResponse(
            responseCode = "500",
            description = "Service unavailable",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemJson.class)))
      })
  @PostMapping(
      value = "/fees",
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public BundleOption getFees(
      @RequestBody @Valid PaymentOption paymentOption,
      @RequestParam(required = false, defaultValue = "10") Integer maxOccurrences,
      @RequestParam(required = false, defaultValue = "true")
          @Parameter(
              description =
                  "Flag for the exclusion of Poste bundles: false -> excluded, true or null ->"
                      + " included")
          String allCcp) {
    return calculatorService.calculate(
        paymentOption, maxOccurrences, StringUtils.isBlank(allCcp) || Boolean.parseBoolean(allCcp));
  }

  @Operation(
      summary = "Get taxpayer fees of the specified idPSP with ECs contributions",
      security = {@SecurityRequirement(name = "ApiKey")},
      tags = {"Calculator"})
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "200",
              description = "Ok",
              content =
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = BundleOption.class))),
          @ApiResponse(
              responseCode = "400",
              description = "Bad Request",
              content =
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ProblemJson.class))),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized",
              content = @Content(schema = @Schema())),
          @ApiResponse(
              responseCode = "404",
              description = "Not Found",
              content =
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ProblemJson.class))),
          @ApiResponse(
              responseCode = "422",
              description = "Unable to process the request",
              content =
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ProblemJson.class))),
          @ApiResponse(
              responseCode = "429",
              description = "Too many requests",
              content = @Content(schema = @Schema())),
          @ApiResponse(
              responseCode = "500",
              description = "Service unavailable",
              content =
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ProblemJson.class)))
      })
  @PostMapping(
      value = "/psps/{idPsp}/fees/multi",
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public BundleOption getFeesByPspMulti(
      @Parameter(description = "PSP identifier", required = true) @PathVariable("idPsp")
      String idPsp,
      @RequestBody @Valid PaymentOptionByPspMulti paymentOptionByPsp,
      @RequestParam(required = false, defaultValue = "10") Integer maxOccurrences,
      @RequestParam(required = false, defaultValue = "true")
      @Parameter(
          description =
              "Flag for the exclusion of Poste bundles: false -> excluded, true or null ->"
                  + " included")
      String allCcp) {
    PaymentOptionMulti paymentOption =
        PaymentOptionMulti.builder()
            .paymentMethod(paymentOptionByPsp.getPaymentMethod())
            .touchpoint(paymentOptionByPsp.getTouchpoint())
            .idPspList(
                List.of(
                    PspSearchCriteria.builder()
                        .idPsp(idPsp)
                        .idChannel(paymentOptionByPsp.getIdChannel())
                        .idBrokerPsp(paymentOptionByPsp.getIdBrokerPsp())
                        .build()))
            .bin(paymentOptionByPsp.getBin())
            .paymentNotice(paymentOptionByPsp.getPaymentNotice())
            .build();
    return BundleOption.builder()
        .belowThreshold(false)
        .build();
  }

  @Operation(
      summary = "Get taxpayer fees of all or specified idPSP with ECs contributions",
      security = {@SecurityRequirement(name = "ApiKey")},
      tags = {"Calculator"})
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "200",
              description = "Ok",
              content =
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = BundleOption.class))),
          @ApiResponse(
              responseCode = "400",
              description = "Bad Request",
              content =
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ProblemJson.class))),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized",
              content = @Content(schema = @Schema())),
          @ApiResponse(
              responseCode = "404",
              description = "Not Found",
              content =
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ProblemJson.class))),
          @ApiResponse(
              responseCode = "422",
              description = "Unable to process the request",
              content =
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ProblemJson.class))),
          @ApiResponse(
              responseCode = "429",
              description = "Too many requests",
              content = @Content(schema = @Schema())),
          @ApiResponse(
              responseCode = "500",
              description = "Service unavailable",
              content =
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ProblemJson.class)))
      })
  @PostMapping(
      value = "/fees/multi",
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public BundleOption getFeesMulti(
      @RequestBody @Valid PaymentOption paymentOption,
      @RequestParam(required = false, defaultValue = "10") Integer maxOccurrences,
      @RequestParam(required = false, defaultValue = "true")
      @Parameter(
          description =
              "Flag for the exclusion of Poste bundles: false -> excluded, true or null ->"
                  + " included")
      String allCcp) {
    return BundleOption.builder()
        .belowThreshold(false)
        .build();
  }
}
