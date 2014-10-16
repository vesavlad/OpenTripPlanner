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

package org.opentripplanner.gtfs.validator.feed;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import org.joda.time.LocalDate;
import org.opentripplanner.gtfs.format.Feed;
import org.opentripplanner.gtfs.format.FeedFile;
import org.opentripplanner.gtfs.model.Agency;
import org.opentripplanner.gtfs.model.Calendar;
import org.opentripplanner.gtfs.model.CalendarDate;
import org.opentripplanner.gtfs.model.FareAttribute;
import org.opentripplanner.gtfs.model.FareRule;
import org.opentripplanner.gtfs.model.FeedInfo;
import org.opentripplanner.gtfs.model.Frequency;
import org.opentripplanner.gtfs.model.Route;
import org.opentripplanner.gtfs.model.Shape;
import org.opentripplanner.gtfs.model.Stop;
import org.opentripplanner.gtfs.model.StopTime;
import org.opentripplanner.gtfs.model.Transfer;
import org.opentripplanner.gtfs.model.Trip;
import org.opentripplanner.gtfs.validator.ValidationException;
import org.opentripplanner.gtfs.validator.table.AgencyValidator;
import org.opentripplanner.gtfs.validator.table.CalendarValidator;
import org.opentripplanner.gtfs.validator.table.CalendarDateValidator;
import org.opentripplanner.gtfs.validator.table.FareAttributeValidator;
import org.opentripplanner.gtfs.validator.table.FareRuleValidator;
import org.opentripplanner.gtfs.validator.table.FeedInfoValidator;
import org.opentripplanner.gtfs.validator.table.FrequencyValidator;
import org.opentripplanner.gtfs.validator.table.RouteValidator;
import org.opentripplanner.gtfs.validator.table.ShapeValidator;
import org.opentripplanner.gtfs.validator.table.StopTimeValidator;
import org.opentripplanner.gtfs.validator.table.StopValidator;
import org.opentripplanner.gtfs.validator.table.TransferValidator;
import org.opentripplanner.gtfs.validator.table.TripValidator;

import java.net.URL;
import java.util.Currency;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.opentripplanner.gtfs.format.FeedFile.AGENCY;
import static org.opentripplanner.gtfs.format.FeedFile.CALENDAR;
import static org.opentripplanner.gtfs.format.FeedFile.CALENDAR_DATES;
import static org.opentripplanner.gtfs.format.FeedFile.FARE_ATTRIBUTES;
import static org.opentripplanner.gtfs.format.FeedFile.FARE_RULES;
import static org.opentripplanner.gtfs.format.FeedFile.FEED_INFO;
import static org.opentripplanner.gtfs.format.FeedFile.FREQUENCIES;
import static org.opentripplanner.gtfs.format.FeedFile.ROUTES;
import static org.opentripplanner.gtfs.format.FeedFile.SHAPES;
import static org.opentripplanner.gtfs.format.FeedFile.STOPS;
import static org.opentripplanner.gtfs.format.FeedFile.STOP_TIMES;
import static org.opentripplanner.gtfs.format.FeedFile.TRANSFERS;
import static org.opentripplanner.gtfs.format.FeedFile.TRIPS;

public class FeedValidator {
    final public Iterable<Agency> agency;
    final public Iterable<Stop> stops;
    final public Iterable<Route> routes;
    final public Iterable<Trip> trips;
    final public Iterable<StopTime> stop_times;
    final public Optional<Iterable<Calendar>> calendar;
    final public Optional<Iterable<CalendarDate>> calendar_dates;
    final public Optional<Iterable<FareAttribute>> fare_attributes;
    final public Optional<Iterable<FareRule>> fare_rules;
    final public Optional<Iterable<Shape>> shapes;
    final public Optional<Iterable<Frequency>> frequencies;
    final public Optional<Iterable<Transfer>> transfers;
    final public Optional<Iterable<FeedInfo>> feed_info;

    public FeedValidator(Feed feed) {
        agency = agency(feed);
        stops = stops(feed);
        routes = routes(feed);
        trips = trips(feed);
        stop_times = stop_times(feed);
        calendar = calendar(feed);
        calendar_dates = calendar_dates(feed);
        fare_attributes = fare_attributes(feed);
        fare_rules = fare_rules(feed);
        shapes = shapes(feed);
        frequencies = frequencies(feed);
        transfers = transfers(feed);
        feed_info = feed_info(feed);

        if (!calendar.isPresent() && !calendar_dates.isPresent()) {
            throw new ValidationException(CALENDAR, "omitted, but that requires " + CALENDAR_DATES);
        }
    }

    public static String requiredString(Map<String, String> row, String column, FeedFile feedFile) {
        String string = row.get(column);

        if (string == null) {
            throw new ValidationException(feedFile, "required column " + column + " not found");
        } else {
            return string;
        }
    }

    public static Optional<String> optionalString(
            Map<String, String> row, String column, FeedFile feedFile) {
        String string = row.get(column);

        if (string == null) {
            return Optional.absent();
        } else {
            return Optional.of(string);
        }
    }

    public static URL requiredUrl(Map<String, String> row, String column, FeedFile feedFile) {
        return stringToUrl(requiredString(row, column, feedFile), feedFile);
    }

    public static Optional<URL> optionalUrl(
            Map<String, String> row, String column, FeedFile feedFile) {
        String string = row.get(column);

        if (Strings.isNullOrEmpty(string)) {
            return Optional.absent();
        } else {
            return Optional.of(stringToUrl(string, feedFile));
        }
    }

    public static TimeZone requiredTz(Map<String, String> row, String column, FeedFile feedFile) {
        return stringToTz(requiredString(row, column, feedFile), feedFile);
    }

    public static Optional<TimeZone> optionalTz(
            Map<String, String> row, String column, FeedFile feedFile) {
        String string = row.get(column);

        if (Strings.isNullOrEmpty(string)) {
            return Optional.absent();
        } else {
            return Optional.of(stringToTz(string, feedFile));
        }
    }

    public static Locale requiredLang(Map<String, String> row, String column, FeedFile feedFile) {
        return stringToLang(requiredString(row, column, feedFile), feedFile);
    }

    public static Optional<Locale> optionalLang(
            Map<String, String> row, String column, FeedFile feedFile) {
        String string = row.get(column);

        if (string == null) {
            return Optional.absent();
        } else {
            return Optional.of(stringToLang(string, feedFile));
        }
    }

    public static double requiredDouble(
            Map<String, String> row, String column, double min, double max, FeedFile feedFile) {
        return stringToDouble(requiredString(row, column, feedFile), min, max, feedFile);
    }

    public static Optional<Double> optionalDouble(
            Map<String, String> row, String column, FeedFile feedFile) {
        String string = row.get(column);

        if (Strings.isNullOrEmpty(string)) {
            return Optional.absent();
        } else {
            return Optional.of(stringToDouble(string, 0, Double.MAX_VALUE, feedFile));
        }
    }

    public static int requiredInt(
            Map<String, String> row, String column, int min, int max, FeedFile feedFile) {
        return stringToInt(requiredString(row, column, feedFile), min, max, feedFile);
    }

    public static Optional<Integer> requiredIntOptionalValue(
            Map<String, String> row, String column, int min, int max, FeedFile feedFile) {
        String string = requiredString(row, column, feedFile);

        if (string.equals("")) {
            return Optional.absent();
        } else {
            return Optional.of(stringToInt(string, min, max, feedFile));
        }
    }

    public static Optional<Integer> optionalInt(
            Map<String, String> row, String column, int min, int max, FeedFile feedFile) {
        String string = row.get(column);

        if (Strings.isNullOrEmpty(string)) {
            return Optional.absent();
        } else {
            return Optional.of(stringToInt(string, min, max, feedFile));
        }
    }

    public static int optionalColor(
            Map<String, String> row, String column, int defaultColor, FeedFile feedFile) {
        String string = row.get(column);

        if (Strings.isNullOrEmpty(string)) {
            return defaultColor;
        } else {
            return stringHexToInt(string, 0X000000, 0XFFFFFF, feedFile);
        }
    }

    public static boolean requiredBool(Map<String, String> row, String column, FeedFile feedFile) {
        return stringBinToBool(requiredString(row, column, feedFile), feedFile);
    }

    public static Optional<Boolean> optionalBool(
            Map<String, String> row, String column, FeedFile feedFile) {
        String string = row.get(column);

        if (string == null) {
            return Optional.absent();
        } else {
            return Optional.of(stringBinToBool(string, feedFile));
        }
    }

    public static int requiredTimeOfDay(Map<String, String> row, String column, FeedFile feedFile) {
        return stringToTimeOfDay(requiredString(row, column, feedFile), feedFile);
    }

    public static LocalDate requiredDate(
            Map<String, String> row, String column, FeedFile feedFile) {
        return stringToDate(requiredString(row, column, feedFile), feedFile);
    }

    public static Optional<LocalDate> optionalDate(
            Map<String, String> row, String column, FeedFile feedFile) {
        String string = row.get(column);

        if (string == null) {
            return Optional.absent();
        } else {
            return Optional.of(stringToDate(string, feedFile));
        }
    }

    public static Currency requiredCurrency(
            Map<String, String> row, String column, FeedFile feedFile) {
        return stringToCurrency(requiredString(row, column, feedFile), feedFile);
    }

    private static URL stringToUrl(String string, FeedFile feedFile) {
        try {
            return new URL(string);
        } catch (Exception e) {
            throw new ValidationException(feedFile, e);
        }
    }

    private static TimeZone stringToTz(String string, FeedFile feedFile) {
        try {
            return TimeZone.getTimeZone(string);
        } catch (Exception e) {
            throw new ValidationException(feedFile, e);
        }
    }

    private static Locale stringToLang(String string, FeedFile feedFile) {
        try {
            return new Locale(string);
        } catch (Exception e) {
            throw new ValidationException(feedFile, e);
        }
    }

    private static double stringToDouble(String string, double min, double max, FeedFile feedFile) {
        double value;

        try {
            value = Double.parseDouble(string);
        } catch (Exception e) {
            throw new ValidationException(feedFile, e);
        }

        if (value >= min) {
            if (value <= max) {
                return value;
            } else {
                throw new ValidationException(feedFile, String.format(
                        "double value out of range (was %f, must be no more than %f)",
                        value, max));
            }
        } else {
            throw new ValidationException(feedFile, String.format(
                    "double value out of range (was %f, must be at least %f)",
                    value, min));
        }
    }

    private static int stringToInt(String string, int min, int max, FeedFile feedFile) {
        int value;

        try {
            value = Integer.parseInt(string);
        } catch (Exception e) {
            throw new ValidationException(feedFile, e);
        }

        if (value >= min) {
            if (value <= max) {
                return value;
            } else {
                throw new ValidationException(feedFile, String.format(
                        "integer value out of range (was %d, must be no more than %d)",
                        value, max));
            }
        } else {
            throw new ValidationException(feedFile, String.format(
                    "integer value out of range (was %d, must be at least %d)",
                    value, min));
        }
    }

    private static int stringHexToInt(String string, int min, int max, FeedFile feedFile) {
        int value;

        try {
            value = Integer.parseInt(string, 16);
        } catch (Exception e) {
            throw new ValidationException(feedFile, e);
        }

        if (value >= min) {
            if (value <= max) {
                return value;
            } else {
                throw new ValidationException(feedFile, String.format(
                        "hexadecimal integer value out of range (was %x, must be no more than %x)",
                        value, max));
            }
        } else {
            throw new ValidationException(feedFile, String.format(
                    "hexadecimal integer value out of range (was %d, must be at least %d)",
                    value, min));
        }
    }

    private static Boolean stringBinToBool(String string, FeedFile feedFile) {
        if ("0".equals(string)) return FALSE;
        else if ("1".equals(string)) return TRUE;
        else throw new ValidationException(feedFile, String.format(
                "binary integer value out of range (was %s, must be 0 or 1)", string));
    }

    private static int stringToTimeOfDay(String string, FeedFile feedFile) {
        final int hours, minutes, seconds;
        String[] fields = string.split(":");

        if (string.equals("")) {
            return Integer.MIN_VALUE;
        } else if (fields.length != 3) {
            throw new ValidationException(feedFile, String.format(
                    "wrong number of subfields in time (was %d, expected 3)",
                    fields.length));
        }

        try {
            hours = Integer.parseInt(fields[0]);
            minutes = Integer.parseInt(fields[1]);
            seconds = Integer.parseInt(fields[2]);
        } catch (Exception e) {
            throw new ValidationException(feedFile, e);
        }

        if (seconds < 0) throw new ValidationException(feedFile, String.format(
                "seconds value out of range (was %d, must be at least 0)", seconds));
        if (seconds > 59) throw new ValidationException(feedFile, String.format(
                "seconds value out of range (was %d, must be no more than 59)", seconds));

        if (minutes < 0) throw new ValidationException(feedFile, String.format(
                "minutes value out of range (was %d, must be at least 0)", minutes));
        if (minutes > 59) throw new ValidationException(feedFile, String.format(
                "minutes value out of range (was %d, must be no more than 59)", minutes));

        if (hours < 0) throw new ValidationException(feedFile, String.format(
                "hours value out of range (was %d, must be at least 0)", hours));
        // According to the General Transit Feed Specification Reference hours can legally exceed 23

        return (hours * 60 * 60) + minutes * 60 + seconds;
    }

    private static LocalDate stringToDate(String string, FeedFile feedFile) {
        final int year, month, day;

        if (string.length() == 8) {
            year = stringToInt(string.substring(0, 4), 0, 9999, feedFile);
            month = stringToInt(string.substring(4, 6), 0, 99, feedFile);
            day = stringToInt(string.substring(6, 8), 0, 99, feedFile);
        } else {
            throw new ValidationException(feedFile, String.format(
                    "date value does not have the right length (was %d long, must be 8 (YYYYMMHH))",
                    string.length()));
        }

        try {
            return new LocalDate(year, month, day);
        } catch (Exception e) {
            throw new ValidationException(feedFile, e);
        }
    }

    private static Currency stringToCurrency(String string, FeedFile feedFile) {
        try {
            return Currency.getInstance(string);
        } catch (Exception e) {
            throw new ValidationException(feedFile, e);
        }
    }

    private static Iterable<Map<String, String>> required(Feed feed, FeedFile feedFile) {
        Iterable<Map<String, String>> iterable = feed.get(feedFile.toString());

        if (iterable == null) {
            throw new ValidationException(feedFile, "required feed file not found");
        } else {
            return iterable;
        }
    }

    private static Optional<Iterable<FeedInfo>> feed_info(Feed feed) {
        Iterable<Map<String, String>> iterable = feed.get(FEED_INFO.toString());

        if (iterable == null) {
            return Optional.absent();
        } else {
            return Optional.<Iterable<FeedInfo>>of(new FeedInfoValidator(iterable));
        }
    }

    private static Optional<Iterable<Transfer>> transfers(Feed feed) {
        Iterable<Map<String, String>> iterable = feed.get(TRANSFERS.toString());

        if (iterable == null) {
            return Optional.absent();
        } else {
            return Optional.<Iterable<Transfer>>of(new TransferValidator(iterable));
        }
    }

    private static Optional<Iterable<Frequency>> frequencies(Feed feed) {
        Iterable<Map<String, String>> iterable = feed.get(FREQUENCIES.toString());

        if (iterable == null) {
            return Optional.absent();
        } else {
            return Optional.<Iterable<Frequency>>of(new FrequencyValidator(iterable));
        }
    }

    private static Optional<Iterable<Shape>> shapes(Feed feed) {
        Iterable<Map<String, String>> iterable = feed.get(SHAPES.toString());

        if (iterable == null) {
            return Optional.absent();
        } else {
            return Optional.<Iterable<Shape>>of(new ShapeValidator(iterable));
        }
    }

    private static Optional<Iterable<FareRule>> fare_rules(Feed feed) {
        Iterable<Map<String, String>> iterable = feed.get(FARE_RULES.toString());

        if (iterable == null) {
            return Optional.absent();
        } else {
            return Optional.<Iterable<FareRule>>of(new FareRuleValidator(iterable));
        }
    }

    private static Optional<Iterable<FareAttribute>> fare_attributes(Feed feed) {
        Iterable<Map<String, String>> iterable = feed.get(FARE_ATTRIBUTES.toString());

        if (iterable == null) {
            return Optional.absent();
        } else {
            return Optional.<Iterable<FareAttribute>>of(new FareAttributeValidator(iterable));
        }
    }

    private static Optional<Iterable<CalendarDate>> calendar_dates(Feed feed) {
        Iterable<Map<String, String>> iterable = feed.get(CALENDAR_DATES.toString());

        if (iterable == null) {
            return Optional.absent();
        } else {
            return Optional.<Iterable<CalendarDate>>of(new CalendarDateValidator(iterable));
        }
    }

    private static Optional<Iterable<Calendar>> calendar(Feed feed) {
        Iterable<Map<String, String>> iterable = feed.get(CALENDAR.toString());

        if (iterable == null) {
            return Optional.absent();
        } else {
            return Optional.<Iterable<Calendar>>of(new CalendarValidator(iterable));
        }
    }

    private static Iterable<StopTime> stop_times(Feed feed) {
        return new StopTimeValidator(required(feed, STOP_TIMES));
    }

    private static Iterable<Trip> trips(Feed feed) {
        return new TripValidator(required(feed, TRIPS));
    }

    private static Iterable<Route> routes(Feed feed) {
        return new RouteValidator(required(feed, ROUTES));
    }

    private static Iterable<Stop> stops(Feed feed) {
        return new StopValidator(required(feed, STOPS));
    }

    private static Iterable<Agency> agency(Feed feed) {
        return new AgencyValidator(required(feed, AGENCY));
    }
}
