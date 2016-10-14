package org.nrg.containers.events;

import org.nrg.framework.event.EventI;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xft.security.UserI;

public class SessionArchiveEvent implements EventI {
    private XnatImagesessiondata session;
    private UserI user;

    public SessionArchiveEvent(final XnatImagesessiondata session, final UserI user) {
        this.session = session;
        this.user = user;
    }

    public XnatImagesessiondata getSession() {

        return session;
    }
    public UserI getUser() {
        return user;
    }
}