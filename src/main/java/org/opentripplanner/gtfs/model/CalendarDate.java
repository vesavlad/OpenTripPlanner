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

import org.joda.time.LocalDate;
import org.opentripplanner.gtfs.format.FeedFile;

import java.util.Map;

import static org.opentripplanner.gtfs.format.FeedFile.CALENDAR_DATES;
import static org.opentripplanner.gtfs.validator.FeedValidator.requiredDate;
import static org.opentripplanner.gtfs.validator.FeedValidator.requiredInt;
import static org.opentripplanner.gtfs.validator.FeedValidator.requiredString;

public class CalendarDate {
    final static public FeedFile FEED_FILE = CALENDAR_DATES;

    final public String service_id;
    final public LocalDate date;
    final public int exception_type;

    public CalendarDate(Map<String, String> row) {
        service_id = requiredString(row, "service_id", FEED_FILE);
        date = requiredDate(row, "date", FEED_FILE);
        exception_type = requiredInt(row, "exception_type", 1, 2, FEED_FILE);
    }
}
