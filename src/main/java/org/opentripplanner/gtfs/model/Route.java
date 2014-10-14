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
import java.util.Map;

import static org.opentripplanner.gtfs.format.FeedFile.ROUTES;
import static org.opentripplanner.gtfs.validator.FeedValidator.optionalColor;
import static org.opentripplanner.gtfs.validator.FeedValidator.optionalString;
import static org.opentripplanner.gtfs.validator.FeedValidator.optionalUrl;
import static org.opentripplanner.gtfs.validator.FeedValidator.requiredInt;
import static org.opentripplanner.gtfs.validator.FeedValidator.requiredString;

public class Route {
    final static public FeedFile FEED_FILE = ROUTES;

    final public String route_id;
    final public Optional<String> agency_id;
    final public String route_short_name;
    final public String route_long_name;
    final public Optional<String> route_desc;
    final public int route_type;
    final public Optional<URL> route_url;
    final public int route_color;
    final public int route_text_color;

    public Route(Map<String, String> row) {
        route_id = requiredString(row, "route_id", FEED_FILE);
        agency_id = optionalString(row, "agency_id", FEED_FILE);
        route_short_name = requiredString(row, "route_short_name", FEED_FILE);
        route_long_name = requiredString(row, "route_long_name", FEED_FILE);
        route_desc = optionalString(row, "route_desc", FEED_FILE);
        route_type = requiredInt(row, "route_type",0, 7, FEED_FILE);
        route_url = optionalUrl(row, "route_url", FEED_FILE);
        route_color = optionalColor(row, "route_color", 0XFFFFFF, FEED_FILE);
        route_text_color = optionalColor(row, "route_text_color", 0X000000, FEED_FILE);
    }
}
