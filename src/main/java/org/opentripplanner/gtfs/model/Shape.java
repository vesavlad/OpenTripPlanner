/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.gtfs.model;

import com.google.common.base.Optional;
import org.opentripplanner.gtfs.format.FeedFile;

import java.util.Map;

import static org.opentripplanner.gtfs.format.FeedFile.SHAPES;
import static org.opentripplanner.gtfs.validator.FeedValidator.optionalDouble;
import static org.opentripplanner.gtfs.validator.FeedValidator.requiredDouble;
import static org.opentripplanner.gtfs.validator.FeedValidator.requiredInt;
import static org.opentripplanner.gtfs.validator.FeedValidator.requiredString;

public class Shape {
    final static public FeedFile FEED_FILE = SHAPES;

    final public String shape_id;
    final public double shape_pt_lat;
    final public double shape_pt_lon;
    final public int shape_pt_sequence;
    final public Optional<Double> shape_dist_traveled;

    public Shape(Map<String, String> row) {
        shape_id = requiredString(row, "shape_id", FEED_FILE);
        shape_pt_lat = requiredDouble(row, "shape_pt_lat", -90, 90, FEED_FILE);
        shape_pt_lon = requiredDouble(row, "shape_pt_lon", -180, 180, FEED_FILE);
        shape_pt_sequence = requiredInt(row, "shape_pt_sequence", 0, Integer.MAX_VALUE, FEED_FILE);
        shape_dist_traveled = optionalDouble(row, "shape_dist_traveled", FEED_FILE);
    }
}
