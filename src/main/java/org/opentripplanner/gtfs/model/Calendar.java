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

import static org.opentripplanner.gtfs.format.FeedFile.CALENDAR;
import static org.opentripplanner.gtfs.validator.feed.FeedValidator.requiredBool;
import static org.opentripplanner.gtfs.validator.feed.FeedValidator.requiredDate;
import static org.opentripplanner.gtfs.validator.feed.FeedValidator.requiredString;

public class Calendar {
    final static public FeedFile FEED_FILE = CALENDAR;

    final public String service_id;
    final public boolean monday;
    final public boolean tuesday;
    final public boolean wednesday;
    final public boolean thursday;
    final public boolean friday;
    final public boolean saturday;
    final public boolean sunday;
    final public LocalDate start_date;
    final public LocalDate end_date;

    public Calendar(Map<String, String> row) {
        service_id = requiredString(row, "service_id", FEED_FILE);
        monday = requiredBool(row, "monday", FEED_FILE);
        tuesday = requiredBool(row, "tuesday", FEED_FILE);
        wednesday = requiredBool(row, "wednesday", FEED_FILE);
        thursday = requiredBool(row, "thursday", FEED_FILE);
        friday = requiredBool(row, "friday", FEED_FILE);
        saturday = requiredBool(row, "saturday", FEED_FILE);
        sunday = requiredBool(row, "sunday", FEED_FILE);
        start_date = requiredDate(row, "start_date", FEED_FILE);
        end_date = requiredDate(row, "end_date", FEED_FILE);
    }
}
