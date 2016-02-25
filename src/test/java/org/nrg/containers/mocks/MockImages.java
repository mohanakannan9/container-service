package org.nrg.containers.mocks;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.nrg.containers.model.Image;

import java.util.List;
import java.util.Map;

public class MockImages {
    public final static String FOO_NAME = "foo";
    public final static String FOO_ID = "0";
    public final static Long FOO_SIZE = 0L;
    public final static List<String> FOO_TAGS = Lists.newArrayList("tag1", "tag2");
    public final static Map<String, String> FOO_LABELS = ImmutableMap.of("label0", "value0");
    public final static Image FOO =
            new Image(FOO_NAME, FOO_ID, FOO_SIZE, FOO_TAGS, FOO_LABELS);

    public final static String FIRST_NAME = "first";
    public final static String FIRST_ID = "0";
    public final static Long FIRST_SIZE = 0L;
    public final static List<String> FIRST_TAGS = Lists.newArrayList("tag1", "tag2");
    public final static Map<String, String> FIRST_LABELS = ImmutableMap.of("label0", "value0");
    public final static Image FIRST =
            new Image(FIRST_NAME, FIRST_ID, FIRST_SIZE, FIRST_TAGS, FIRST_LABELS);

    public final static String SECOND_NAME = "second";
    public final static String SECOND_ID = "0";
    public final static Long SECOND_SIZE = 0L;
    public final static List<String> SECOND_TAGS = Lists.newArrayList("tagX", "tagY");
    public final static Map<String, String> SECOND_LABELS = ImmutableMap.of("label1", "value1");
    public final static Image SECOND =
            new Image(SECOND_NAME, SECOND_ID, SECOND_SIZE, SECOND_TAGS, SECOND_LABELS);

    public final static List<Image> FIRST_AND_SECOND = Lists.newArrayList(FIRST, SECOND);
}
