package net.floodlightcontroller.learningswitch;

import com.google.common.collect.HashMultimap;
import net.floodlightcontroller.core.IOFSwitch;
import org.sdnplatform.sync.internal.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Prim {
    protected static Logger logger = LoggerFactory.getLogger(LearningSwitch.class);
    private HashMultimap<IOFSwitch, IOFSwitch> Edges = HashMultimap.create();
    private PriorityQueue <Pair <Long, Pair<IOFSwitch, IOFSwitch> > > queue;
    private Collection<Long> choosed = new HashSet<>(), allSwitches = new HashSet<>();
    private IOFSwitch current;

    public Prim(IOFSwitch current) {
        queue = new PriorityQueue <>(Comparator.comparingInt(a -> a.getKey().intValue()));
        this.current = current;
        addEdge((long) 0, current);
    }

    public void addEdge(Long w, IOFSwitch u) {
        if(choosed.contains(u.getId().getLong()))
            return ;

        allSwitches.add(u.getId().getLong());
        logger.info("Prim add edge: " + current.getId().toString() + " " + u.getId().toString());
        queue.add(new Pair<>(w, new Pair<>(u, current)));
    }

    public IOFSwitch next() {
        while(!queue.isEmpty()) {
            Pair <Long, Pair<IOFSwitch, IOFSwitch>> v = queue.poll();
            if (choosed.contains(v.getValue().getKey()))
                continue;

            IOFSwitch parent = v.getValue().getValue();
            current = v.getValue().getKey();
            choosed.add(current.getId().getLong());

            Edges.put(parent, current);
            Edges.put(current, parent);
            break;
        }
        logger.info("Prim choosed: " + current.getId().toString());
        return current;
    }

    public boolean isDone() {
        return allSwitches.size() == choosed.size();
    }

    public boolean hasEdge(long v, long u) {
        for (IOFSwitch x: Edges.keySet())
            for (IOFSwitch y: Edges.get(x))
                if (x.getId().getLong() == v && y.getId().getLong() == u)
                    return true;

        return false;
    }

    public Collection<IOFSwitch> getNeighbours(IOFSwitch v) {
        return Edges.get(v);
    }
}
