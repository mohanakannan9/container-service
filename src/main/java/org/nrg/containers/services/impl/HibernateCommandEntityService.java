package org.nrg.containers.services.impl;

import com.google.common.collect.Sets;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.hibernate.exception.ConstraintViolationException;
import org.nrg.containers.daos.CommandDao;
import org.nrg.containers.model.CommandEntity;
import org.nrg.containers.model.CommandWrapperEntity;
import org.nrg.containers.services.CommandEntityService;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.framework.exceptions.NrgRuntimeException;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class HibernateCommandEntityService extends AbstractHibernateEntityService<CommandEntity, CommandDao>
        implements CommandEntityService {
    private static final Logger log = LoggerFactory.getLogger(HibernateCommandEntityService.class);

    @Override
    public void afterPropertiesSet() {
        // Set the default JayWay JSONPath configuration
        Configuration.setDefaults(new Configuration.Defaults() {

            private final JsonProvider jsonProvider = new JacksonJsonProvider();
            private final MappingProvider mappingProvider = new JacksonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                return Sets.newHashSet(Option.DEFAULT_PATH_LEAF_TO_NULL);
            }
        });
    }

    @Override
    public List<CommandEntity> findByProperties(final Map<String, Object> properties) {
        return getDao().findByProperties(properties);
    }

    @Override
    public CommandEntity update(final long id, final CommandEntity updates, final Boolean ignoreNull) throws NotFoundException {
        // TODO
        return null;
    }

    @Override
    public CommandEntity create(final CommandEntity commandEntity) throws NrgRuntimeException {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Saving command " + commandEntity.getName());
            }
            if (commandEntity.getCommandWrapperEntities() != null) {
                for (final CommandWrapperEntity commandWrapperEntity : commandEntity.getCommandWrapperEntities()) {
                    commandWrapperEntity.setCommandEntity(commandEntity);
                }
            }
            return super.create(commandEntity);
        } catch (ConstraintViolationException e) {
            throw new NrgServiceRuntimeException("This command duplicates a command already in the database.", e);
        }
    }
}
