package org.opentripplanner.routing.edgetype;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.ElevatorOffboardVertex;
import org.opentripplanner.routing.vertextype.ElevatorOnboardVertex;
import org.opentripplanner.transit.model.basic.I18NString;
import org.opentripplanner.util.geometry.GeometryUtils;

/**
 * A relatively low cost edge for alighting from an elevator. All narrative generation is done by
 * the ElevatorAlightEdge (other edges are silent), because it is the only edge that knows where the
 * user is to get off.
 *
 * @author mattwigway
 */
public class ElevatorAlightEdge extends Edge implements BikeWalkableEdge, ElevatorEdge {

  private static final long serialVersionUID = 3925814840369402222L;

  /**
   * This is the level of this elevator exit, used in narrative generation.
   */
  private final I18NString level;

  /**
   * The polyline geometry of this edge. It's generally a polyline with two coincident points, but
   * some elevators have horizontal dimension, e.g. the ones on the Eiffel Tower.
   */
  private final LineString the_geom;

  /**
   * @param level It's a float for future expansion.
   */
  public ElevatorAlightEdge(
    ElevatorOnboardVertex from,
    ElevatorOffboardVertex to,
    I18NString level
  ) {
    super(from, to);
    this.level = level;

    // set up the geometry
    Coordinate[] coords = new Coordinate[2];
    coords[0] = new Coordinate(from.getX(), from.getY());
    coords[1] = new Coordinate(to.getX(), to.getY());
    the_geom = GeometryUtils.getGeometryFactory().createLineString(coords);
  }

  public String toString() {
    return "ElevatorAlightEdge(" + fromv + " -> " + tov + ")";
  }

  @Override
  public State traverse(State s0) {
    StateEditor s1 = createEditorForDrivingOrWalking(s0, this);
    s1.incrementWeight(1);
    return s1.makeState();
  }

  /**
   * The level from OSM is the name
   */
  @Override
  public I18NString getName() {
    return level;
  }

  /**
   * The name is not bogus; it's level n from OSM.
   *
   * @author mattwigway
   */
  @Override
  public boolean hasBogusName() {
    return false;
  }

  @Override
  public LineString getGeometry() {
    return the_geom;
  }

  @Override
  public double getDistanceMeters() {
    return 0;
  }
}
