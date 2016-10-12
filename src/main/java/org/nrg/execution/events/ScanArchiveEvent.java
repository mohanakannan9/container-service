package org.nrg.execution.events;

import org.nrg.framework.event.EventI;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xft.security.UserI;

public class ScanArchiveEvent implements EventI {

    private static final String eventId = "ScanArchived";
    private XnatImagescandata scan;
    private String sessionId;
    private String rootArchivePath;
    private UserI user;

    public ScanArchiveEvent(final XnatImagescandata scan, final XnatImagesessiondata session, final UserI user, final String rootArchivePath) {
        this.scan = scan;
        this.sessionId = session.getId();
        this.user = user;
        this.rootArchivePath = rootArchivePath;
    }

    public static String getEventId() {
        return eventId;
    }
    public XnatImagescandata getScan() {
        return scan;
    }

    public String getSessionId() {
        return sessionId;
    }

    public UserI getUser() {
        return user;
    }

    public String getRootArchivePath() {
        return rootArchivePath;
    }
}