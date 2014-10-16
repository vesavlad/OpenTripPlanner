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

import static org.opentripplanner.gtfs.format.FeedFile.TRANSFERS;
import static org.opentripplanner.gtfs.validator.feed.FeedValidator.optionalInt;
import static org.opentripplanner.gtfs.validator.feed.FeedValidator.requiredIntOptionalValue;
import static org.opentripplanner.gtfs.validator.feed.FeedValidator.requiredString;

public class Transfer {
    final static public FeedFile FEED_FILE = TRANSFERS;

    final public String from_stop_id;
    final public String to_stop_id;
    final public Optional<Integer> transfer_type;
    final public Optional<Integer> min_transfer_time;

    public Transfer(Map<String, String> row) {
        from_stop_id = requiredString(row, "from_stop_id", FEED_FILE);
        to_stop_id = requiredString(row, "to_stop_id", FEED_FILE);
        transfer_type = requiredIntOptionalValue(row, "transfer_type", 0, 3, FEED_FILE);
        min_transfer_time = optionalInt(row, "min_transfer_time", 0, Integer.MAX_VALUE, FEED_FILE);
    }
}
