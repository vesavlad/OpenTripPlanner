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

import static org.opentripplanner.gtfs.format.FeedFile.FARE_RULES;
import static org.opentripplanner.gtfs.validator.FeedValidator.optionalString;
import static org.opentripplanner.gtfs.validator.FeedValidator.requiredString;

public class FareRule {
    final static public FeedFile FEED_FILE = FARE_RULES;

    final public String fare_id;
    final public Optional<String> route_id;
    final public Optional<String> origin_id;
    final public Optional<String> destination_id;
    final public Optional<String> contains_id;

    public FareRule(Map<String, String> row) {
        fare_id = requiredString(row, "fare_id", FEED_FILE);
        route_id = optionalString(row, "route_id", FEED_FILE);
        origin_id = optionalString(row, "origin_id", FEED_FILE);
        destination_id = optionalString(row, "destination_id", FEED_FILE);
        contains_id = optionalString(row, "contains_id", FEED_FILE);
    }
}
