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
import org.joda.time.LocalDate;
import org.opentripplanner.gtfs.format.FeedFile;

import java.net.URL;
import java.util.Locale;
import java.util.Map;

import static org.opentripplanner.gtfs.format.FeedFile.FEED_INFO;
import static org.opentripplanner.gtfs.validator.feed.FeedValidator.optionalDate;
import static org.opentripplanner.gtfs.validator.feed.FeedValidator.optionalString;
import static org.opentripplanner.gtfs.validator.feed.FeedValidator.requiredLang;
import static org.opentripplanner.gtfs.validator.feed.FeedValidator.requiredString;
import static org.opentripplanner.gtfs.validator.feed.FeedValidator.requiredUrl;

public class FeedInfo {
    final static public FeedFile FEED_FILE = FEED_INFO;

    final public String feed_publisher_name;
    final public URL feed_publisher_url;
    final public Locale feed_lang;
    final public Optional<LocalDate> feed_start_date;
    final public Optional<LocalDate> feed_end_date;
    final public Optional<String> feed_version;

    public FeedInfo(Map<String, String> row) {
        feed_publisher_name = requiredString(row, "feed_publisher_name", FEED_FILE);
        feed_publisher_url = requiredUrl(row, "feed_publisher_url", FEED_FILE);
        feed_lang = requiredLang(row, "feed_lang", FEED_FILE);
        feed_start_date = optionalDate(row, "feed_start_date", FEED_FILE);
        feed_end_date = optionalDate(row, "feed_end_date", FEED_FILE);
        feed_version = optionalString(row, "feed_version", FEED_FILE);
    }
}
