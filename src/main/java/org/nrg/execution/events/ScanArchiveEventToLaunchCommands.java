package org.nrg.execution.events;

import org.nrg.execution.model.xnat.Scan;
import org.nrg.framework.event.EventI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xft.security.UserI;

public class ScanArchiveEventToLaunchCommands implements EventI {
    private Scan scan;
    private String sessionId;
    private UserI user;

    public ScanArchiveEventToLaunchCommands(final Scan scan, final String sessionId, final UserI user) {
        this.scan = scan;
        this.sessionId = sessionId;
        this.user = user;
    }

    public Scan getScan() {
        return scan;
    }

    public String getSessionId() {
        return sessionId;
    }

    public UserI getUser() {
        return user;
    }
}