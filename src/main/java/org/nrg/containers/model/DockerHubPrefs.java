package org.nrg.containers.model;

import org.nrg.framework.exceptions.NrgServiceError;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.prefs.annotations.NrgPreference;
import org.nrg.prefs.annotations.NrgPreferenceBean;
import org.nrg.prefs.beans.AbstractPreferenceBean;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static org.nrg.containers.model.DockerHubPrefs.PREF_ID;

@NrgPreferenceBean(toolId = PREF_ID,
    toolName = "Docker Hubs Prefs",
    description = "Manages the preferences for the Docker Hubs",
    strict = false)
public class DockerHubPrefs extends AbstractPreferenceBean {
    private static final Logger _log = LoggerFactory.getLogger(DockerHubPrefs.class);
    public static final String PREF_ID = "dockerHubPrefs";

//    private static final String DEFAULT_HUB =
//        String.format("{'%s':{'url':'%s','username':'%s','password':'%s','email':'%s'}}",
//            DefaultHub.key(), DefaultHub.url(), DefaultHub.username(), DefaultHub.password(), DefaultHub.email());

    public boolean hasContainerHub(final String url) {
        for (final DockerHub instance : getDockerHubPrefs()) {
            if (instance.url().equals(url)) {
                return true;
            }
        }
        return false;
    }

    @NrgPreference(defaultValue = "[{'url':'https://index.docker.io/v1/', 'name':'Docker Hub'}]", key = "url")
    public List<DockerHub> getDockerHubPrefs() {
        return getListValue(PREF_ID);
    }

    public DockerHub getDockerHubPref(final String url) throws IOException {
        final String value = getValue(PREF_ID, url);
        return deserialize(value, DockerHub.class);
    }

    public void setDockerHub(final DockerHub instance) {
        final String instanceId = getPrefId(PREF_ID, instance);

        try {
            set(serialize(instance), PREF_ID, instanceId);
        } catch (InvalidPreferenceName invalidPreferenceName) {
            _log.info("Got an invalid preference name error setting Docker Hub " + instanceId);
            throw new NrgServiceRuntimeException(NrgServiceError.Unknown,
                "Could not set Docker Hub " + instance);
        } catch (IOException e) {
            throw new NrgServiceRuntimeException(NrgServiceError.UnknownEntity,
                    "Object does not look like a Docker Hub instance: " + instance);
        }
    }

    public void deleteDockerHub(final DockerHub instance) {
        final String instanceId = getPrefId(PREF_ID, instance);
        try {
            delete(PREF_ID, instanceId);
        } catch (InvalidPreferenceName invalidPreferenceName) {
            _log.info("Got an invalid preference name error trying to delete Docker Hub with id " + instanceId);
        }
    }

    public List<DockerHub> getDockerHubs() {
        return getDockerHubPrefs();
    }


    private String getPrefId(final String prefId, final DockerHub instance) {
        final String url = instance.url();
        return getNamespacedPropertyId(prefId, url);
    }
}
