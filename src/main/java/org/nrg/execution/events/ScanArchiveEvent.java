package org.nrg.execution.events;

import org.nrg.framework.event.EventI;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xft.security.UserI;

public class ScanArchiveEvent implements EventI {

    private static final String eventId = "ScanArchived";
    private XnatImagescandata scan;
    private XnatImagesessiondata session;
    private UserI user;

    public ScanArchiveEvent(XnatImagescandata scan, XnatImagesessiondata session, UserI user) {
        this.scan = scan;
        this.session = session;
        this.user = user;
    }

    public static String getEventId() {
        return eventId;
    }
    public XnatImagescandata getScan() {
        return scan;
    }

    public XnatImagesessiondata getSession() {
        return session;
    }

    public UserI getUser() {
        return user;
    }
}