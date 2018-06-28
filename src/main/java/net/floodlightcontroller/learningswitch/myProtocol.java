package net.floodlightcontroller.learningswitch;

import java.util.ArrayList;

public class myProtocol extends SaraProtocol {
    private ArrayList<Long> switchesID = new ArrayList<Long>();

    @Override
    public boolean haveEntryFor (long id) {
        if(switchesID.contains(id))
            return true;
        return false;
    }

    @Override
    public void makeEntryFor(long currentSwitchId) {
        switchesID.add(currentSwitchId);
    }
}
