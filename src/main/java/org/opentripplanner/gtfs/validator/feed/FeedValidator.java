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
import org.opentripplanner.gtfs.validator.table.CalendarDateValidator;
import org.opentripplanner.gtfs.validator.table.CalendarValidator;
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
import org.opentripplanner.routing.trippattern.Deduplicator;

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
    final private Deduplicator deduplicator;

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

    public FeedValidator(Feed feed, Deduplicator dedup) {
        deduplicator = dedup;

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
            throw new ValidationException(CALENDAR, 0, "omitted, which requires " + CALENDAR_DATES);
        }
    }

    private static Iterable<Map<String, String>> required(Feed feed, FeedFile feedFile) {
        Iterable<Map<String, String>> iterable = feed.get(feedFile.toString());

        if (iterable == null) {
            throw new ValidationException(feedFile, 0, "omitted, while GTFS requires its presence");
        } else {
            return iterable;
        }
    }

    private Optional<Iterable<FeedInfo>> feed_info(Feed feed) {
        Iterable<Map<String, String>> iterable = feed.get(FEED_INFO.toString());

        if (iterable == null) {
            return Optional.absent();
        } else {
            return Optional.<Iterable<FeedInfo>>of(new FeedInfoValidator(iterable, deduplicator));
        }
    }

    private Optional<Iterable<Transfer>> transfers(Feed feed) {
        Iterable<Map<String, String>> iterable = feed.get(TRANSFERS.toString());

        if (iterable == null) {
            return Optional.absent();
        } else {
            return Optional.<Iterable<Transfer>>of(new TransferValidator(iterable, deduplicator));
        }
    }

    private Optional<Iterable<Frequency>> frequencies(Feed feed) {
        Iterable<Map<String, String>> iterable = feed.get(FREQUENCIES.toString());

        if (iterable == null) {
            return Optional.absent();
        } else {
            return Optional.<Iterable<Frequency>>of(new FrequencyValidator(iterable, deduplicator));
        }
    }

    private Optional<Iterable<Shape>> shapes(Feed feed) {
        Iterable<Map<String, String>> iterable = feed.get(SHAPES.toString());

        if (iterable == null) {
            return Optional.absent();
        } else {
            return Optional.<Iterable<Shape>>of(new ShapeValidator(iterable, deduplicator));
        }
    }

    private Optional<Iterable<FareRule>> fare_rules(Feed feed) {
        Iterable<Map<String, String>> iterable = feed.get(FARE_RULES.toString());

        if (iterable == null) {
            return Optional.absent();
        } else {
            return Optional.<Iterable<FareRule>>of(new FareRuleValidator(iterable, deduplicator));
        }
    }

    private Optional<Iterable<FareAttribute>> fare_attributes(Feed feed) {
        Iterable<Map<String, String>> iterable = feed.get(FARE_ATTRIBUTES.toString());

        if (iterable == null) {
            return Optional.absent();
        } else {
            return Optional.<Iterable<FareAttribute>>of(new FareAttributeValidator(iterable, deduplicator));
        }
    }

    private Optional<Iterable<CalendarDate>> calendar_dates(Feed feed) {
        Iterable<Map<String, String>> iterable = feed.get(CALENDAR_DATES.toString());

        if (iterable == null) {
            return Optional.absent();
        } else {
            return Optional.<Iterable<CalendarDate>>of(new CalendarDateValidator(iterable, deduplicator));
        }
    }

    private Optional<Iterable<Calendar>> calendar(Feed feed) {
        Iterable<Map<String, String>> iterable = feed.get(CALENDAR.toString());

        if (iterable == null) {
            return Optional.absent();
        } else {
            return Optional.<Iterable<Calendar>>of(new CalendarValidator(iterable, deduplicator));
        }
    }

    private Iterable<StopTime> stop_times(Feed feed) {
        return new StopTimeValidator(required(feed, STOP_TIMES), deduplicator);
    }

    private Iterable<Trip> trips(Feed feed) {
        return new TripValidator(required(feed, TRIPS), deduplicator);
    }

    private Iterable<Route> routes(Feed feed) {
        return new RouteValidator(required(feed, ROUTES), deduplicator);
    }

    private Iterable<Stop> stops(Feed feed) {
        return new StopValidator(required(feed, STOPS), deduplicator);
    }

    private Iterable<Agency> agency(Feed feed) {
        return new AgencyValidator(required(feed, AGENCY), deduplicator);
    }
}
