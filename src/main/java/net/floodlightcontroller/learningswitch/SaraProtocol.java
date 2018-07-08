package net.floodlightcontroller.learningswitch;

import net.floodlightcontroller.core.IOFSwitch;
import org.apache.commons.lang.ObjectUtils;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.types.OFPort;

import java.sql.Timestamp;
import java.util.*;

public class SaraProtocol {
    private Map<InEntry, OutEntry> learned = new HashMap<>();
    protected Map<Long, Set<OutEntry>> waitingRoom = new HashMap<>();

    ///////////////////////////////////////////////////////////////////////////
    public Queue<Long> BFSQueue = new LinkedList<>();
    public Long nextSwitch;
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    public long broadcastTime;

    public void setBroadcastTime() {
        this.broadcastTime = System.currentTimeMillis();
    }

    public long delay() {
        return this.broadcastTime - System.currentTimeMillis();
    }
    ///////////////////////////////////////////////////////////////////////////

    protected long currentSwitch;

    public boolean haveEntryFor(long id) {
        return waitingRoom.containsKey(id);
    }

    public void setCurrentSwitch(long currentSwitch) {
        // todo save ports of ex switch
        this.nextSwitch = BFSQueue.poll(); //TODO: here is the right place?
        this.currentSwitch = currentSwitch;
    }

    public void makeEntryFor(long currentSwitchId) {
        waitingRoom.put(currentSwitchId,new HashSet<>());
        // todo
    }

    ///////////////////////////////////////////////////////////////////////////
    public OFPort findNextPort(long currentSwitch, long destinationSwitch) { //DFS
        List<Entry> switchEntries = getInEntry(currentSwitch);
        for (Entry entry: switchEntries){
            if (learned.get(entry).getSw() == destinationSwitch)
                return entry.port;
            OFPort subResult = findNextPort(learned.get(entry).getSw(), destinationSwitch);
            if (subResult != null)
                return learned.get(entry).getPort();
        }
        return null;
    }
    public List<Entry> getInEntry(long switchId) {
        List<Entry> result = new LinkedList<>();
        for (Entry e: learned.keySet())
            if (e.sw == switchId)
                result.add(e);
        return result;
    }

    public void addHosts(IOFSwitch sw) {
        List<Entry> registeredPorts = getInEntry(sw.getId().getLong());
        for (OFPortDesc p : sw.getPorts()) {
            if(findInEntryPort(registeredPorts, p) != null) continue;
            else addHostOnPort(p, sw);
        }
    }

    private Entry findInEntryPort(List<Entry> list, OFPortDesc p) {
        for (Entry e: list)
            if (e.port.equals(p))
                return e;
        return null;
    }

    private void addHostOnPort(OFPortDesc p, IOFSwitch sw) {
        learned.put(new InEntry(sw.getId().getLong(), p.getPortNo(), Long.MAX_VALUE), new Host(p.getPortNo()));
    }
    ///////////////////////////////////////////////////////////////////////////

    private class Entry {
        protected long sw;
        protected OFPort port;
        protected long value;

        public Entry() {}

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
        public OutEntry() {}
        OutEntry(long sw, OFPort port, long value) {
            super(sw, port, value);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    private class Host extends OutEntry {
        public Host(OFPort port) {
            this.port = port;
        }
    }
    ///////////////////////////////////////////////////////////////////////////

    public void learnLinkForCurrentSwitch(long sw, OFPort inPort, long value) {
        if (learned.containsKey(new InEntry(sw, inPort,value)))
            return;
        else if (waitingRoom.containsKey(sw)) {
            for( OutEntry current : waitingRoom.get(sw)){
                if(current.getSw() == currentSwitch){
                    ///////////////////////////////////////////////////////////////////////////
                    BFSQueue.add(current.getSw());
                    ///////////////////////////////////////////////////////////////////////////
                    learned.put(new InEntry(sw,inPort,value),current);
                    learned.put(new InEntry(current,value),new OutEntry(sw,inPort,value));
                }
            }
            waitingRoom.get(sw).add(new OutEntry(sw, inPort,value));
        }else {
            if (waitingRoom.containsKey(currentSwitch))
                waitingRoom.get(currentSwitch).add(new OutEntry(sw, inPort,value));
            else {
                Set<OutEntry> temp = new HashSet<>();
                temp.add(new OutEntry(sw, inPort,value));
                waitingRoom.put(currentSwitch,temp);
            }
        }
    }

}
