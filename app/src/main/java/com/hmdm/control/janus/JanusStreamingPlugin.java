package com.hmdm.control.janus;

import com.hmdm.control.Const;
import com.hmdm.control.janus.json.JanusPollResponse;

public class JanusStreamingPlugin extends JanusPlugin {
    @Override
    public String getName() {
        return Const.JANUS_PLUGIN_STREAMING;
    }

    @Override
    public void onWebRtcUp() {
    }

    @Override
    public void onPollingEvent(JanusPollResponse event) {
    }

    @Override
    public int destroy() {
        // TODO
        return 0;
    }
}
