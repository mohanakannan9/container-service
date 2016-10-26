package org.nrg.containers.rest;

import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.framework.annotations.XapiRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

//@Api(description = "XNAT Container Services REST API")
@XapiRestController
@RequestMapping(value = "/test")
public class TestApi {
  private static final Logger _log = LoggerFactory.getLogger(TestApi.class);

  private static final String JSON = MediaType.APPLICATION_JSON_UTF8_VALUE;
  private static final String PLAIN_TEXT = MediaType.TEXT_PLAIN_VALUE;

  @RequestMapping(method = GET, produces = PLAIN_TEXT)
  public String getTest()
      throws NoServerPrefException {
    _log.debug("getTest: I hit the test method");
    return "I hit the test method";
  }
}
