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
import static org.opentripplanner.gtfs.validator.feed.FeedValidator.optionalBool;
import static org.opentripplanner.gtfs.validator.feed.FeedValidator.optionalInt;
import static org.opentripplanner.gtfs.validator.feed.FeedValidator.optionalString;
import static org.opentripplanner.gtfs.validator.feed.FeedValidator.requiredString;

public class Trip {
    final static public FeedFile FEED_FILE = TRIPS;

    final public String route_id;
    final public String service_id;
    final public String trip_id;
    final public Optional<String> trip_headsign;
    final public Optional<String> trip_short_name;
    final public Optional<Boolean> direction_id;
    final public Optional<String> block_id;
    final public Optional<String> shape_id;
    final public Optional<Integer> wheelchair_accessible;
    final public Optional<Integer> bikes_allowed;

    public Trip(Map<String, String> row) {
        route_id = requiredString(row, "route_id", FEED_FILE);
        service_id = requiredString(row, "service_id", FEED_FILE);
        trip_id = requiredString(row, "trip_id", FEED_FILE);
        trip_headsign = optionalString(row, "trip_headsign", FEED_FILE);
        trip_short_name = optionalString(row, "trip_short_name", FEED_FILE);
        direction_id = optionalBool(row, "direction_id", FEED_FILE);
        block_id = optionalString(row, "block_id", FEED_FILE);
        shape_id = optionalString(row, "shape_id", FEED_FILE);
        wheelchair_accessible = optionalInt(row, "wheelchair_accessible", 0, 2, FEED_FILE);
        bikes_allowed = optionalInt(row, "bikes_allowed", 0, 2, FEED_FILE);
    }
}
