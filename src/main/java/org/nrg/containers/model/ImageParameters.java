package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ImageParameters {
    public ImageParameters() {}

    public ImageParameters(final List<String> volumes, final String commandStr, final List<String> commandList) {
        setVolumes(volumes);
        setCommandStr(commandStr);
        setCommandList(commandList);
    }

    @JsonProperty("volumes")
    public List<String> getVolumes() {
        return _volumes;
    }

    public String[] getVolumesArray() {
        return _volumes.toArray(new String[_volumes.size()]);
    }

    public void setVolumes(final List<String> volumes) {
        _volumes = volumes;
    }

    @JsonProperty("command_string")
    public String getCommandStr() {
        return _commandStr;
    }

    public void setCommandStr(final String commandStr) {
        _commandStr = commandStr;
    }

    @JsonProperty("command_list")
    public List<String> getCommandList() {
        return _commandList;
    }

    public String[] getCommandArray() {
        return _commandList != null ?
                _commandList.toArray(new String[_commandList.size()]) :
                new String[] {_commandStr};
    }

    public void setCommandList(final List<String> commandList) {
        _commandList = commandList;
    }

    private List<String> _volumes;
    private String _commandStr;
    private List<String> _commandList;
}
