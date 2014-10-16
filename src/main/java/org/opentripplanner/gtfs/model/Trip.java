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
import org.opentripplanner.gtfs.validator.table.TripValidator;

public class Trip {
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

    public Trip(TripValidator validator) {
        route_id = validator.requiredString("route_id");
        service_id = validator.requiredString("service_id");
        trip_id = validator.requiredString("trip_id");
        trip_headsign = validator.optionalString("trip_headsign");
        trip_short_name = validator.optionalString("trip_short_name");
        direction_id = validator.optionalBool("direction_id");
        block_id = validator.optionalString("block_id");
        shape_id = validator.optionalString("shape_id");
        wheelchair_accessible = validator.optionalInt("wheelchair_accessible", 0, 2);
        bikes_allowed = validator.optionalInt("bikes_allowed", 0, 2);
    }
}
