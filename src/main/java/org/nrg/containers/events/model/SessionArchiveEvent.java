package org.nrg.containers.events.model;

import com.google.auto.value.AutoValue;
import org.nrg.framework.event.EventI;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xft.security.UserI;

@AutoValue
public abstract class SessionArchiveEvent implements EventI {
    public abstract XnatImagesessiondata session();
    public abstract String project();
    public abstract UserI user();

    public static SessionArchiveEvent create(final XnatImagesessiondata session,
                                             final String project,
                                             final UserI user) {
        return new AutoValue_SessionArchiveEvent(session, project, user);
    }

    public static SessionArchiveEvent create(final XnatImagesessiondata session,
                                             final UserI userI) {
        return create(session, session.getProject(), userI);
    }
}