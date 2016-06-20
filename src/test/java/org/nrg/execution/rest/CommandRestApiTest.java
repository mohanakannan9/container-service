package org.nrg.execution.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.execution.config.CommandRestApiTestConfig;
import org.nrg.execution.model.Command;
import org.nrg.execution.services.CommandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@Transactional
@ContextConfiguration(classes = CommandRestApiTestConfig.class)
public class CommandRestApiTest {

    private MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper();
    private final MediaType JSON = MediaType.APPLICATION_JSON_UTF8;
    private final MediaType XML = MediaType.APPLICATION_XML;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private CommandService commandService;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void testGetAll() throws Exception {
        final String path = "/commands";

        final String commandJson =
                "{\"name\": \"one\", \"docker-image\":\"abc123\"}";
        final Command command = mapper.readValue(commandJson, Command.class);
        final Command created = commandService.create(command);

//        when(commandService.getAll()).thenReturn(Lists.newArrayList(command));

        final MockHttpServletRequestBuilder request = get(path);

        final String response =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(JSON))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final List<Command> commandResponseList = mapper.readValue(response, new TypeReference<List<Command>>() {});
        assertThat(commandResponseList, hasSize(1));
        final Command commandResponse = commandResponseList.get(0);
        assertNotEquals(0L, commandResponse.getId());
        assertEquals(created.getId(), commandResponse.getId());
        assertEquals("one", commandResponse.getName());
        assertEquals("abc123", commandResponse.getDockerImage());
    }

    @Test
    public void testGet() throws Exception {
        final String path = "/commands/1";

        final String commandJson =
                "{\"name\": \"one\", \"docker-image\":\"abc123\"}";
        final Command command = mapper.readValue(commandJson, Command.class);
        final Command created = commandService.create(command);

        final MockHttpServletRequestBuilder request = get(path);

        final String response =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(JSON))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final Command commandResponse = mapper.readValue(response, Command.class);
        assertNotEquals(0L, commandResponse.getId());
        assertEquals(created.getId(), commandResponse.getId());
        assertEquals("one", commandResponse.getName());
        assertEquals("abc123", commandResponse.getDockerImage());
    }

    @Test
    public void testCreate() throws Exception {
        final String path = "/commands";

        final String commandJson =
                "{\"name\": \"toCreate\", \"docker-image\":\"abc123\"}";

        final MockHttpServletRequestBuilder request =
                post(path).content(commandJson).contentType(JSON);

        final String response =
                mockMvc.perform(request)
                        .andExpect(status().isCreated())
                        .andExpect(content().contentType(JSON))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final Command commandResponse = mapper.readValue(response, Command.class);
        assertNotEquals(0L, commandResponse.getId());

        final Command retrieved = commandService.retrieve(commandResponse.getId());
        assertNotEquals(0L, retrieved.getId());
        assertEquals(retrieved.getId(), commandResponse.getId());
        assertEquals("toCreate", retrieved.getName());
        assertEquals(retrieved.getName(), commandResponse.getName());
        assertEquals("abc123", retrieved.getDockerImage());
        assertEquals(retrieved.getDockerImage(), commandResponse.getDockerImage());

        // Errors
        // Violate unique name+docker-image-id constraint (we'll trigger that by performing the same 'create' request again)
        mockMvc.perform(request).andExpect(status().isBadRequest());

        // No 'Content-type' header
        final MockHttpServletRequestBuilder noContentType = post(path).content(commandJson);
        mockMvc.perform(noContentType)
                .andExpect(status().isUnsupportedMediaType());

        // Bad 'Accepts' header
        final MockHttpServletRequestBuilder badAccept =
                post(path).content(commandJson)
                        .contentType(JSON)
                        .accept(XML);
        mockMvc.perform(badAccept)
                .andExpect(status().isNotAcceptable());
    }

    @Test
    public void testDelete() throws Exception {
        final String pathTemplate = "/commands/%d";

        final String commandJson =
                "{\"name\": \"toDelete\", \"docker-image\":\"abc123\"}";
        final Command command = mapper.readValue(commandJson, Command.class);
        commandService.create(command);

        final String path = String.format(pathTemplate, command.getId());
        final MockHttpServletRequestBuilder request = delete(path);


        mockMvc.perform(request)
                .andExpect(status().isNoContent());

        final List<Command> commandsWithName = commandService.findByName("toDelete");
        assertNull(commandsWithName);
    }
}
