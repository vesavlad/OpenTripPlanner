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
import org.opentripplanner.gtfs.validator.table.FareRuleValidator;

public class FareRule {
    final public String fare_id;
    final public Optional<String> route_id;
    final public Optional<String> origin_id;
    final public Optional<String> destination_id;
    final public Optional<String> contains_id;

    public FareRule(FareRuleValidator validator) {
        fare_id = validator.requiredString("fare_id");
        route_id = validator.optionalString("route_id");
        origin_id = validator.optionalString("origin_id");
        destination_id = validator.optionalString("destination_id");
        contains_id = validator.optionalString("contains_id");
    }
}
