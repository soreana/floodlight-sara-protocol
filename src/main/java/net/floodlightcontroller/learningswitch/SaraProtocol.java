package net.floodlightcontroller.learningswitch;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.*;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class SaraProtocol {
    private byte[] routingPacketPayload = new byte[20];
    private long TIMEOUT = 2000;
    private Date broadcastTime;
    private Map<InEntry, OutEntry> learned = new HashMap<>();
    private Map<Long, Set<OutEntry>> waitingRoom = new HashMap<>();
    private Map<Long, Set<OFPort>> HostLink = new HashMap<>();
    private Prim prim;
    private long currentSwitch;
    private boolean initialized = false;
    private Timer timer = new Timer();
    private SaraProtocolUtils.SaraProtocolState state = SaraProtocolUtils.SaraProtocolState.BROADCAST;
    protected static Logger logger = LoggerFactory.getLogger(SaraProtocol.class);

    public void printEdges() {
        for (InEntry ie: learned.keySet()) {
            OutEntry oe = learned.get(ie);
            logger.info(ie.toString() + " & " + oe.toString());
        }
    }

    public SaraProtocolUtils.SaraProtocolState getState() {
        return state;
    }

    private void addHostLink(Long sw, OFPort p) {
        if (!HostLink.containsKey(sw))
            HostLink.put(sw, new HashSet<>());

        HostLink.get(sw).add(p);
    }

    public boolean isHostLink(Long sw, OFPort p) {
        if (HostLink.containsKey(sw))
            return HostLink.get(sw).contains(p);

        return false;
    }

    private void onTimeout() {
        state = SaraProtocolUtils.SaraProtocolState.BROADCAST;

        logger.info("Timeout reached!");
    }

    private void setTimer() {
        stopTimer();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                onTimeout();
            }
        }, TIMEOUT);
    }

    private void stopTimer() {
        timer.cancel();
    }

    public void pushDefaultPacket(IOFSwitch sw, OFPacketIn pi) {
        OFPort outport = OFPort.FLOOD;
        if (pi == null) {
            return;
        }

        OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();

        // set actions
        List<OFAction> actions = new ArrayList<OFAction>();
        actions.add(sw.getOFFactory().actions().buildOutput().setPort(outport).setMaxLen(0xffFFffFF).build());

        pob.setActions(actions);

        // If the switch doens't support buffering set the buffer id to be none
        // otherwise it'll be the the buffer id of the PacketIn
        if (sw.getBuffers() == 0) {
            // We set the PI buffer id here so we don't have to check again below
            pi = pi.createBuilder().setBufferId(OFBufferId.NO_BUFFER).build();
            pob.setBufferId(OFBufferId.NO_BUFFER);
        } else {
            pob.setBufferId(pi.getBufferId());
        }

        pob.setInPort(OFPort.CONTROLLER);

        // If the buffer id is none or the switch doesn's support buffering
        // we send the data with the packet out
        if (pi.getBufferId() == OFBufferId.NO_BUFFER) {
            byte[] packetData = pi.getData();
            pob.setData(packetData);
        }

        sw.write(pob.build());
    }

    public long getLatency(Date receivedTime) {
        return receivedTime.getTime() - broadcastTime.getTime();
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void initialize(IOFSwitch sw, OFPort inPort) {
        logger.info("Initialized");
        addHostLink(sw.getId().getLong(), inPort);
        prim = new Prim(sw);
        initialized = true;
    }

    public void broadcastNextSwitch(IOFSwitch reserved_sw, OFPacketIn pi) {
        if (prim.isDone()) {
            printEdges();

            return;
        }
        IOFSwitch sw = prim.next();
        currentSwitch = sw.getId().getLong();
        waitingRoom.put(currentSwitch, new HashSet<>());
        logger.info("Broadcasting on: " + String.valueOf(currentSwitch));

        state = SaraProtocolUtils.SaraProtocolState.GET_RESPOND;
        state.stayInGetRespondFor(sw.getPorts().size());

        broadcastTime = new Date();
        pushDefaultPacket(sw, pi);
        setTimer();
    }

    public void handleRespond(IOFSwitch sw, OFPort inPort) {
        long latency = getLatency(new Date());

        if (sw.getId().getLong() == currentSwitch)
            return ;
        learnLinkForCurrentSwitch(sw.getId().getLong(), inPort, latency);
        prim.addEdge(latency, sw);
        logger.info("Handle respond: " + String.valueOf(sw.getId().getLong()) + " latency: " + latency);
        state = state.nextState();
        if (state == SaraProtocolUtils.SaraProtocolState.BROADCAST)
            stopTimer();
    }

    private long getConnectedSwitch(long sw, OFPort p) {
        for (InEntry ie: learned.keySet())
            if (ie.getSw() == sw && ie.getPort() == p)
                return learned.get(ie).getSw();

        return -1;
    }

    public boolean isGoodPort(long sw, OFPort p) {
        long dest = getConnectedSwitch(sw, p);

        return (!prim.isDone()) || (dest == -1) || prim.hasEdge(sw, dest);
    }

    public boolean isDone() {
        return prim.isDone();
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

        public String toString() {
            return "sw: " + String.valueOf(sw) + " port: " + port.toString() + " value: " + String.valueOf(value);
        }
    }

    private class InEntry extends Entry {
        InEntry(long sw, OFPort port, long value) {
            super(sw, port, value);
        }

        InEntry(OutEntry outEntry, long value) {
            super(outEntry.getSw(), outEntry.getPort(), value);
        }
    }

    private class OutEntry extends Entry {
        OutEntry(long sw, OFPort port, long value) {
            super(sw, port, value);
        }
    }

    public void learnLinkForCurrentSwitch(long sw, OFPort inPort, long latency) {
        if (learned.containsKey(new InEntry(sw, inPort, latency)))
            return;
        else if (waitingRoom.containsKey(sw)) {
            for (OutEntry current : waitingRoom.get(sw)) {
                if (current.getSw() == currentSwitch) {
                    learned.put(new InEntry(sw, inPort, latency), current);
                    learned.put(new InEntry(current, latency), new OutEntry(sw, inPort, latency));
                }
            }
        }
        Set<OutEntry> temp = new HashSet<>();
        temp.add(new OutEntry(sw, inPort, latency));
        waitingRoom.put(currentSwitch, temp);
    }
}
