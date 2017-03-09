package org.nrg.containers.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nrg.containers.config.ContainerExecutionTestConfig;
import org.nrg.containers.services.ContainerExecutionService;
import org.nrg.xft.security.UserI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(classes = ContainerExecutionTestConfig.class)
public class ContainerExecutionTest {

    @Autowired private ObjectMapper mapper;
    @Autowired private ContainerExecutionService containerExecutionService;

    @Test
    public void testSpringConfiguration() {
        assertThat(containerExecutionService, not(nullValue()));
    }

    @Test
    @DirtiesContext
    public void testSave() throws Exception {
        final ResolvedCommand resolvedCommand = new ResolvedDockerCommand();
        final String containerId = "abc123";
        final UserI mockAdmin = Mockito.mock(UserI.class);
        when(mockAdmin.getLogin()).thenReturn("admin");

        final ContainerExecution created = containerExecutionService.save(resolvedCommand, containerId, mockAdmin);
        assertNotEquals(0L, created);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final ContainerExecution retrieved = containerExecutionService.get(created.getId());
        assertEquals(created, retrieved);
    }
}
