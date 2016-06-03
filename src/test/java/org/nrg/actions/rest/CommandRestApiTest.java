package org.nrg.actions.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.actions.config.CommandRestApiTestConfig;
import org.nrg.actions.model.Command;
import org.nrg.actions.services.CommandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = CommandRestApiTestConfig.class)
public class CommandRestApiTest {

    private MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper();
    private final MediaType JSON = MediaType.APPLICATION_JSON_UTF8;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private CommandService mockCommandService;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void testGetAll() throws Exception {
        final String path = "/commands";

        final String commandJson =
                "{\"id\": 1, \"name\": \"one\", \"type\":\"docker-image\"}";
        final Command command = mapper.readValue(commandJson, Command.class);

        when(mockCommandService.getAll()).thenReturn(Lists.newArrayList(command));

        final MockHttpServletRequestBuilder request =
                get(path).accept(JSON);

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
        assertEquals(Long.valueOf(1), (Long)commandResponse.getId());
        assertEquals("one", commandResponse.getName());
    }
}
