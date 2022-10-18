//package it.gov.pagopa.afm.calculator.controller;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.media.Content;
//import io.swagger.v3.oas.annotations.media.Schema;
//import io.swagger.v3.oas.annotations.responses.ApiResponse;
//import io.swagger.v3.oas.annotations.responses.ApiResponses;
//import io.swagger.v3.oas.annotations.security.SecurityRequirement;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import it.gov.pagopa.afm.calculator.model.ProblemJson;
//import it.gov.pagopa.afm.calculator.model.configuration.Configuration;
//import it.gov.pagopa.afm.calculator.service.ConfigurationService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.MediaType;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//
//@RestController()
//@Tag(name = "Configuration", description = "Everything about Calculator Configuration")
//public class ConfigurationController {
//
//    @Autowired
//    ConfigurationService configurationService;
//
//    @Operation(summary = "Get calculator configuration", security = {@SecurityRequirement(name = "ApiKey")}, tags = {"Configuration"})
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Configuration.class))),
//            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemJson.class))),
//            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema())),
//            @ApiResponse(responseCode = "429", description = "Too many requests", content = @Content(schema = @Schema())),
//            @ApiResponse(responseCode = "500", description = "Service unavailable", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemJson.class)))})
//    @GetMapping(value = "/configuration", produces = {MediaType.APPLICATION_JSON_VALUE})
//    public Configuration getConfiguration() {
//        return configurationService.get();
//    }
//}
