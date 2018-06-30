package net.floodlightcontroller.learningswitch;

import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class SaraProtocol {
    private Map<InEntry, OutEntry> learned = new HashMap<>();
    private Map<Long, Set<OutEntry>> waitingRoom = new HashMap<>();
    protected Logger log = LoggerFactory.getLogger(LearningSwitch.class);

    private long currentSwitch;
    private long prevSwitch;

    public long getPrevSwitch() {
        return prevSwitch;
    }

    public Map<InEntry, OutEntry> getLearned() {
        return learned;
    }

    public boolean haveEntryFor(long id) {
        return waitingRoom.containsKey(id);
    }

    public Map<Long, Set<OutEntry>> getWaitingRoom() {
        return waitingRoom;
    }

    public void setCurrentSwitch(long currentSwitch) {
        // todo save ports of ex switch
        prevSwitch=this.currentSwitch;
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

    private class InEntry extends Entry {
        InEntry(long sw, OFPort port, long value) {
            super(sw, port, value);
        }

        InEntry(OutEntry outEntry, long value) {
            super(outEntry.getSw(),outEntry.getPort(), value);
        }
    }

    private class OutEntry extends Entry {
        OutEntry(long sw, OFPort port, long value) {
            super(sw, port, value);
        }
    }


    public void learnLinkForCurrentSwitch(long sw, OFPort inPort, long value) {
        if (learned.containsKey(new InEntry(sw, inPort,value)))
            return;
        else if (waitingRoom.containsKey(sw)) {
//            log.info("here............");
            for( OutEntry current : waitingRoom.get(sw)){
//                log.info("here...he{}", current);
                if(current.getSw() == currentSwitch && sw == prevSwitch){
//                    log.info("here...here....");
                    learned.put(new InEntry(sw,inPort,value),current);
                    learned.put(new InEntry(current,value),new OutEntry(sw,inPort,value));
                }
            }
            waitingRoom.get(sw).add(new OutEntry(sw, inPort,value));
        }else {
            Set<OutEntry> temp = new HashSet<>();
            temp.add(new OutEntry(sw, inPort, value));
            waitingRoom.put(currentSwitch, temp);
        }
    }

}
