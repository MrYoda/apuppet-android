package com.hmdm.control.janus.json;

public class JanusAttachRequest extends JanusRequest {
    private String plugin;

    public JanusAttachRequest() {
    }

    public JanusAttachRequest(String pluginName) {
        super("attach", true);
        this.plugin = pluginName;
    }

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }
}
