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

package org.opentripplanner.gtfs.validator.table;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.opentripplanner.gtfs.format.FeedFile;
import org.opentripplanner.gtfs.validator.ValidationException;

import java.util.Map;

abstract class TableValidator<T> implements Iterable<T> {
    final private FeedFile feedFile;
    final Iterable<Map<String, String>> maps;
    private int line = 1;

    TableValidator(FeedFile feedFile, Iterable<Map<String, String>> input) {
        this.feedFile = feedFile;
        maps = Iterables.transform(input, new Function<Map<String, String>, Map<String, String>>() {
            @Override
            public Map<String, String> apply(Map<String, String> input) {
                line++;

                if (input.keySet().size() == input.values().size()) {
                    return input;
                } else {
                    throw new ValidationException(TableValidator.this.feedFile, String.format(
                            "incorrect column count (expected %d cells, found %d cells) on line %d",
                            input.keySet().size(), input.values().size(), line));
                }
            }
        });
    }
}
