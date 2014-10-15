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

import java.net.URL;
import java.util.Map;
import java.util.TimeZone;

import static org.opentripplanner.gtfs.format.FeedFile.STOPS;
import static org.opentripplanner.gtfs.validator.FeedValidator.optionalInt;
import static org.opentripplanner.gtfs.validator.FeedValidator.optionalString;
import static org.opentripplanner.gtfs.validator.FeedValidator.optionalTz;
import static org.opentripplanner.gtfs.validator.FeedValidator.optionalUrl;
import static org.opentripplanner.gtfs.validator.FeedValidator.requiredDouble;
import static org.opentripplanner.gtfs.validator.FeedValidator.requiredString;

public class Stop {
    final static public FeedFile FEED_FILE = STOPS;

    final public String stop_id;
    final public Optional<String> stop_code;
    final public String stop_name;
    final public Optional<String> stop_desc;
    final public double stop_lat;
    final public double stop_lon;
    final public Optional<String> zone_id;
    final public Optional<URL> stop_url;
    final public Optional<Integer> location_type;
    final public Optional<String> parent_station;
    final public Optional<TimeZone> stop_timezone;
    final public Optional<Integer> wheelchair_boarding;

    public Stop(Map<String, String> row) {
        stop_id = requiredString(row, "stop_id", FEED_FILE);
        stop_code = optionalString(row, "stop_code", FEED_FILE);
        stop_name = requiredString(row, "stop_name", FEED_FILE);
        stop_desc = optionalString(row, "stop_desc", FEED_FILE);
        stop_lat = requiredDouble(row, "stop_lat", -90, 90, FEED_FILE);
        stop_lon = requiredDouble(row, "stop_lon", -180, 180, FEED_FILE);
        zone_id = optionalString(row, "zone_id", FEED_FILE);
        stop_url = optionalUrl(row, "stop_url", FEED_FILE);
        location_type = optionalInt(row, "location_type", 0, 1, FEED_FILE);
        parent_station = optionalString(row, "parent_station", FEED_FILE);
        stop_timezone = optionalTz(row, "stop_timezone", FEED_FILE);
        wheelchair_boarding = optionalInt(row, "wheelchair_boarding", 0, 2, FEED_FILE);
    }
}
