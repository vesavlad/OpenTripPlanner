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
import org.opentripplanner.gtfs.validator.table.StopTimeValidator;

public class StopTime {
    final public String trip_id;
    final public int arrival_time;
    final public int departure_time;
    final public String stop_id;
    final public int stop_sequence;
    final public Optional<String> stop_headsign;
    final public Optional<Integer> pickup_type;
    final public Optional<Integer> drop_off_type;
    final public Optional<Double> shape_dist_traveled;

    public StopTime(StopTimeValidator validator) {
        trip_id = validator.requiredString("trip_id");
        arrival_time = validator.requiredTimeOfDay("arrival_time");
        departure_time = validator.requiredTimeOfDay("departure_time");
        stop_id = validator.requiredString("stop_id");
        stop_sequence = validator.requiredInt("stop_sequence", 0, Integer.MAX_VALUE);
        stop_headsign = validator.optionalString("stop_headsign");
        pickup_type = validator.optionalInt("pickup_type", 0, 3);
        drop_off_type = validator.optionalInt("drop_off_type", 0, 3);
        shape_dist_traveled = validator.optionalDouble("shape_dist_traveled");
    }
}
