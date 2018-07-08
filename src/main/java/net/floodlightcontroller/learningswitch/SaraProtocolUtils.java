package net.floodlightcontroller.learningswitch;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IpProtocol;

import java.sql.Time;
import java.util.Timer;

public interface SaraProtocolUtils {

    /**
     * use this method to see if you should care about incoming packet
     *
     * @param cntx used to extract packet info
     * @return true if packet contain ARP or ICMP header
     */

    static boolean ICareAbout(FloodlightContext cntx) {
        Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        if (eth.getEtherType() == EthType.IPv4) {
            IPv4 ip = (IPv4) eth.getPayload();
            return ip.getProtocol() == IpProtocol.ICMP;
        } else return eth.getEtherType() == EthType.ARP;
    }

    long TIME_OUT = 10;

    enum SaraProtocolState {
        BROADCAST,
        GET_RESPOND;
        private int stayInGetRespond = 0;

        private class TimeOutHandler implements Runnable {

            private SaraProtocolState saraProtocolStates;
            private LearningSwitch learningSwitch;
            private IOFSwitch sw;

            public TimeOutHandler (SaraProtocolState saraProtocolStates, LearningSwitch learningSwitch, IOFSwitch sw) {
                this.saraProtocolStates = saraProtocolStates;
                this.learningSwitch = learningSwitch;
                this.sw = sw;
            }

            @Override
            public void run() {
                try {
                    Thread.sleep(TIME_OUT*1000);
                    saraProtocolStates = SaraProtocolState.BROADCAST;
                    this.learningSwitch.setHosts(this.sw);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void stayInGetRespondFor(int n, LearningSwitch learningSwitch) {
            if (n <= 0)
                throw new RuntimeException("parameter n should be positive");
            stayInGetRespond = n;
        }

        public SaraProtocolState nextState() {
            stayInGetRespond--;
            if (stayInGetRespond <= 0)
                return BROADCAST;
            return GET_RESPOND;
        }
    }
}
