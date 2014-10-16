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
import org.opentripplanner.gtfs.validator.table.FareAttributeValidator;

import java.util.Currency;

public class FareAttribute {
    final public String fare_id;
    final public double price;
    final public Currency currency_type;
    final public int payment_method;
    final public Optional<Integer> transfers;
    final public Optional<Integer> transfer_duration;

    public FareAttribute(FareAttributeValidator validator) {
        fare_id = validator.requiredString("fare_id");
        price = validator.requiredDouble("price", 0, Double.MAX_VALUE);
        currency_type = validator.requiredCurrency("currency_type");
        payment_method = validator.requiredInt("payment_method", 0, 1);
        transfers = validator.requiredIntOptionalValue("transfers", 0, 2);
        transfer_duration = validator.optionalInt("transfer_duration", 0, Integer.MAX_VALUE);
    }
}
