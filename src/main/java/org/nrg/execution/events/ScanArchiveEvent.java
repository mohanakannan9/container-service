package org.nrg.execution.events;

import org.nrg.framework.event.EventI;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xft.security.UserI;

public class ScanArchiveEvent implements EventI {

    private static final String eventId = "ScanArchived";
    private XnatImagescandata scan;
    private UserI user;

    public ScanArchiveEvent(XnatImagescandata scan, UserI user) {
        this.scan = scan;
        this.user = user;
    }

    public static String getEventId() {
        return eventId;
    }
    public XnatImagescandata getScan() {
        return scan;
    }
    public UserI getUser() {
        return user;
    }
}