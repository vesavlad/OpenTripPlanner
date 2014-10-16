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
import org.opentripplanner.gtfs.validator.table.FrequencyValidator;

public class Frequency {
    final public String trip_id;
    final public int start_time;
    final public int end_time;
    final public int headway_secs;
    final public Optional<Integer> exact_times;

    public Frequency(FrequencyValidator validator) {
        trip_id = validator.requiredString("trip_id");
        start_time = validator.requiredTimeOfDay("start_time");
        end_time = validator.requiredTimeOfDay("end_time");
        headway_secs = validator.requiredInt("headway_secs", 0, Integer.MAX_VALUE);
        exact_times = validator.optionalInt("exact_times", 0, 1);
    }
}
