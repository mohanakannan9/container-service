package org.nrg.containers.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nrg.containers.config.ContainerEntityTestConfig;
import org.nrg.containers.model.command.auto.Command;
import org.nrg.containers.model.command.auto.ResolvedCommand;
import org.nrg.containers.model.command.auto.ResolvedInputTreeNode;
import org.nrg.containers.model.command.auto.ResolvedInputValue;
import org.nrg.containers.model.container.auto.Container;
import org.nrg.containers.model.container.entity.ContainerEntity;
import org.nrg.containers.services.ContainerEntityService;
import org.nrg.xft.security.UserI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
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
        final ResolvedInputTreeNode<? extends Command.Input> inputTreeNode =
                ResolvedInputTreeNode.create(
                        Command.CommandInput.builder()
                                .name("foo")
                        .build(),
                        Collections.singletonList(ResolvedInputTreeNode.ResolvedInputTreeValueAndChildren.create(
                                ResolvedInputValue.builder().value("bar").build()
                        ))
                );
        final ResolvedCommand resolvedCommand = ResolvedCommand.builder()
                .commandId(1L)
                .commandName("a name")
                .wrapperId(1L)
                .wrapperName("another name")
                .image("xnat/dcm2niix:1.0")
                .resolvedInputTrees(Collections.<ResolvedInputTreeNode<? extends Command.Input>>singletonList(inputTreeNode))
                .commandLine("Anything I want")
                .build();

        final String containerId = "abc123";
        final String workflowId = "workflow";
        final UserI mockAdmin = Mockito.mock(UserI.class);
        when(mockAdmin.getLogin()).thenReturn("admin");
        final Container container = Container.containerFromResolvedCommand(resolvedCommand, containerId, mockAdmin.getLogin())
                .toBuilder()
                .workflowId(workflowId)
                .build();
        final ContainerEntity toCreate = ContainerEntity.fromPojo(container);
        final ContainerEntity created = containerEntityService.save(toCreate, mockAdmin);
        assertThat(created.getId(), is(not(0L)));

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final ContainerEntity retrieved = containerEntityService.get(created.getId());
        assertThat(retrieved, is(created));
    }

    @Test
    @DirtiesContext
    public void testRetrieveServices() throws Exception {
        final long databaseId = 1;
        final long commandId = 10;
        final long wrapperId = 100;
        final String containerId = "rumpus";
        final String userId = "me";
        final String dockerImage = "whale";
        final String commandLine = "exit 0";

        final String nonFinalizedStatus = "Running";
        final String nonFinalizedId = "foo";
        final ContainerEntity serviceNonfinalized = ContainerEntity.fromPojo(Container.builder()
                .databaseId(databaseId)
                .commandId(commandId)
                .wrapperId(wrapperId)
                .containerId(containerId)
                .userId(userId)
                .dockerImage(dockerImage)
                .commandLine(commandLine)
                .serviceId(nonFinalizedId)
                .status(nonFinalizedStatus)
                .build());
        final ContainerEntity serviceFinalized = ContainerEntity.fromPojo(Container.builder()
                .databaseId(databaseId)
                .commandId(commandId)
                .wrapperId(wrapperId)
                .containerId(containerId)
                .userId(userId)
                .dockerImage(dockerImage)
                .commandLine(commandLine)
                .serviceId("123")
                .status("Complete")
                .build());
        final ContainerEntity container = ContainerEntity.fromPojo(Container.builder()
                .databaseId(databaseId)
                .commandId(commandId)
                .wrapperId(wrapperId)
                .containerId(containerId)
                .userId(userId)
                .dockerImage(dockerImage)
                .commandLine(commandLine)
                .build());

        final ContainerEntity serviceNonfinalizedCreated = containerEntityService.create(serviceNonfinalized);
        final ContainerEntity serviceFinalizedCreated = containerEntityService.create(serviceFinalized);
        containerEntityService.create(container);

        final List<ContainerEntity> services = containerEntityService.retrieveServices();
        assertThat(services, hasSize(2));
        assertThat(services, hasItems(serviceFinalizedCreated, serviceNonfinalizedCreated));

        final List<ContainerEntity> nonfinalizedServices = containerEntityService.retrieveNonfinalizedServices();
        assertThat(nonfinalizedServices, hasSize(1));
        assertThat(nonfinalizedServices, hasItem(serviceNonfinalizedCreated));
    }
}
