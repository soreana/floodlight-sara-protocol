package net.floodlightcontroller.learningswitch;

import net.floodlightcontroller.core.IOFSwitch;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.types.OFPort;
import org.python.antlr.op.In;

import java.util.*;

public class SaraProtocol {
    private Map<InEntry, OutEntry> learned = new HashMap<>();
    private Map<Long, Set<OutEntry>> waitingRoom = new HashMap<>();
    private Map<Long, Set<OutEntry>> switchPorts = new HashMap<>();
    private Map<Long, Set<OutEntry>> hostPorts = new HashMap<>();
    private Map<Long, IOFSwitch> switches = new HashMap<>();

    public void printLearned(){
         System.out.println("Learned: ");
        for(InEntry learned: this.learned.keySet()){
            System.out.print(learned.getSw());
        }
    }


    private long currentSwitch;

    public boolean haveEntryFor(long id) {
        return waitingRoom.containsKey(id);
    }

    public void setCurrentSwitch(long currentSwitch) {
        this.currentSwitch = currentSwitch;
    }

    public void makeEntryFor(long currentSwitchId, IOFSwitch iofSwitch) {
        this.waitingRoom.put(currentSwitchId,new HashSet<>());
        this.hostPorts.put(currentSwitchId, new HashSet<>());
        this.switches.put(currentSwitchId, iofSwitch);
    }

    public void setPorts(long switchId, ArrayList<OFPortDesc> ports){
        Set<OutEntry> setPorts = new HashSet<>();
        for (OFPortDesc p : ports){
            setPorts.add(new OutEntry(switchId, p.getPortNo(), 3));
        }
        this.switchPorts.put(switchId, setPorts);
    }

    public boolean isComplete(long switchId){
        return this.waitingRoom.containsKey(switchId) && this.waitingRoom.get(switchId).size() == 0;
    }

    public boolean needComplete(long switchId){
        return this.waitingRoom.containsKey(switchId) && this.waitingRoom.get(switchId).size() != 0;
    }

    public boolean learned(Long sw, OFPortDesc p)
    {
//        return learned.containsKey(new InEntry(sw, p.getPortNo(), 3));
        return myContains(learned.keySet(), sw, p.getPortNo());
    }

    private Set<IOFSwitch> markSwitches;
    private ArrayList<OFPortDesc> path;

    public void setHostPorts(Long switchId){
        for (OutEntry outEntry : switchPorts.get(switchId))
        {
//            if (!this.waitingRoom.get(switchId).contains(outEntry)){
            if (!myContains2(waitingRoom.get(switchId), outEntry.getSw(), outEntry.getPort())){
                this.hostPorts.get(switchId).add(outEntry);
            }
        }
    }

    public OFPortDesc findDfsPort(IOFSwitch currentSwitch) {
        markSwitches = new HashSet<>();
        path = new ArrayList<>();
        IOFSwitch iofSwitch = dfs(currentSwitch);
        if (iofSwitch != null)
            return path.get(0);
        throw new RuntimeException("path not found");
    }

    private IOFSwitch dfs(IOFSwitch iofSwitch){
        markSwitches.add(iofSwitch);
        if (needComplete(iofSwitch.getId().getLong()))
            return iofSwitch;
        for (OFPortDesc ofPortDesc : iofSwitch.getPorts()){
//            if (markSwitches.contains(switches.get(learned.get(new InEntry(iofSwitch.getId().getLong(), ofPortDesc.getPortNo(), 3)).getSw())))
            if (markSwitches.contains(switches.get(myGet1(learned.keySet(), iofSwitch.getId().getLong(), ofPortDesc.getPortNo()).getSw())))
                continue;
            path.add(ofPortDesc);
//            IOFSwitch found = dfs(switches.get(learned.get(new InEntry(iofSwitch.getId().getLong(), ofPortDesc.getPortNo(), 3)).getSw()));
            IOFSwitch found = dfs(switches.get(myGet1(learned.keySet(), iofSwitch.getId().getLong(), ofPortDesc.getPortNo()).getSw()));
            if (found != null)
                return found;
            path.remove(ofPortDesc);
        }
        return null;
    }

    public boolean isFinished()
    {
        if (waitingRoom.size() != 0)
        {
            for (Long id : waitingRoom.keySet()){
                if (waitingRoom.get(id).size() > 0)
                    return false;
            }
            return true;
        }
        return false;
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
//        if (learned.containsKey(new InEntry(sw, inPort,value)))
        if (myContains(learned.keySet(), sw, inPort))
            return;
        else if (waitingRoom.containsKey(sw)) {
            for( OutEntry current : waitingRoom.get(sw)){
                if(current.getSw() == currentSwitch){
                    learned.put(new InEntry(sw,inPort,value),current);
                    learned.put(new InEntry(current,value),new OutEntry(sw,inPort,value));
                    waitingRoom.get(sw).remove(current);
                }
            }
            waitingRoom.get(sw).add(new OutEntry(sw, inPort,value));
        }else {
            if (waitingRoom.get(sw) == null) {
                Set<OutEntry> temp = new HashSet<>();
                temp.add(new OutEntry(sw, inPort, value));
                waitingRoom.put(currentSwitch, temp);
            }else {
                waitingRoom.get(sw).add(new OutEntry(sw, inPort, value));
            }
        }
    }

    private boolean myContains(Set<InEntry> entries, long sw, OFPort inPort)
    {
        for (InEntry entry: entries){
            if (sw == entry.getSw() && inPort.equals(entry.getPort()))
                return true;
        }
        return false;
    }

    private boolean myContains2(Set<OutEntry> entries, long sw, OFPort inPort)
    {
        for (OutEntry entry: entries){
            if (sw == entry.getSw() && inPort.equals(entry.getPort()))
                return true;
        }
        return false;
    }

    private InEntry myGet1(Set<InEntry> entries, long sw, OFPort inPort){
        for (InEntry entry: entries){
            if (sw == entry.getSw() && inPort.equals(entry.getPort()))
                return entry;
        }
        return null;
    }

    private OutEntry myGet2(Set<OutEntry> entries, long sw, OFPort inPort){
        for (OutEntry entry: entries){
            if (sw == entry.getSw() && inPort.equals(entry.getPort()))
                return entry;
        }
        return null;
    }

}
