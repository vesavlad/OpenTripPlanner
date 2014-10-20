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
import org.opentripplanner.gtfs.validator.table.TransferValidator;

import java.io.Serializable;

public class Transfer implements Serializable {
    final public String from_stop_id;
    final public String to_stop_id;
    final public Optional<Integer> transfer_type;
    final public Optional<Integer> min_transfer_time;

    public Transfer(TransferValidator validator) {
        from_stop_id = validator.requiredString("from_stop_id");
        to_stop_id = validator.requiredString("to_stop_id");
        transfer_type = validator.requiredIntOptionalValue("transfer_type", 0, 3);
        min_transfer_time = validator.optionalInt("min_transfer_time", 0, Integer.MAX_VALUE);
    }
}
