package org.nrg.containers.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.nrg.containers.config.ImageMetadataTestConfig;
import org.nrg.containers.metadata.service.ImageMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@Rollback
@Transactional
@ContextConfiguration(classes = ImageMetadataTestConfig.class)
public class ImageMetadataTest {
    @Autowired
    private ImageMetadataService metadataService;

    @Autowired
    private ObjectMapper mapper;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testSpringConfiguration() {
        assertThat(metadataService, not(nullValue()));
    }

    @Test
    public void testGetByImageId() {
        // TODO
    }

    @Test
    public void testDeserializeMetadata() throws IOException {

        final List<Map<Object, Object>> alphaArgs = Lists.newArrayList();
        alphaArgs.add(ImmutableMap.builder()
                .put("name", "foo")
                .put("type", "string")
                .put("description", "A Foo that Bars")
                .put("value", "FooFooFoo")
                .put("user-settable", "false")
                .build());
        alphaArgs.add(ImmutableMap.builder()
                .put("name", "bar")
                .put("type", "an invalid type")
                .put("description", "A Bar that Foos")
                .put("value", "Baz")
                .put("user-settable", "true")
                .build());

        final Set<String> mountsIn = Sets.newHashSet("/input1", "/input2");
        final Map<String, Object> metadataMap =
                ImmutableMap.of(
                        "xnat.version", "alpha",
                        "xnat.mounts.in", mountsIn,
                        "xnat.mounts.out", "/output",
                        "xnat.execution", "La la la la",
                        "xnat.args", alphaArgs);

        final Map<String, String> invalidMetadataMap =
                ImmutableMap.of(
                        "xnat.version", "XXX"
                );

        final String alphaJsonString = mapper.writeValueAsString(metadataMap);
        final ImageMetadata metadata =
                mapper.readValue(alphaJsonString, ImageMetadata.class);

        assertThat(metadata, is(not(nullValue())));
        assertEquals(metadata.version(), "alpha");

        assertThat(metadata.getImageId(), isEmptyOrNullString());
        assertEquals(metadata.getExecution(), "La la la la");
        assertThat(metadata.getMountsOut(), contains("/output"));
        assertThat(metadata.getMountsIn(), containsInAnyOrder("/input1", "/input2"));

//        // Invalid
//        final String invalidJsonString = mapper.writeValueAsString(invalidMetadataMap);
//
//        thrown.expect(JsonMappingException.class);
//        final ImageMetadata metadataInvalid =
//                mapper.readValue(invalidJsonString, ImageMetadata.class);
    }

    @Test
    public void testSerializeMetadata() throws IOException {
        // alpha
        final ImageMetadata alphaMetadata =
                ImageMetadata.builder()
                .mountsIn("/input1", "/input2")
                .mountOut("/output")
                .execution("La la la la")
                .arg(ImageMetadataArg.builder()
                    .name("foo")
                    .type("string")
                    .description("A Foo that Bars")
                    .value("FooFooFoo")
                    .userSettable(false)
                    .build())
                .arg(ImageMetadataArg.builder()
                    .name("bar")
                    .type("an invalid type")
                    .description("A Bar that Foos")
                    .value("Baz")
                    .userSettable(true)
                    .validationRegex(".*")
                    .flag("-o")
                    .build())
                .build();

        // alpha
        final String alphaSerialized = mapper.writeValueAsString(alphaMetadata);
        final ImageMetadata alphaRedeserialized =
            mapper.readValue(alphaSerialized, ImageMetadata.class);
        assertEquals(alphaMetadata, alphaRedeserialized);
    }

    @Test
    public void testSave() throws Exception {
        final ImageMetadata alphaMetadata =
                ImageMetadata.builder()
                        .mountsIn("/input1", "/input2")
                        .mountOut("/output")
                        .execution("La la la la")
                        .arg(ImageMetadataArg.builder()
                                .name("foo")
                                .type("string")
                                .description("A Foo that Bars")
                                .value("FooFooFoo")
                                .userSettable(false)
                                .build())
                        .arg(ImageMetadataArg.builder()
                                .name("bar")
                                .type("an invalid type")
                                .description("A Bar that Foos")
                                .value("Baz")
                                .userSettable(true)
                                .validationRegex(".*")
                                .flag("-o")
                                .build())
                        .build();
        assertEquals(0, alphaMetadata.getId());
        metadataService.create(alphaMetadata);
        final long id = alphaMetadata.getId();
        assertNotEquals(0, id);
        final ImageMetadata retrieved = metadataService.retrieve(id);

        assertEquals(retrieved, alphaMetadata);
    }
}
