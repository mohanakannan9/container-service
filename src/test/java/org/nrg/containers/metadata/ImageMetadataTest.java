package org.nrg.containers.metadata;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.containers.config.ImageMetadataTestConfig;
import org.nrg.containers.metadata.service.ImageMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@RunWith(SpringJUnit4ClassRunner.class)
@Rollback
@Transactional
@ContextConfiguration(classes = ImageMetadataTestConfig.class)
public class ImageMetadataTest {
    @Autowired
    private ImageMetadataService metadataService;

    @Test
    public void testSpringConfiguration() {
        assertThat(metadataService, not(nullValue()));
    }
}
