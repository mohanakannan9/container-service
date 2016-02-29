package org.nrg.containers.model;

import com.google.common.collect.Lists;

import java.util.List;

public class ContainerMocks {
    public final static String FOO_ID = "foo";
    public final static String FOO_STATUS = "Great";
    public final static Container FOO =
            new Container(FOO_ID, FOO_STATUS);

    public final static String FIRST_ID = "0";
    public final static String FIRST_STATUS = "first";
    public final static Container FIRST =
            new Container(FIRST_ID, FIRST_STATUS);

    public final static String SECOND_ID = "1";
    public final static String SECOND_STATUS = "second";
    public final static Container SECOND =
            new Container(SECOND_ID, SECOND_STATUS);

    public final static List<Container> FIRST_AND_SECOND = Lists.newArrayList(FIRST, SECOND);
}
