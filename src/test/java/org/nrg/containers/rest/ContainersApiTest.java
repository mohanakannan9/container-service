//package org.nrg.containers.rest;
//
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.google.common.collect.ImmutableMap;
//import com.google.common.collect.Lists;
//import com.google.common.collect.Maps;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.nrg.containers.config.RestApiTestConfig;
//import org.nrg.containers.exceptions.DockerServerException;
//import org.nrg.containers.exceptions.NoServerPrefException;
//import org.nrg.containers.exceptions.NotFoundException;
//import org.nrg.containers.model.Container;
//import org.nrg.containers.model.DockerHub;
//import org.nrg.containers.model.DockerServer;
//import org.nrg.containers.services.ContainerService;
//import org.nrg.prefs.exceptions.InvalidPreferenceName;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//import org.springframework.test.context.web.WebAppConfiguration;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//import org.springframework.web.context.WebApplicationContext;
//
//import javax.servlet.ServletContext;
//import java.io.IOException;
//import java.io.UnsupportedEncodingException;
//import java.net.URLEncoder;
//import java.util.List;
//import java.util.Map;
//
//import static java.nio.charset.StandardCharsets.UTF_8;
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.equalTo;
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNotNull;
//import static org.mockito.Mockito.doNothing;
//import static org.mockito.Mockito.doThrow;
//import static org.mockito.Mockito.reset;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@RunWith(SpringJUnit4ClassRunner.class)
//@WebAppConfiguration
//@ContextConfiguration(classes = RestApiTestConfig.class)
//@SuppressWarnings("ThrowableInstanceNeverThrown")
//public class ContainersApiTest {
//
//    private MockMvc mockMvc;
//    final ObjectMapper mapper = new ObjectMapper();
//
//    final MediaType FORM = MediaType.APPLICATION_FORM_URLENCODED;
//    final MediaType JSON = MediaType.APPLICATION_JSON_UTF8;
//    final MediaType PLAIN_TEXT = MediaType.TEXT_PLAIN;
//
//    final NotFoundException NOT_FOUND_EXCEPTION = new NotFoundException("Some cool message");
//    final NoServerPrefException NO_SERVER_PREF_EXCEPTION = new NoServerPrefException("message");
//    final InvalidPreferenceName INVALID_PREFERENCE_NAME = new InvalidPreferenceName("*invalid name*");
//    final DockerServerException CONTAINER_SERVER_EXCEPTION =
//        new DockerServerException("Your server dun goofed.");
//
//    final static String MOCK_CONTAINER_HOST = "fake://host.url";
//    final static String MOCK_CONTAINER_CERT_PATH = "/path/to/file";
//    final static DockerServer MOCK_CONTAINER_SERVER =
//        new DockerServer(MOCK_CONTAINER_HOST, MOCK_CONTAINER_CERT_PATH);
//
//    @Autowired
//    private WebApplicationContext wac;
//
//    @Autowired
//    private ContainerService service;
//
//    @Before
//    public void setup() throws InvalidPreferenceName {
//        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
//
//        reset(service); // To ensure test mock objects are isolated
//    }
//
//    @Test
//    public void testWebApplicationContextSetup() {
//        assertNotNull(service);
//        assertNotNull(wac);
//
//        ServletContext childServletContext = wac.getServletContext();
//        assertNotNull(childServletContext);
//    }
//
//    @Test
//    public void testGetContainers() throws Exception {
//        final List<Container> mockContainerList = Lists.newArrayList(new Container("0", "first"), new Container("1", "second"));
//
//        final String path = "/containers";
//        final MockHttpServletRequestBuilder request = get(path).accept(JSON);
//
//        when(service.getContainers())
//                .thenReturn(mockContainerList)                          // Happy path
//                .thenThrow(NO_SERVER_PREF_EXCEPTION);    // No server pref defined
//
//        // Happy path
//        final String response =
//                mockMvc.perform(request)
//                        .andExpect(status().isOk())
//                        .andExpect(content().contentType(JSON))
//                        .andReturn()
//                        .getResponse()
//                        .getContentAsString();
//
//        List<Container> responseImageList = mapper.readValue(response, new TypeReference<List<Container>>(){});
//        assertThat(responseImageList, equalTo(mockContainerList));
//
//        // No server pref defined
//        mockMvc.perform(request)
//                .andExpect(status().isFailedDependency());
//    }
//
//    @Test
//    public void testGetContainersWithParams() throws Exception {
////        final List<Container> mockContainerList = ContainerMocks.FIRST_AND_SECOND;
////
////        final Map<String, List<String>> blank = Maps.newHashMap();
////
////        final String path = "/containers";
////        final MockHttpServletRequestBuilder request = get(path).accept(JSON);
////
////        when(service.getContainers(blank))
////                .thenReturn(mockContainerList)                          // Happy path
////                .thenThrow(NO_SERVER_PREF_EXCEPTION);    // No server pref defined
////
////        // Happy path
////        final String response =
////                mockMvc.perform(request)
////                        .andExpect(status().isOk())
////                        .andExpect(content().contentType(JSON))
////                        .andReturn()
////                        .getResponse()
////                        .getContentAsString();
////
////        List<Container> responseImageList = mapper.readValue(response, new TypeReference<List<Container>>(){});
////        assertThat(responseImageList, equalTo(mockContainerList));
////
////        // No server pref defined
////        mockMvc.perform(request)
////                .andExpect(status().isFailedDependency());
//    }
//
//    @Test
//    public void testGetContainer() throws Exception {
//        final String id = "foo";
//        final Container mockContainer = new Container("foo", "Great");
//
//        final String path = "/containers/" + id;
//        final MockHttpServletRequestBuilder request = get(path).accept(JSON);
//
//        when(service.getContainer(id))
//                .thenReturn(mockContainer)                              // Happy path
//                .thenThrow(NOT_FOUND_EXCEPTION)          // Not found
//                .thenThrow(NO_SERVER_PREF_EXCEPTION);    // No server pref defined
//
//        // Happy path
//        final String responseById =
//                mockMvc.perform(request)
//                        .andExpect(status().isOk())
//                        .andExpect(content().contentType(JSON))
//                        .andReturn()
//                        .getResponse()
//                        .getContentAsString();
//
//        final Container containerById = mapper.readValue(responseById, Container.class);
//        assertThat(containerById, equalTo(mockContainer));
//
//        // Not found
//        mockMvc.perform(request)
//                .andExpect(status().isNotFound());
//
//        // No server pref defined
//        mockMvc.perform(request)
//                .andExpect(status().isFailedDependency());
//    }
//
//    @Test
//    public void testGetContainerStatus() throws Exception {
//        final String id = "foo";
//        final String status = "Great";
//
//        final String path = "/containers/" + id + "/status";
//        final MockHttpServletRequestBuilder request = get(path);
//
//        when(service.getContainerStatus(id))
//                .thenReturn(status)                                     // Happy path
//                .thenThrow(NO_SERVER_PREF_EXCEPTION);    // No server pref defined
//
//        // Happy path
//        final String response =
//                mockMvc.perform(request)
//                        .andExpect(status().isOk())
//                        .andExpect(content().contentType(PLAIN_TEXT))
//                        .andReturn()
//                        .getResponse()
//                        .getContentAsString();
//
//        assertThat(response, equalTo(status));
//
//        // No server pref defined
//        mockMvc.perform(request)
//                .andExpect(status().isFailedDependency());
//    }
//
//    @Test
//    public void testVerbContainer() throws Exception {
////        final Map<String, String> verbAndStatus =
//    }
//
//    @Test
//    public void testGetContainerLogs() throws Exception {
//        // TODO
//    }
//
//    @Test
//    public void testLaunch() throws Exception {
//        final String path = "/containers/launch";
//        // TODO
//    }
//
//    @Test
//    public void testLaunchOn() throws Exception {
//
//    }
//
//    @Test
//    public void testLaunchFromScript() throws Exception {
//        final String scriptId = "123";
//        final Map<String, String> otherArgs =
//            ImmutableMap.of("arg1", "val1", "arg2", "val2");
//
//        final String path = "/containers/launch/script/" + scriptId;
//
//        // REQUEST 0: No "onServer" param (defaults to false)
//        final MockHttpServletRequestBuilder request =
//            post(path).contentType(JSON).content(mapper.writeValueAsString(otherArgs));
//
//        when(service.launchFromScript(scriptId, otherArgs, false))
//            .thenReturn("yay")
//            .thenThrow(NOT_FOUND_EXCEPTION);
//
//        final String response =
//            mockMvc.perform(request)
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(PLAIN_TEXT))
//                .andReturn()
//                .getResponse()
//                .getContentAsString();
//
//        assertEquals("yay", response);
//
//        // Not found
//        mockMvc.perform(request).andExpect(status().isNotFound());
//    }
//
////    @Test
////    public void testPullByName() throws Exception {
////        final String imageName = "foo/bar";
////        final String noAuthUrl = "http://a.url";
////        final String authUrl = "http://different.url";
////        final String username = "foo";
////        final String password = "bar";
////
////        final String path = "/containers/pull";
////
////        final MockHttpServletRequestBuilder requestNoAuth =
////            get(path).param("image", imageName).param("hub", noAuthUrl);
////
////        doNothing().when(service).pullByName(imageName, noAuthUrl);
////
////        mockMvc.perform(requestNoAuth)
////            .andExpect(status().isOk());
////        verify(service, times(1)).pullByName(imageName, noAuthUrl);
////
////        final MockHttpServletRequestBuilder requestWithAuth = get(path)
////                .param("image", imageName)
////                .param("hub", authUrl)
////                .param("username", username)
////                .param("password", password);
////
////        doNothing().when(service).pullByName(imageName, authUrl, username, password);
////
////        mockMvc.perform(requestWithAuth)
////            .andExpect(status().isOk());
////        verify(service, times(1)).pullByName(imageName, authUrl, username, password);
////    }
//
//    public String urlEncode(final String raw) throws UnsupportedEncodingException {
//        return URLEncoder.encode(raw, UTF_8.name());
//    }
//}
