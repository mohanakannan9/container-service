package org.nrg.actions.model.tree;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;

@Converter(autoApply = true)
public class MatchTreeNodeConverter implements AttributeConverter<MatchTreeNode, String> {
    private final static Logger log = LoggerFactory.getLogger(MatchTreeNodeConverter.class);
    private final static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(final MatchTreeNode matchTreeNode) {
        try {
            return objectMapper.writeValueAsString(matchTreeNode);
        } catch (JsonProcessingException e) {
            log.error("Error writing MatchTreeNode as json: " + matchTreeNode, e);
        }
        return null;
    }

    @Override
    public MatchTreeNode convertToEntityAttribute(final String dbData) {
        try {
            return objectMapper.readValue(dbData, MatchTreeNode.class);
        } catch (IOException e) {
            log.error("Unexpected IOEx decoding json from database: " + dbData, e);
        }
        return null;
    }
}
