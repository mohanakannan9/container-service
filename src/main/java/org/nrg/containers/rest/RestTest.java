package org.nrg.containers.rest;

import com.google.common.collect.Lists;
import org.nrg.containers.model.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/test")
public class RestTest {
    private static final Logger _log = LoggerFactory.getLogger(RestTest.class);

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public String testtesttest() {
        String message = "I hit the test method.";
        _log.debug("LOGGING: "+message);
        return message;
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public List<Image> listTest() {
        _log.debug("I am returning a list.");
        List<Image> list = Lists.newArrayList();
        list.add(new Image("Foo"));
        list.add(new Image("Bar"));
        return list;
    }

    @RequestMapping(value = "/error", method = RequestMethod.GET)
    public ResponseEntity<String> errorTest() {
        String message = "I will return an http 500 error code.";
        _log.debug(message);
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @RequestMapping(value = "/code/{code:\\d{3}}", method = RequestMethod.GET)
    public ResponseEntity<String> codeTestParams(@PathVariable("code") Integer code) {
        _log.debug("I will return an http "+code+" code.");
        return new ResponseEntity<>("<h2>Code "+code+"</h2>", HttpStatus.valueOf(code));
    }
}