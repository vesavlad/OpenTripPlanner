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

import static org.opentripplanner.gtfs.format.FeedFile.TRIPS;
import static org.opentripplanner.gtfs.validator.FeedValidator.optionalDouble;
import static org.opentripplanner.gtfs.validator.FeedValidator.optionalInt;
import static org.opentripplanner.gtfs.validator.FeedValidator.optionalString;
import static org.opentripplanner.gtfs.validator.FeedValidator.requiredInt;
import static org.opentripplanner.gtfs.validator.FeedValidator.requiredString;
import static org.opentripplanner.gtfs.validator.FeedValidator.requiredTimeOfDay;

public class StopTime {
    final static public FeedFile FEED_FILE = TRIPS;

    final public String trip_id;
    final public int arrival_time;
    final public int departure_time;
    final public String stop_id;
    final public int stop_sequence;
    final public Optional<String> stop_headsign;
    final public int pickup_type;
    final public int drop_off_type;
    final public Optional<Double> shape_dist_traveled;

    public StopTime(Map<String, String> row) {
        trip_id = requiredString(row, "trip_id", FEED_FILE);
        arrival_time = requiredTimeOfDay(row, "arrival_time", FEED_FILE);
        departure_time = requiredTimeOfDay(row, "departure_time", FEED_FILE);
        stop_id = requiredString(row, "stop_id", FEED_FILE);
        stop_sequence = requiredInt(row, "stop_sequence", 0, Integer.MAX_VALUE, FEED_FILE);
        stop_headsign = optionalString(row, "stop_headsign", FEED_FILE);
        pickup_type = optionalInt(row, "pickup_type", 0, 3, FEED_FILE);
        drop_off_type = requiredInt(row, "drop_off_type", 0, 3, FEED_FILE);
        shape_dist_traveled = optionalDouble(row, "shape_dist_traveled", FEED_FILE);
    }
}
