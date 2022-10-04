package it.gov.pagopa.afm.calculator;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.afm.calculator.entity.Bundle;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@UtilityClass
public class TestUtil {

    /**
     * @param relativePath Path from source root of the json file
     * @return the Json string read from the file
     * @throws IOException if an I/O error occurs reading from the file or a malformed or unmappable byte sequence is read
     */
    public String readStringFromFile(String relativePath) throws IOException {
        ClassLoader classLoader = TestUtil.class.getClassLoader();
        File file = new File(Objects.requireNonNull(classLoader.getResource(relativePath)).getPath());
        return Files.readString(file.toPath());
    }

    /**
     * @param relativePath Path from source root of the file
     * @return the requested file
     */
    public File readFile(String relativePath) {
        ClassLoader classLoader = TestUtil.class.getClassLoader();
        return new File(Objects.requireNonNull(classLoader.getResource(relativePath)).getFile());
    }

    /**
     * @param object to map into the Json string
     * @return object as Json string
     * @throws JsonProcessingException if there is an error during the parsing of the object
     */
    public String toJson(Object object) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(object);
    }

    /**
     * @param relativePath a relative path of valid JSON file
     * @param valueType class to convert the JSON
     * @param <T> a Java Class
     * @return an object of type T using the JSON
     * @throws IOException if an IO error occurs
     */
    public <T> T readObjectFromFile(String relativePath, Class<T> valueType) throws IOException {
        var jsonFile = readFile(relativePath);
        return new ObjectMapper().readValue(jsonFile, valueType);
    }


    public static List<Bundle> getMockBundleList() {
        return Collections.singletonList(getMockBuild());
    }

    private static Bundle getMockBuild() {
        return Bundle.builder()
                .id("1")
                .name("bundle1")
                .idPsp("123")
                .build();
    }

}
