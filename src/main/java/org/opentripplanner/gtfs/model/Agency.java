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
import org.opentripplanner.gtfs.validator.table.AgencyValidator;

import java.io.Serializable;
import java.net.URL;
import java.util.Locale;
import java.util.TimeZone;

public class Agency implements Serializable {
    final public Optional<String> agency_id;
    final public String agency_name;
    final public URL agency_url;
    final public TimeZone agency_timezone;
    final public Optional<Locale> agency_lang;
    final public Optional<String> agency_phone;
    final public Optional<URL> agency_fare_url;

    public Agency(AgencyValidator validator) {
        agency_id = validator.optionalString("agency_id");
        agency_name = validator.requiredString("agency_name");
        agency_url = validator.requiredUrl("agency_url");
        agency_timezone = validator.requiredTz("agency_timezone");
        agency_lang = validator.optionalLang("agency_lang");
        agency_phone = validator.optionalString("agency_phone");
        agency_fare_url = validator.optionalUrl("agency_fare_url");
    }
}
