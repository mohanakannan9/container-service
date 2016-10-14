package org.nrg.execution.events;

import org.nrg.execution.model.xnat.Scan;
import org.nrg.framework.event.EventI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xft.security.UserI;

public class ScanArchiveEventToLaunchCommands implements EventI {
    private Scan scan;
    private UserI user;

    public ScanArchiveEventToLaunchCommands(final Scan scan, final UserI user) {
        this.scan = scan;
        this.user = user;
    }

    public Scan getScan() {
        return scan;
    }

    public UserI getUser() {
        return user;
    }
}