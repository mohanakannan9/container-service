package org.nrg.containers.model.container.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.hibernate.envers.Audited;
import org.nrg.containers.model.command.auto.ResolvedCommand.ResolvedCommandMountFiles;
import org.nrg.containers.model.container.auto.Container;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.Objects;

import static org.nrg.containers.model.command.auto.Command.CommandWrapperInput;

@Entity
@Audited
public class ContainerMountFilesEntity {
    private long id;
    @JsonIgnore private ContainerEntityMount containerEntityMount;
    @JsonProperty("from-xnat-input") private String fromXnatInput;
    @JsonProperty("from-uri") private String fromUri;
    @JsonProperty("root-directory") private String rootDirectory;
    private String path;

    public ContainerMountFilesEntity() {}

    public ContainerMountFilesEntity(final ResolvedCommandMountFiles resolvedCommandMountFiles) {
        this.fromXnatInput = resolvedCommandMountFiles.fromXnatInput();
        this.fromUri = resolvedCommandMountFiles.fromUri();
        this.rootDirectory = resolvedCommandMountFiles.rootDirectory();
        this.path = resolvedCommandMountFiles.path();
    }

    public static ContainerMountFilesEntity create(final Container.ContainerMountFiles containerMountFilesPojo) {
        final ContainerMountFilesEntity containerMountFilesEntity = new ContainerMountFilesEntity();
        containerMountFilesEntity.setId(containerMountFilesPojo.databaseId());
        containerMountFilesEntity.setFromXnatInput(containerMountFilesPojo.fromXnatInput());
        containerMountFilesEntity.setFromUri(containerMountFilesPojo.fromUri());
        containerMountFilesEntity.setRootDirectory(containerMountFilesPojo.rootDirectory());
        containerMountFilesEntity.setPath(containerMountFilesPojo.path());
        return containerMountFilesEntity;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    @ManyToOne
    public ContainerEntityMount getContainerEntityMount() {
        return containerEntityMount;
    }

    public void setContainerEntityMount(final ContainerEntityMount containerEntityMount) {
        this.containerEntityMount = containerEntityMount;
    }

    public String getFromXnatInput() {
        return fromXnatInput;
    }

    public void setFromXnatInput(final String fromXnatInput) {
        this.fromXnatInput = fromXnatInput;
    }

    public String getFromUri() {
        return fromUri;
    }

    public void setFromUri(final String fromUri) {
        this.fromUri = fromUri;
    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    public void setRootDirectory(final String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ContainerMountFilesEntity that = (ContainerMountFilesEntity) o;
        return Objects.equals(this.containerEntityMount, that.containerEntityMount) &&
                Objects.equals(this.fromXnatInput, that.fromXnatInput) &&
                Objects.equals(this.fromUri, that.fromUri) &&
                Objects.equals(this.rootDirectory, that.rootDirectory) &&
                Objects.equals(this.path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(containerEntityMount, fromXnatInput, fromUri, rootDirectory, path);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("fromXnatInput", fromXnatInput)
                .add("fromUri", fromUri)
                .add("rootDirectory", rootDirectory)
                .add("path", path)
                .toString();
    }
}
