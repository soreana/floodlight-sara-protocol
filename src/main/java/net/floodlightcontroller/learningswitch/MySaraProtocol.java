package net.floodlightcontroller.learningswitch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MySaraProtocol extends SaraProtocol implements SaraProtocolUtils {


    @Override
    public boolean haveEntryFor(long id){
        if(waitingRoom.containsKey(id)) return true;
        return false;
    }
    @Override
    public void setCurrentSwitch(long currentSwitch) {
        // todo save ports of ex switch
        this.currentSwitch = currentSwitch;
    }
    @Override
    public void makeEntryFor(long currentSwitchId) {
        waitingRoom.put(currentSwitchId,new HashSet<>());
        // todo
    }

    public Graph createMSTfromTopology() {

        int n = learned.size();

        Map<Integer,Integer> vertices = new HashMap<>(n);
        Graph graph = new Graph(vertices.size(), n);

        // using for-each loop for iteration over Map.entrySet()
        for (Map.Entry<InEntry,OutEntry> entry : learned.entrySet()) {

            int switch1 = (int)entry.getKey().getSw();
            int switch2 = (int)entry.getValue().getSw();

            if ( !vertices.containsKey(switch1))
                vertices.put(switch1,1);
            if ( !vertices.containsKey(switch2))
                vertices.put(switch2,1);
        }


        for (Map.Entry<InEntry,OutEntry> entry : learned.entrySet()) {

            int switch1 = (int)entry.getKey().getSw();
            int switch2 = (int)entry.getValue().getSw();
            Long weight = entry.getKey().getValue();
            graph.addEdge(switch1,switch2,weight);
        }
        return graph;
    }



}



