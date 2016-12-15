package org.nrg.containers.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.containers.config.ContainerExecutionTestConfig;
import org.nrg.containers.services.ContainerExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(classes = ContainerExecutionTestConfig.class)
public class ContainerExecutionTest {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ContainerExecutionService containerExecutionService;

    @Test
    public void testSpringConfiguration() {
        assertThat(containerExecutionService, not(nullValue()));
    }
}
