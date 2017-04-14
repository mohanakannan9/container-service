package org.nrg.containers.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nrg.containers.config.ContainerEntityTestConfig;
import org.nrg.containers.model.container.entity.ContainerEntity;
import org.nrg.containers.services.ContainerEntityService;
import org.nrg.xft.security.UserI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(classes = ContainerEntityTestConfig.class)
public class ContainerEntityTest {

    @Autowired private ObjectMapper mapper;
    @Autowired private ContainerEntityService containerEntityService;

    @Test
    public void testSpringConfiguration() {
        assertThat(containerEntityService, not(nullValue()));
    }

    @Test
    @DirtiesContext
    public void testSave() throws Exception {
        final ResolvedCommand resolvedCommand = new ResolvedDockerCommand();
        resolvedCommand.setCommandId(1L);
        resolvedCommand.setXnatCommandWrapperId(1L);
        resolvedCommand.setImage("xnat/dcm2niix:1.0");
        resolvedCommand.setCommandInputValues(ImmutableMap.of("foo", "bar"));

        final String containerId = "abc123";
        final UserI mockAdmin = Mockito.mock(UserI.class);
        when(mockAdmin.getLogin()).thenReturn("admin");

        final ContainerEntity created = containerEntityService.save(resolvedCommand, containerId, mockAdmin);
        assertThat(created.getId(), is(not(0L)));

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final ContainerEntity retrieved = containerEntityService.get(created.getId());
        assertThat(retrieved, is(created));
    }
}
