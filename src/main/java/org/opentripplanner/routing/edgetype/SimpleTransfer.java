package org.opentripplanner.routing.edgetype;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Represents a transfer between stops that does not take the street network into account.
 *
 * TODO these should really have a set of valid modes in case bike vs. walk transfers are different
 */
public class SimpleTransfer extends Edge {
    private static final long serialVersionUID = 20140408L;

    private double distance;
    
    private List<Edge> edges;

    public SimpleTransfer(TransitStopVertex from, TransitStopVertex to, double distance, LineString geometry, List<Edge> edges) {
        super(from, to);
        this.distance = distance;
        this.edges = edges;
    }

    public SimpleTransfer(TransitStopVertex from, TransitStopVertex to, double distance, LineString geometry) {
        this(from, to, distance, geometry, null);
    }

    @Override
    public State traverse(State s0) {
        // Forbid taking shortcuts composed of two transfers in a row
        if (s0.backEdge instanceof SimpleTransfer) {
            return null;
        }
        if (s0.backEdge instanceof StreetTransitLink) {
            return null;
        }
        if(distance > s0.getOptions().maxTransferWalkDistance) {
            return null;
        }
        // Only transfer right after riding a vehicle.
        RoutingRequest rr = s0.getOptions();
        double walkspeed = rr.walkSpeed;
        StateEditor se = s0.edit(this);
        se.setBackMode(TraverseMode.WALK);
        int time = (int) Math.ceil(distance / walkspeed) + 2 * StreetTransitLink.STL_TRAVERSE_COST;
        se.incrementTimeInSeconds(time);
        se.incrementWeight(time * rr.walkReluctance);
        se.incrementWalkDistance(distance);
        return se.makeState();
    }

    @Override
    public String getName() {
        return fromv.getName() + " => " + tov.getName();
    }
    
    @Override
    public String getName(Locale locale) {
        //TODO: localize
        return this.getName();
    }

    @Override
    public double weightLowerBound(RoutingRequest rr) {
        int time = (int) (distance / rr.walkSpeed); 
        return (time * rr.walkReluctance);
    }
    
    @Override
    public double getDistance(){
    	return this.distance;
    }

    @Override
    public LineString getGeometry(){
        return GeometryUtils.concatenateLineStrings(
                edges.stream()
                        .filter(t -> t.getGeometry() != null)
                        .map(Edge::getGeometry)
                        .collect(Collectors.toList()));
   }

    public List<Edge> getEdges() { return this.edges; }

    @Override
    public String toString() {
        return "SimpleTransfer " + getName();
    }
}
