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

import static org.opentripplanner.gtfs.format.FeedFile.FREQUENCIES;
import static org.opentripplanner.gtfs.validator.feed.FeedValidator.optionalInt;
import static org.opentripplanner.gtfs.validator.feed.FeedValidator.requiredInt;
import static org.opentripplanner.gtfs.validator.feed.FeedValidator.requiredString;
import static org.opentripplanner.gtfs.validator.feed.FeedValidator.requiredTimeOfDay;

public class Frequency {
    final static public FeedFile FEED_FILE = FREQUENCIES;

    final public String trip_id;
    final public int start_time;
    final public int end_time;
    final public int headway_secs;
    final public Optional<Integer> exact_times;

    public Frequency(Map<String, String> row) {
        trip_id = requiredString(row, "trip_id", FEED_FILE);
        start_time = requiredTimeOfDay(row, "start_time", FEED_FILE);
        end_time = requiredTimeOfDay(row, "end_time", FEED_FILE);
        headway_secs = requiredInt(row, "headway_secs", 0, Integer.MAX_VALUE, FEED_FILE);
        exact_times = optionalInt(row, "exact_times", 0, 1, FEED_FILE);
    }
}
