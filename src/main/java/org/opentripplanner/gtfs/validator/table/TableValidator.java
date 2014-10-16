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
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import org.joda.time.LocalDate;
import org.opentripplanner.gtfs.format.FeedFile;
import org.opentripplanner.gtfs.validator.ValidationException;

import java.net.URL;
import java.util.Currency;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

abstract class TableValidator<T> implements Iterable<T> {
    final private FeedFile feedFile;
    final Iterable<Map<String, String>> maps;
    private int line = 1;
    Map<String, String> row;

    TableValidator(FeedFile feedFile, Iterable<Map<String, String>> input) {
        this.feedFile = feedFile;
        maps = Iterables.transform(input, new Function<Map<String, String>, Map<String, String>>() {
            @Override
            public Map<String, String> apply(Map<String, String> input) {
                line++;

                if (input.keySet().size() == input.values().size()) {
                    return input;
                } else {
                    throw new ValidationException(TableValidator.this.feedFile, line, String.format(
                            "incorrect column count (expected %d cells, found %d cells)",
                            input.keySet().size(), input.values().size()));
                }
            }
        });
    }

    public String requiredString(String column) {
        String string = row.get(column);

        if (string == null) {
            throw new ValidationException(feedFile, 1, "required column " + column + " is omitted");
        } else {
            return string;
        }
    }

    public Optional<String> optionalString(String column) {
        String string = row.get(column);

        if (string == null) {
            return Optional.absent();
        } else {
            return Optional.of(string);
        }
    }

    public URL requiredUrl(String column) {
        return stringToUrl(requiredString(column));
    }

    public Optional<URL> optionalUrl(String column) {
        String string = row.get(column);

        if (Strings.isNullOrEmpty(string)) {
            return Optional.absent();
        } else {
            return Optional.of(stringToUrl(string));
        }
    }

    public TimeZone requiredTz(String column) {
        return stringToTz(requiredString(column));
    }

    public Optional<TimeZone> optionalTz(String column) {
        String string = row.get(column);

        if (Strings.isNullOrEmpty(string)) {
            return Optional.absent();
        } else {
            return Optional.of(stringToTz(string));
        }
    }

    public Locale requiredLang(String column) {
        return stringToLang(requiredString(column));
    }

    public Optional<Locale> optionalLang(String column) {
        String string = row.get(column);

        if (string == null) {
            return Optional.absent();
        } else {
            return Optional.of(stringToLang(string));
        }
    }

    public double requiredDouble(String column, double min, double max) {
        return stringToDouble(requiredString(column), min, max);
    }

    public Optional<Double> optionalDouble(String column) {
        String string = row.get(column);

        if (Strings.isNullOrEmpty(string)) {
            return Optional.absent();
        } else {
            return Optional.of(stringToDouble(string, 0, Double.MAX_VALUE));
        }
    }

    public int requiredInt(String column, int min, int max) {
        return stringToInt(requiredString(column), min, max);
    }

    public Optional<Integer> requiredIntOptionalValue(String column, int min, int max) {
        String string = requiredString(column);

        if (string.equals("")) {
            return Optional.absent();
        } else {
            return Optional.of(stringToInt(string, min, max));
        }
    }

    public Optional<Integer> optionalInt(String column, int min, int max) {
        String string = row.get(column);

        if (Strings.isNullOrEmpty(string)) {
            return Optional.absent();
        } else {
            return Optional.of(stringToInt(string, min, max));
        }
    }

    public int optionalColor(String column, int defaultColor) {
        String string = row.get(column);

        if (Strings.isNullOrEmpty(string)) {
            return defaultColor;
        } else {
            return stringHexToInt(string, 0X000000, 0XFFFFFF);
        }
    }

    public boolean requiredBool(String column) {
        return stringBinToBool(requiredString(column));
    }

    public Optional<Boolean> optionalBool(String column) {
        String string = row.get(column);

        if (string == null) {
            return Optional.absent();
        } else {
            return Optional.of(stringBinToBool(string));
        }
    }

    public int requiredTimeOfDay(String column) {
        return stringToTimeOfDay(requiredString(column));
    }

    public LocalDate requiredDate(String column) {
        return stringToDate(requiredString(column));
    }

    public Optional<LocalDate> optionalDate(String column) {
        String string = row.get(column);

        if (string == null) {
            return Optional.absent();
        } else {
            return Optional.of(stringToDate(string));
        }
    }

    public Currency requiredCurrency(String column) {
        return stringToCurrency(requiredString(column));
    }

    private URL stringToUrl(String string) {
        try {
            return new URL(string);
        } catch (Exception e) {
            throw new ValidationException(feedFile, line, e);
        }
    }

    private TimeZone stringToTz(String string) {
        try {
            return TimeZone.getTimeZone(string);
        } catch (Exception e) {
            throw new ValidationException(feedFile, line, e);
        }
    }

    private Locale stringToLang(String string) {
        try {
            return new Locale(string);
        } catch (Exception e) {
            throw new ValidationException(feedFile, line, e);
        }
    }

    private double stringToDouble(String string, double min, double max) {
        double value;

        try {
            value = Double.parseDouble(string);
        } catch (Exception e) {
            throw new ValidationException(feedFile, line, e);
        }

        if (value >= min) {
            if (value <= max) {
                return value;
            } else {
                throw new ValidationException(feedFile, line, String.format(
                        "double value out of range (was %f, must be no more than %f)",
                        value, max));
            }
        } else {
            throw new ValidationException(feedFile, line, String.format(
                    "double value out of range (was %f, must be at least %f)",
                    value, min));
        }
    }

    private int stringToInt(String string, int min, int max) {
        int value;

        try {
            value = Integer.parseInt(string);
        } catch (Exception e) {
            throw new ValidationException(feedFile, line, e);
        }

        if (value >= min) {
            if (value <= max) {
                return value;
            } else {
                throw new ValidationException(feedFile, line, String.format(
                        "integer value out of range (was %d, must be no more than %d)",
                        value, max));
            }
        } else {
            throw new ValidationException(feedFile, line, String.format(
                    "integer value out of range (was %d, must be at least %d)",
                    value, min));
        }
    }

    private int stringHexToInt(String string, int min, int max) {
        int value;

        try {
            value = Integer.parseInt(string, 16);
        } catch (Exception e) {
            throw new ValidationException(feedFile, line, e);
        }

        if (value >= min) {
            if (value <= max) {
                return value;
            } else {
                throw new ValidationException(feedFile, line, String.format(
                        "hexadecimal integer value out of range (was %x, must be no more than %x)",
                        value, max));
            }
        } else {
            throw new ValidationException(feedFile, line, String.format(
                    "hexadecimal integer value out of range (was %d, must be at least %d)",
                    value, min));
        }
    }

    private Boolean stringBinToBool(String string) {
        switch (string) {
            case "0":
                return FALSE;
            case "1":
                return TRUE;
            default:
                throw new ValidationException(feedFile, line, String.format(
                        "binary integer value out of range (was %s, must be 0 or 1)", string));
        }
    }

    private int stringToTimeOfDay(String string) {
        final int hours, minutes, seconds;
        String[] fields = string.split(":");

        if (string.equals("")) {
            return Integer.MIN_VALUE;
        } else if (fields.length != 3) {
            throw new ValidationException(feedFile, line, String.format(
                    "wrong number of subfields in time (was %d, expected 3)",
                    fields.length));
        }

        try {
            hours = Integer.parseInt(fields[0]);
            minutes = Integer.parseInt(fields[1]);
            seconds = Integer.parseInt(fields[2]);
        } catch (Exception e) {
            throw new ValidationException(feedFile, line, e);
        }

        if (seconds < 0) throw new ValidationException(feedFile, line, String.format(
                "seconds value out of range (was %d, must be at least 0)", seconds));
        if (seconds > 59) throw new ValidationException(feedFile, line, String.format(
                "seconds value out of range (was %d, must be no more than 59)", seconds));

        if (minutes < 0) throw new ValidationException(feedFile, line, String.format(
                "minutes value out of range (was %d, must be at least 0)", minutes));
        if (minutes > 59) throw new ValidationException(feedFile, line, String.format(
                "minutes value out of range (was %d, must be no more than 59)", minutes));

        if (hours < 0) throw new ValidationException(feedFile, line, String.format(
                "hours value out of range (was %d, must be at least 0)", hours));
        // According to the General Transit Feed Specification Reference hours can legally exceed 23

        return (hours * 60 * 60) + minutes * 60 + seconds;
    }

    private LocalDate stringToDate(String string) {
        final int year, month, day;

        if (string.length() == 8) {
            year = stringToInt(string.substring(0, 4), 0, 9999);
            month = stringToInt(string.substring(4, 6), 0, 99);
            day = stringToInt(string.substring(6, 8), 0, 99);
        } else {
            throw new ValidationException(feedFile, line, String.format(
                    "date value does not have the right length (was %d long, must be 8 (YYYYMMHH))",
                    string.length()));
        }

        try {
            return new LocalDate(year, month, day);
        } catch (Exception e) {
            throw new ValidationException(feedFile, line, e);
        }
    }

    private Currency stringToCurrency(String string) {
        try {
            return Currency.getInstance(string);
        } catch (Exception e) {
            throw new ValidationException(feedFile, line, e);
        }
    }
}
