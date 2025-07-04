package it.gov.pagopa.afm.calculator.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
import org.springdoc.core.GroupedOpenApi;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class SwaggerConfig {

    private static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final String BASE_PATH = "afm/calculator-service";
    private static final String BASE_PATH_NODE = "afm/node/calculator-service";

    @Bean
    public OpenAPI customOpenAPI(
            @Value("${info.application.description}") String appDescription,
            @Value("${info.application.version}") String appVersion) {
        return new OpenAPI()
                .servers(buildOpenapiServers(List.of(BASE_PATH, BASE_PATH_NODE), BASE_PATH))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        "ApiKey",
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.APIKEY)
                                                .description("The API key to access this function app.")
                                                .name("Ocp-Apim-Subscription-Key")
                                                .in(SecurityScheme.In.HEADER)))
                .info(
                        new Info()
                                .title("PagoPA API Calculator Logic")
                                .version(appVersion)
                                .description(appDescription)
                                .termsOfService("https://www.pagopa.gov.it/"));
    }

    @Bean
    public GlobalOpenApiCustomizer sortOperationsAlphabetically() {
        return openApi -> {
            Paths paths =
                    openApi.getPaths().entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .collect(
                                    Paths::new,
                                    (map, item) -> map.addPathItem(item.getKey(), item.getValue()),
                                    Paths::putAll);

            paths.forEach(
                    (key, value) ->
                            value
                                    .readOperations()
                                    .forEach(
                                            operation -> {
                                                var responses =
                                                        operation.getResponses().entrySet().stream()
                                                                .sorted(Map.Entry.comparingByKey())
                                                                .collect(
                                                                        ApiResponses::new,
                                                                        (map, item) ->
                                                                                map.addApiResponse(item.getKey(), item.getValue()),
                                                                        ApiResponses::putAll);
                                                operation.setResponses(responses);
                                            }));
            openApi.setPaths(paths);
        };
    }

    @Bean
    public GlobalOpenApiCustomizer addCommonHeaders() {
        return openApi ->
                openApi
                        .getPaths()
                        .forEach(
                                (key, value) -> {

                                    // add Request-ID as request header
                                    value.addParametersItem(
                                            new Parameter()
                                                    .in("header")
                                                    .name(HEADER_REQUEST_ID)
                                                    .schema(new StringSchema())
                                                    .description(
                                                            "This header identifies the call, if not passed it is"
                                                                    + " self-generated. This ID is returned in the response."));

                                    // add Request-ID as response header
                                    value
                                            .readOperations()
                                            .forEach(
                                                    operation ->
                                                            operation
                                                                    .getResponses()
                                                                    .values()
                                                                    .forEach(
                                                                            response ->
                                                                                    response.addHeaderObject(
                                                                                            HEADER_REQUEST_ID,
                                                                                            new Header()
                                                                                                    .schema(new StringSchema())
                                                                                                    .description(
                                                                                                            "This header identifies the call"))));
                                });
    }

    @Bean
    public Map<String, GroupedOpenApi> configureGroupOpenApi(
            Map<String, GroupedOpenApi> groupOpenApi) {
        groupOpenApi.forEach(
                (id, groupedOpenApi) ->
                        groupedOpenApi
                                .getOpenApiCustomisers()
                                .add(
                                        openApi -> {
                                            var baseTitle = openApi.getInfo().getTitle();
                                            var group = groupedOpenApi.getDisplayName();
                                            String title = String.format("%s - %s", baseTitle, group);
                                            openApi.getInfo().setTitle(title);
                                            if (id.contains("node")) {
                                                openApi.setServers(
                                                        buildOpenapiServers(List.of(BASE_PATH_NODE), BASE_PATH_NODE));
                                            } else {
                                                openApi.setServers(buildOpenapiServers(List.of(BASE_PATH), BASE_PATH));
                                            }
                                            if (id.equals("v2") || id.equals("node_v2")) {
                                                openApi.setPaths(removeMultiFromPath(openApi.getPaths()));
                                            }
                                        }));
        return groupOpenApi;
    }

    private Paths removeMultiFromPath(Paths paths) {
        Paths updated = new Paths();
        paths.forEach(
                (k, v) -> {
                    if (k.contains("/multi")) {
                        updated.addPathItem(k.replace("/multi", ""), v);
                    } else {
                        updated.addPathItem(k, v);
                    }
                });
        return updated;
    }

    private List<Server> buildOpenapiServers(List<String> basePathList, String defaultBasePath) {
        return List.of(
                new Server().url("http://localhost:8080"),
                new Server()
                        .url("https://{host}{basePath}")
                        .variables(
                                new ServerVariables()
                                        .addServerVariable(
                                                "host",
                                                new ServerVariable()
                                                        ._enum(
                                                                List.of(
                                                                        "api.dev.platform.pagopa.it",
                                                                        "api.uat.platform.pagopa.it",
                                                                        "api.platform.pagopa.it"))
                                                        ._default("api.dev.platform.pagopa.it"))
                                        .addServerVariable(
                                                "basePath",
                                                new ServerVariable()._enum(basePathList)._default(defaultBasePath))));
    }
}
