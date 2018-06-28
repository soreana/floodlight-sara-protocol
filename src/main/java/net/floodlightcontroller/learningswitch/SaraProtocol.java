package net.floodlightcontroller.learningswitch;

import org.projectfloodlight.openflow.types.OFPort;

import java.util.*;

public class SaraProtocol {
    protected Map<InEntry, OutEntry> learned = new HashMap<>();
    protected Map<Long, Set<OutEntry>> waitingRoom = new HashMap<>();

    protected long currentSwitch;

    public boolean haveEntryFor(long id) {
        // todo
        return false;
    }

    public void setCurrentSwitch(long currentSwitch) {
        // todo save ports of ex switch
        this.currentSwitch = currentSwitch;
    }

    public void makeEntryFor(long currentSwitchId) {
        waitingRoom.put(currentSwitchId,new HashSet<>());
        // todo
    }

    private class Entry {
        private long sw;
        private OFPort port;
        private long value;

        Entry(long sw, OFPort port, long value) {
            this.sw = sw;
            this.port = port;
            this.value = value;
        }

        public OFPort getPort() {
            return port;
        }

        public long getSw() {
            return sw;
        }

        public long getValue() {
            return value;
        }
    }

    protected class InEntry extends Entry {
        InEntry(long sw, OFPort port, long value) {
            super(sw, port, value);
        }

        InEntry(OutEntry outEntry, long value) {
            super(outEntry.getSw(),outEntry.getPort(), value);
        }
    }

    protected class OutEntry extends Entry {
        OutEntry(long sw, OFPort port, long value) {
            super(sw, port, value);
        }
    }

    public void learnLinkForCurrentSwitch(long sw, OFPort inPort, long value) {
        if (learned.containsKey(new InEntry(sw, inPort,value)))
            return;
        else if (waitingRoom.containsKey(sw)) {
            for( OutEntry current : waitingRoom.get(sw)){
                if(current.getSw() == currentSwitch){
                    learned.put(new InEntry(sw,inPort,value),current);
                    learned.put(new InEntry(current,value),new OutEntry(sw,inPort,value));
                }
            }
            waitingRoom.get(sw).add(new OutEntry(sw, inPort,value));
        }else {
            Set<OutEntry> temp = new HashSet<>();
            temp.add(new OutEntry(sw, inPort,value));
            waitingRoom.put(currentSwitch,temp);
        }
    }

}
