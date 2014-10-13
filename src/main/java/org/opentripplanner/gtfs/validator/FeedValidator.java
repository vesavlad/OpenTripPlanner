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

package org.opentripplanner.gtfs.validator;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import org.opentripplanner.gtfs.format.Feed;
import org.opentripplanner.gtfs.format.FeedFile;

import java.util.Map;

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
    final public Iterable<Map<String, String>> agency;
    final public Iterable<Map<String, String>> stops;
    final public Iterable<Map<String, String>> routes;
    final public Iterable<Map<String, String>> trips;
    final public Iterable<Map<String, String>> stop_times;
    final public Optional<Iterable<Map<String, String>>> calendar;
    final public Optional<Iterable<Map<String, String>>> calendar_dates;
    final public Optional<Iterable<Map<String, String>>> fare_attributes;
    final public Optional<Iterable<Map<String, String>>> fare_rules;
    final public Optional<Iterable<Map<String, String>>> shapes;
    final public Optional<Iterable<Map<String, String>>> frequencies;
    final public Optional<Iterable<Map<String, String>>> transfers;
    final public Optional<Iterable<Map<String, String>>> feed_info;

    public FeedValidator(Feed feed) {
        agency = required(feed, AGENCY);
        stops = required(feed, STOPS);
        routes = required(feed, ROUTES);
        trips = required(feed, TRIPS);
        stop_times = required(feed, STOP_TIMES);
        calendar = optional(feed, CALENDAR);
        calendar_dates = optional(feed, CALENDAR_DATES);
        fare_attributes = optional(feed, FARE_ATTRIBUTES);
        fare_rules = optional(feed, FARE_RULES);
        shapes = optional(feed, SHAPES);
        frequencies = optional(feed, FREQUENCIES);
        transfers = optional(feed, TRANSFERS);
        feed_info = optional(feed, FEED_INFO);

        if (!calendar.isPresent() && !calendar_dates.isPresent()) {
            throw new ValidationException(CALENDAR, "omitted, but that requires " + CALENDAR_DATES);
        }
    }

    private static Iterable<Map<String, String>> required(Feed feed, FeedFile feedFile) {
        Iterable<Map<String, String>> iterable = feed.get(feedFile.toString());

        if (iterable == null) {
            throw new ValidationException(feedFile, "required feed file not found");
        } else {
            return addValidator(feedFile, iterable);
        }
    }

    private static Optional<Iterable<Map<String, String>>> optional(Feed feed, FeedFile feedFile) {
        Iterable<Map<String, String>> iterable = feed.get(feedFile.toString());

        if (iterable == null) {
            return Optional.absent();
        } else {
            return Optional.of(addValidator(feedFile, iterable));
        }
    }

    private static Iterable<Map<String, String>> addValidator(final FeedFile feedFile,
                                                              Iterable<Map<String, String>> input) {
        return Iterables.transform(input, new Function<Map<String, String>, Map<String, String>>() {
            private int line = 1;

            @Override
            public Map<String, String> apply(Map<String, String> input) {
                line++;

                if (input.keySet().size() != input.values().size()) {
                    throw new ValidationException(feedFile, String.format(
                            "incorrect column count (expected %d cells, found %d cells) on line %d",
                            input.keySet().size(), input.values().size(), line));
                } else {
                    return input;
                }
            }
        });
    }
}
