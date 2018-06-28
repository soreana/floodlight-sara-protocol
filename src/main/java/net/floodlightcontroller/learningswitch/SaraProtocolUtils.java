package net.floodlightcontroller.learningswitch;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IpProtocol;

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
//            return ip.getProtocol() == IpProtocol.ICMP;
            return false;
        } else return eth.getEtherType() == EthType.ARP;
    }

    long TIME_OUT = 100;

    enum SaraProtocolState {
        BROADCAST,
        GET_RESPOND,
        ROUTE;
        private int stayInGetRespond = 0;

        public void stayInGetRespondFor(int n) {
            if (n <= 0)
                throw new RuntimeException("parameter n should be positive");
            stayInGetRespond = n;
        }

        public SaraProtocolState nextState() {
            stayInGetRespond--;
            if (stayInGetRespond <= 0)
                return ROUTE;
            return GET_RESPOND;
        }
    }
}
