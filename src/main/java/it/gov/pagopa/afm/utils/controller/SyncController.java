package it.gov.pagopa.afm.utils.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.gov.pagopa.afm.utils.entity.CDI;
import it.gov.pagopa.afm.utils.model.ProblemJson;
import it.gov.pagopa.afm.utils.model.bundle.BundleResponse;
import it.gov.pagopa.afm.utils.service.CDIService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class SyncController {

  @Autowired private CDIService cdiService;

  @Operation(
    summary = "API to retry the import of the CDIs and convert to bundles.",
    security = {@SecurityRequirement(name = "ApiKey")},
    operationId = "syncCDI",
    tags = {"Import CDI rest API"}
  )
  @ApiResponses(
    value = {
      @ApiResponse(
        responseCode = "200",
        description = "Obtained bundle list.",
        content =
            @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array =
                  @ArraySchema(
                    schema = @Schema(name = "BundleResponse", implementation = BundleResponse.class)
                  )
            )
      ),
      @ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = @Content(schema = @Schema())
      ),
      @ApiResponse(
        responseCode = "403",
        description = "Forbidden",
        content = @Content(schema = @Schema())
      ),
      @ApiResponse(
        responseCode = "429",
        description = "Too many requests",
        content = @Content(schema = @Schema())
      ),
      @ApiResponse(
        responseCode = "500",
        description = "Service unavailable.",
        content =
            @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ProblemJson.class)
            )
      )
    }
  )
  @GetMapping(value = "/cdis/sync", produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<List<BundleResponse>> syncCDI() {
    return new ResponseEntity<>(cdiService.syncCDI(), HttpStatus.OK);
  }

  @Operation(
    summary = "API to trigger the import of the CDIs and convert to bundles.",
    security = {@SecurityRequirement(name = "ApiKey")},
    operationId = "syncCDI",
    tags = {"Import CDI rest API"}
  )
  @ApiResponses(
    value = {
      @ApiResponse(
        responseCode = "200",
        description = "Obtained bundle list.",
        content =
            @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array =
                  @ArraySchema(
                    schema = @Schema(name = "BundleResponse", implementation = BundleResponse.class)
                  )
            )
      ),
      @ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = @Content(schema = @Schema())
      ),
      @ApiResponse(
        responseCode = "403",
        description = "Forbidden",
        content = @Content(schema = @Schema())
      ),
      @ApiResponse(
        responseCode = "429",
        description = "Too many requests",
        content = @Content(schema = @Schema())
      ),
      @ApiResponse(
        responseCode = "500",
        description = "Service unavailable.",
        content =
            @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ProblemJson.class)
            )
      )
    }
  )
  @PostMapping(value = "/cdis/sync", produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<List<BundleResponse>> syncCDI(@RequestBody List<CDI> cdis) {
    cdiService.saveCDIs(cdis);
    return ResponseEntity.status(200).build();
  }
}
