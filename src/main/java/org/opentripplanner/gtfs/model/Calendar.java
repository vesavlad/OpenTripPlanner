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
import org.opentripplanner.gtfs.validator.table.CalendarValidator;

import java.io.Serializable;

public class Calendar implements Serializable {
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

    public Calendar(CalendarValidator validator) {
        service_id = validator.requiredString("service_id");
        monday = validator.requiredBoolean("monday");
        tuesday = validator.requiredBoolean("tuesday");
        wednesday = validator.requiredBoolean("wednesday");
        thursday = validator.requiredBoolean("thursday");
        friday = validator.requiredBoolean("friday");
        saturday = validator.requiredBoolean("saturday");
        sunday = validator.requiredBoolean("sunday");
        start_date = validator.requiredDate("start_date");
        end_date = validator.requiredDate("end_date");
    }
}
