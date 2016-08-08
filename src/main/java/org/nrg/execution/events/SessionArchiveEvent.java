package org.nrg.execution.events;

import org.nrg.framework.event.EventI;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xft.security.UserI;

public class SessionArchiveEvent implements EventI {

    private static final String eventId = "SessionArchived";
    private XnatImagesessiondata session;
    private UserI user;

    public SessionArchiveEvent(XnatImagesessiondata session, UserI user) {
        this.session = session;
        this.user = user;
    }

    public static String getEventId() {
        return eventId;
    }
    public XnatImagesessiondata getSession() {

        return session;
    }
    public UserI getUser() {
        return user;
    }
}