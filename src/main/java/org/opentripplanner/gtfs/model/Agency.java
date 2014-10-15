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
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static org.opentripplanner.gtfs.format.FeedFile.AGENCY;
import static org.opentripplanner.gtfs.validator.FeedValidator.optionalLang;
import static org.opentripplanner.gtfs.validator.FeedValidator.optionalString;
import static org.opentripplanner.gtfs.validator.FeedValidator.optionalUrl;
import static org.opentripplanner.gtfs.validator.FeedValidator.requiredString;
import static org.opentripplanner.gtfs.validator.FeedValidator.requiredTz;
import static org.opentripplanner.gtfs.validator.FeedValidator.requiredUrl;

public class Agency {
    final static public FeedFile FEED_FILE = AGENCY;

    final public Optional<String> agency_id;
    final public String agency_name;
    final public URL agency_url;
    final public TimeZone agency_timezone;
    final public Optional<Locale> agency_lang;
    final public Optional<String> agency_phone;
    final public Optional<URL> agency_fare_url;

    public Agency(Map<String, String> row) {
        agency_id = optionalString(row, "agency_id", FEED_FILE);
        agency_name = requiredString(row, "agency_name", FEED_FILE);
        agency_url = requiredUrl(row, "agency_url", FEED_FILE);
        agency_timezone = requiredTz(row, "agency_timezone", FEED_FILE);
        agency_lang = optionalLang(row, "agency_lang", FEED_FILE);
        agency_phone = optionalString(row, "agency_phone", FEED_FILE);
        agency_fare_url = optionalUrl(row, "agency_fare_url", FEED_FILE);
    }
}
