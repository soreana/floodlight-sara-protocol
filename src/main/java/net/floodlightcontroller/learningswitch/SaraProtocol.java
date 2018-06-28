package net.floodlightcontroller.learningswitch;

import org.projectfloodlight.openflow.types.OFPort;
import java.util.*;

public class SaraProtocol {
    private Map<InEntry, OutEntry> learned = new HashMap<>();
    private Map<Long, Set<OutEntry>> waitingRoom = new HashMap<>();
    private HashSet<Long> mark = new HashSet<>();
    private Map<Long, Long> s = new HashMap<>();
    private Map<InEntry, OutEntry> mst= new HashMap<>();
    private Map<Long, Entry> parent = new HashMap<>();

    private long currentSwitch;
    private long currentSwitchStartTime;

    public boolean haveEntryFor(long id) {
        return false;
    }

    public long getCurrentSwitchStartTime() {
        return currentSwitchStartTime;
    }

    public void setCurrentSwitch(long currentSwitch, long currentSwitchStartTime) {
        this.currentSwitch = currentSwitch;
        this.currentSwitchStartTime = currentSwitchStartTime;
    }

    public void makeEntryFor(long currentSwitchId) {
        waitingRoom.put(currentSwitchId,new HashSet<>());
        mark.add(currentSwitchId);
    }

    public void addToQueue(long sw, OFPort inPort, long duration) {
        if (mark.contains(sw))
            return;
        if (s.containsKey(sw)) {
            long t = s.get(sw);
            if (duration < t) {
                s.put(sw, duration);
                parent.put(sw, new Entry(currentSwitch, inPort, duration));
            }
        } else {
            s.put(sw, duration);
            parent.put(sw, new Entry(currentSwitch, inPort, duration));
        }
    }

    public Long extractMin() {
        if (s.isEmpty()) {
            throw new NoSuchElementException();
        }
        Map.Entry<Long, Long> mn = null;
        for (Map.Entry<Long, Long> entry: s.entrySet()) {
            if (mn == null || mn.getValue() > entry.getValue()) {
                mn = entry;
            }
        }
        s.remove(mn.getKey());
        return mn.getKey();
    }

    public void printLearned() {
        System.out.println("Sara Current Switch: " + currentSwitch);
        System.out.print("Learned: ");
        for (InEntry curr : learned.keySet()) {
            System.out.print(curr.getSw() + " - " + curr.getPort() + " || ");
            System.out.println(learned.get(curr).getSw() + " - " + learned.get(curr).getPort());
        }
        System.out.println();
        System.out.print("WaitingRoom: ");
        for (Long curr : waitingRoom.keySet()) {
            System.out.print(curr + " || ");
            for (OutEntry c : waitingRoom.get(curr)) {
                System.out.print(c.getSw() + " - " + c.getPort() + " || ");
            }
            System.out.println();
        }
        System.out.println();
        System.out.print("MST: ");
        for (InEntry curr : mst.keySet()) {
            System.out.print(curr.getSw() + " - " + curr.getPort() + " || ");
            System.out.println(mst.get(curr).getSw() + " - " + mst.get(curr).getPort());
        }
        System.out.println();
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
        System.out.print("Switch in learnLinkForCurrentSwitch: ");
        System.out.println(sw);
        if (learned.containsKey(new InEntry(sw, inPort,value)))
            return ;

        if (waitingRoom.containsKey(sw)) {
            for (OutEntry current : waitingRoom.get(sw)) {
                if (current.getSw() == currentSwitch) {
                    learned.put(new InEntry(sw,inPort,value),current);
                    learned.put(new InEntry(current,value),new OutEntry(sw,inPort,value));
                    if (parent.containsKey(currentSwitch)) {
                        Entry e = parent.get(currentSwitch);
                        if (e.getSw() == sw) {
                            mst.put(new InEntry(sw,inPort,value),current);
                        }
                    }
                    return;
                }
            }
            waitingRoom.get(currentSwitch).add(new OutEntry(sw, inPort,value));
        } else {
            waitingRoom.get(currentSwitch).add(new OutEntry(sw, inPort,value));
        }
    }

}
