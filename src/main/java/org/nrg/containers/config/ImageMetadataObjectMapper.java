package org.nrg.containers.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.nrg.containers.metadata.ImageMetadata;
import org.nrg.framework.utilities.Reflection;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class ImageMetadataObjectMapper {
    @Bean
    public ObjectMapper objectMapper() throws IOException, ClassNotFoundException {

        List<Class<? extends ImageMetadata>> imageMetadataClasses = Lists.newArrayList();
        for (Class<?> clazz : Reflection.getClassesForPackage("org.nrg.containers.metadata")) {
            if (clazz != null && ImageMetadata.class.isAssignableFrom(clazz) && !clazz.equals(ImageMetadata.class)) {
                imageMetadataClasses.add(clazz.asSubclass(ImageMetadata.class));
            }
        }

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.registerSubtypes(imageMetadataClasses.toArray(new Class[imageMetadataClasses.size()]));
        return objectMapper;
    }
}
