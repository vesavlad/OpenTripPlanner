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
import org.opentripplanner.gtfs.validator.table.RouteValidator;

import java.net.URL;

public class Route {
    final public String route_id;
    final public Optional<String> agency_id;
    final public String route_short_name;
    final public String route_long_name;
    final public Optional<String> route_desc;
    final public int route_type;
    final public Optional<URL> route_url;
    final public int route_color;
    final public int route_text_color;

    public Route(RouteValidator validator) {
        route_id = validator.requiredString("route_id");
        agency_id = validator.optionalString("agency_id");
        route_short_name = validator.requiredString("route_short_name");
        route_long_name = validator.requiredString("route_long_name");
        route_desc = validator.optionalString("route_desc");
        route_type = validator.requiredInt("route_type",0, 7);
        route_url = validator.optionalUrl("route_url");
        route_color = validator.optionalColor("route_color", 0XFFFFFF);
        route_text_color = validator.optionalColor("route_text_color", 0X000000);
    }
}
