package org.nrg.execution.model.xnat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;


public class XnatModelTest {
    private static final String FILE_JSON = "{\"name\":\"file.txt\", \"path\":\"/path/to/files/file.txt\", " +
            "\"tags\":[\"squishy\",\"jovial\"], \"format\":\"TEXT\", \"content\":\"TEXT\"}";
    private static final String RESOURCE_JSON = "{\"id\":1, \"label\":\"a_resource\", " +
            "\"directory\":\"/path/to/files\", \"files\":[" + FILE_JSON + "]}";

    private static final String SESSION_JSON = "{\"id\":\"1\", \"label\":\"a_session\", " +
            "\"xsiType\":\"xnat:fakesessiondata\", \"resources\":[" + RESOURCE_JSON + "]}";

    private final ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setup() {
        Configuration.setDefaults(new Configuration.Defaults() {

            private final JsonProvider jsonProvider = new JacksonJsonProvider();
            private final MappingProvider mappingProvider = new JacksonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                return Sets.newHashSet(Option.ALWAYS_RETURN_LIST, Option.DEFAULT_PATH_LEAF_TO_NULL);
            }
        });
    }

    @Test
    public void testDeserializeFile() throws Exception {
        final File file = mapper.readValue(FILE_JSON, File.class);
        assertEquals("file.txt", file.getName());
        assertEquals("/path/to/files/file.txt", file.getPath());
        assertEquals(Lists.newArrayList("squishy", "jovial"), file.getTags());
        assertEquals("TEXT", file.getFormat());
        assertEquals("TEXT", file.getContent());
    }

    @Test
    public void testDeserializeResource() throws Exception {
        final File file = mapper.readValue(FILE_JSON, File.class);
        final Resource resource = mapper.readValue(RESOURCE_JSON, Resource.class);

        assertEquals((Integer) 1, resource.getId());
        assertEquals("a_resource", resource.getLabel());
        assertEquals("/path/to/files", resource.getDirectory());
        assertEquals(Lists.newArrayList(file), resource.getFiles());
    }

    @Test
    public void testDeserializeSession() throws Exception {
        final Resource resource = mapper.readValue(RESOURCE_JSON, Resource.class);
        final Session session = mapper.readValue(SESSION_JSON, Session.class);

        assertEquals("1", session.getId());
        assertEquals("a_session", session.getLabel());
        assertEquals("xnat:fakesessiondata", session.getXsiType());
        assertEquals(Lists.newArrayList(resource), session.getResources());
        assertNull(session.getScans());
        assertNull(session.getAssessors());
    }

    @Test
    public void testJsonPath() throws Exception {
        final Resource expected = mapper.readValue(RESOURCE_JSON, Resource.class);
        final List<Resource> resources = JsonPath.parse(SESSION_JSON).read("$.resources[*]", new TypeRef<List<Resource>>(){});

        assertThat(resources, hasSize(1));
        assertThat(resources.get(0), instanceOf(Resource.class));
        assertEquals(expected, resources.get(0));
    }
}
