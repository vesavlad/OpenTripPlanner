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
import org.opentripplanner.gtfs.validator.table.FeedInfoValidator;

import java.io.Serializable;
import java.net.URL;
import java.util.Locale;

public class FeedInfo implements Serializable {
    final public String feed_publisher_name;
    final public URL feed_publisher_url;
    final public Locale feed_lang;
    final public Optional<LocalDate> feed_start_date;
    final public Optional<LocalDate> feed_end_date;
    final public Optional<String> feed_version;

    public FeedInfo(FeedInfoValidator validator) {
        feed_publisher_name = validator.requiredString("feed_publisher_name");
        feed_publisher_url = validator.requiredUrl("feed_publisher_url");
        feed_lang = validator.requiredLang("feed_lang");
        feed_start_date = validator.optionalDate("feed_start_date");
        feed_end_date = validator.optionalDate("feed_end_date");
        feed_version = validator.optionalString("feed_version");
    }
}
