package org.nrg.actions.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.actions.config.AceRestApiTestConfig;
import org.nrg.actions.model.ActionContextExecution;
import org.nrg.actions.model.ActionContextExecutionDto;
import org.nrg.actions.model.ResolvedCommand;
import org.nrg.actions.services.AceService;
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

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = AceRestApiTestConfig.class)
public class AceRestApiTest {

    private MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper();
    private final MediaType JSON = MediaType.APPLICATION_JSON_UTF8;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private AceService mockAceService;
//
//    @Autowired
//    private CommandService mockCommandService;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void testExecuteAce() throws Exception {
        final String path = "/aces";

        final String aceDtoJson =
                "{\"action-id\": 1, " +
                        "\"command-id\": 2," +
                        "\"description\": \"echo the label of an XNAT session\"," +
                        "\"inputs\": [" +
                        "{\"command-variable-name\": \"message\"," +
                        "\"name\": \"label\"," +
                        "\"value\": \"test_expt\"}]," +
                        "\"name\": \"echo-session\"," +
                        "\"project\": \"test\"," +
                        "\"root-id\": \"XNAT_E00001\"}";
        final ActionContextExecutionDto aceDto =
                mapper.readValue(aceDtoJson, ActionContextExecutionDto.class);

        final String resolvedCommandJson =
                "{\"docker-image-id\": \"abc123\"," +
                "\"run\": [\"/bin/sh\", \"-c\", \"echo test_expt\"]}";
        final ResolvedCommand resolvedCommand =
                mapper.readValue(resolvedCommandJson, ResolvedCommand.class);
        resolvedCommand.setCommandId(aceDto.getCommandId());

        final ActionContextExecution ace = new ActionContextExecution(aceDto, resolvedCommand);
        when(mockAceService.executeAce(aceDto)).thenReturn(ace);

        final MockHttpServletRequestBuilder request =
                post(path).content(aceDtoJson).contentType(JSON);

        final String response =
                mockMvc.perform(request)
                        .andExpect(status().isCreated())
                        .andExpect(content().contentType(JSON))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final ActionContextExecution aceResponse = mapper.readValue(response, ActionContextExecution.class);
        assertEquals(ace, aceResponse);
    }
}
