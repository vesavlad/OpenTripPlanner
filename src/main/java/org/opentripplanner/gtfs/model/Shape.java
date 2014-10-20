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
import org.opentripplanner.gtfs.validator.table.ShapeValidator;

import java.io.Serializable;

public class Shape implements Serializable {
    final public String shape_id;
    final public double shape_pt_lat;
    final public double shape_pt_lon;
    final public int shape_pt_sequence;
    final public Optional<Double> shape_dist_traveled;

    public Shape(ShapeValidator validator) {
        shape_id = validator.requiredString("shape_id");
        shape_pt_lat = validator.requiredDouble("shape_pt_lat", -90, 90);
        shape_pt_lon = validator.requiredDouble("shape_pt_lon", -180, 180);
        shape_pt_sequence = validator.requiredInt("shape_pt_sequence", 0, Integer.MAX_VALUE);
        shape_dist_traveled = validator.optionalDouble("shape_dist_traveled");
    }
}
