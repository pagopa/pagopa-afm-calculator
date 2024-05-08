package it.gov.pagopa.afm.calculator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiGenerationTest {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private MockMvc mvc;

    @Test
    void swaggerSpringPlugin() throws Exception {
        saveOpenAPI("/v3/api-docs/v1/", "openapi-v1-dev-uat.json");
        saveOpenAPI("/v3/api-docs/v2/", "openapi-v2.json");
        saveOpenAPI("/v3/api-docs/node_v1/", "openapi-node-v1.json");
        saveOpenAPI("/v3/api-docs/node_v2/", "openapi-node-v2.json");
        saveOpenAPI("/v3/api-docs/v1_prod/", "openapi-v1.json");
    }

    private void saveOpenAPI(String fromUri, String toFile) throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(fromUri).accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andDo(
                        (result) -> {
                            assertNotNull(result);
                            assertNotNull(result.getResponse());
                            final String content = result.getResponse().getContentAsString();
                            assertFalse(content.isBlank());
                            assertFalse(content.contains("${"), "Generated swagger contains placeholders");
                            Object swagger =
                                    objectMapper.readValue(result.getResponse().getContentAsString(), Object.class);
                            String formatted =
                                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(swagger);
                            Path basePath = Paths.get("openapi/");
                            Files.createDirectories(basePath);
                            Files.write(basePath.resolve(toFile), formatted.getBytes());
                        });
    }
}
