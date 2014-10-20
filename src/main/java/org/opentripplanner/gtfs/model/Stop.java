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
import org.opentripplanner.gtfs.validator.table.StopValidator;

import java.io.Serializable;
import java.net.URL;
import java.util.TimeZone;

public class Stop implements Serializable {
    final public String stop_id;
    final public Optional<String> stop_code;
    final public String stop_name;
    final public Optional<String> stop_desc;
    final public double stop_lat;
    final public double stop_lon;
    final public Optional<String> zone_id;
    final public Optional<URL> stop_url;
    final public Optional<Integer> location_type;
    final public Optional<String> parent_station;
    final public Optional<TimeZone> stop_timezone;
    final public Optional<Integer> wheelchair_boarding;

    public Stop(StopValidator validator) {
        stop_id = validator.requiredString("stop_id");
        stop_code = validator.optionalString("stop_code");
        stop_name = validator.requiredString("stop_name");
        stop_desc = validator.optionalString("stop_desc");
        stop_lat = validator.requiredDouble("stop_lat", -90, 90);
        stop_lon = validator.requiredDouble("stop_lon", -180, 180);
        zone_id = validator.optionalString("zone_id");
        stop_url = validator.optionalUrl("stop_url");
        location_type = validator.optionalInt("location_type", 0, 1);
        parent_station = validator.optionalString("parent_station");
        stop_timezone = validator.optionalTz("stop_timezone");
        wheelchair_boarding = validator.optionalInt("wheelchair_boarding", 0, 2);
    }
}
