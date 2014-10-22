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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;
import org.mapdb.Fun.Tuple2;
import org.opentripplanner.gtfs.format.Feed;
import org.opentripplanner.gtfs.format.FeedFile;
import org.opentripplanner.gtfs.validator.ValidationException;
import org.opentripplanner.gtfs.validator.feed.FeedValidator;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.opentripplanner.common.LoggingUtil.human;
import static org.opentripplanner.gtfs.format.FeedFile.AGENCY;
import static org.opentripplanner.gtfs.format.FeedFile.CALENDAR;
import static org.opentripplanner.gtfs.format.FeedFile.CALENDAR_DATES;
import static org.opentripplanner.gtfs.format.FeedFile.FARE_ATTRIBUTES;
import static org.opentripplanner.gtfs.format.FeedFile.FARE_RULES;
import static org.opentripplanner.gtfs.format.FeedFile.FREQUENCIES;
import static org.opentripplanner.gtfs.format.FeedFile.ROUTES;
import static org.opentripplanner.gtfs.format.FeedFile.SHAPES;
import static org.opentripplanner.gtfs.format.FeedFile.STOPS;
import static org.opentripplanner.gtfs.format.FeedFile.STOP_TIMES;
import static org.opentripplanner.gtfs.format.FeedFile.TRANSFERS;
import static org.opentripplanner.gtfs.format.FeedFile.TRIPS;

/**
 * All entities must be from a single feed namespace.
 */
public class GTFSFeed {
    private static final Logger LOG = LoggerFactory.getLogger(GTFSFeed.class);

    private final DB db = DBMaker.newTempFileDB()
            .transactionDisable()
            .asyncWriteEnable()
            .compressionEnable()
            .make();

    // Map from 2-tuples of (trip_id, stop_sequence) to stop_times.
    private final BTreeMap<Tuple2, StopTime> stop_times_db = db.getTreeMap("stop_times");
    // Map from 2-tuples of (service_id, date) to calendar_dates.
    private final Map<Tuple2, CalendarDate> calendar_dates_db = db.getTreeMap("calendar_dates");
    // Map from 2-tuples of (shape_id, shape_pt_sequence) to shapes.
    private final Map<Tuple2, Shape> shapes_db = db.getTreeMap("shapes");
    // Map from 2-tuples of (from_stop_id, to_stop_id) to transfers.
    private final Map<Tuple2, Transfer> transfers_db = db.getTreeMap("transfers");

    public final Map<String, Agency>        agency;
    public final Map<String, Stop>          stops;
    public final Map<String, Route>         routes;
    public final Map<String, Trip>          trips;
    public final Map<Tuple2, StopTime>      stop_times;
    public final Map<String, Calendar>      calendar;
    public final Map<Tuple2, CalendarDate>  calendar_dates;
    public final Map<String, FareAttribute> fare_attributes;
    public final Map<String, FareRule>      fare_rules;
    public final Map<Tuple2, Shape>         shapes;
    public final Map<String, Frequency>     frequencies;
    public final Map<Tuple2, Transfer>      transfers;
    public final Optional<FeedInfo>         feed_info;
    public final List<ValidationException>  validation_exceptions;

    public GTFSFeed(String file, Deduplicator dedup) {
        final Map<String, Agency>        agencyMap        = Maps.newHashMap();
        final Map<String, Stop>          stopMap          = Maps.newHashMap();
        final Map<String, Route>         routeMap         = Maps.newHashMap();
        final Map<String, Trip>          tripMap          = Maps.newHashMap();
        final Map<String, Calendar>      calendarMap      = Maps.newHashMap();
        final Map<String, FareAttribute> fareAttributeMap = Maps.newHashMap();
        final Map<String, FareRule>      fareRuleMap      = Maps.newHashMap();
        final Map<String, Frequency>     frequencyMap     = Maps.newHashMap();
        final Optional<FeedInfo>         feedInfoOptional                    ;
        final List<ValidationException>  validationExceptionList             ;

        List<ValidationException> unsynchronizedList = Lists.newArrayList();
        validationExceptionList = Collections.synchronizedList(unsynchronizedList);

        LOG.info("Loading GTFS feed");

        try (Feed feed = new Feed(file)) {
            final FeedValidator feedValidator = new FeedValidator(feed,  dedup);

            LOG.info("Loading agency.txt");
            Iterator<Agency> iterator = feedValidator.agency.iterator();
            if (iterator.hasNext()) {
                Agency agency = iterator.next();
                Optional<String> agencyId = agency.agency_id;

                if (agencyId.isPresent()) {
                    put(agencyMap, agencyId.get(), agency, AGENCY);
                } else {
                    put(agencyMap, agency, AGENCY);
                }

                if (iterator.hasNext()) {
                    if (!agencyId.isPresent()) {
                        throw new ValidationException(AGENCY, 2, "multiple agencies, no agency_id");
                    } else {
                        while (iterator.hasNext()) {
                            try {
                                agency = iterator.next();

                                put(agencyMap, agency.agency_id.get(), agency, AGENCY);
                            } catch (ValidationException validationException) {
                                validationExceptionList.add(validationException);
                            }
                        }
                    }
                }
            } else {
                throw new ValidationException(AGENCY, 1, "no agencies were found in the GTFS feed");
            }
            LOG.info("Loaded agency.txt");

            Runnable stopsRunnable = new Runnable() {
                @Override
                public void run() {
                    LOG.info("Loading stops.txt");
                    Iterator<Stop> iterator = feedValidator.stops.iterator();

                    while (iterator.hasNext()) {
                        try {
                            Stop stop = iterator.next();
                            String k = stop.stop_id;

                            put(stopMap, k, stop, STOPS);
                        } catch (ValidationException validationException) {
                            validationExceptionList.add(validationException);
                        }
                    }

                    LOG.info("Loaded stops.txt");
                }
            };

            Runnable routesRunnable = new Runnable() {
                @Override
                public void run() {
                    LOG.info("Loading routes.txt");
                    Iterator<Route> iterator = feedValidator.routes.iterator();

                    while (iterator.hasNext()) {
                        try {
                            Route route = iterator.next();
                            String k = route.route_id;

                            put(routeMap, k, route, ROUTES);
                        } catch (ValidationException validationException) {
                            validationExceptionList.add(validationException);
                        }
                    }

                    LOG.info("Loaded routes.txt");
                }
            };

            Runnable tripsRunnable = new Runnable() {
                @Override
                public void run() {
                    LOG.info("Loading trips.txt");
                    Iterator<Trip> iterator = feedValidator.trips.iterator();

                    while (iterator.hasNext()) {
                        try {
                            Trip trip = iterator.next();
                            String k = trip.trip_id;

                            put(tripMap, k, trip, TRIPS);
                        } catch (ValidationException validationException) {
                            validationExceptionList.add(validationException);
                        }
                    }

                    LOG.info("Loaded trips.txt");
                }
            };

            Runnable stopTimesRunnable = new Runnable() {
                @Override
                public void run() {
                    LOG.info("Loading stop_times.txt");
                    Iterator<StopTime> iterator = feedValidator.stop_times.iterator();

                    while (iterator.hasNext()) {
                        try {
                            StopTime stopTime = iterator.next();
                            Tuple2 k = new Tuple2(stopTime.trip_id, stopTime.stop_sequence);

                            put(stop_times_db, k, stopTime, STOP_TIMES);
                        } catch (ValidationException validationException) {
                            validationExceptionList.add(validationException);
                        }
                    }

                    LOG.info("Loaded stop_times.txt");
                }
            };

            Runnable calendarRunnable = new Runnable() {
                @Override
                public void run() {
                    LOG.info("Loading calendar.txt");

                    if (feedValidator.calendar.isPresent()) {
                        Iterator<Calendar> iterator = feedValidator.calendar.get().iterator();

                        while (iterator.hasNext()) {
                            try {
                                Calendar calendar = iterator.next();
                                String k = calendar.service_id;

                                put(calendarMap, k, calendar, CALENDAR);
                            } catch (ValidationException validationException) {
                                validationExceptionList.add(validationException);
                            }
                        }
                    }

                    LOG.info("Loaded calendar.txt");
                }
            };

            Runnable calendarDatesRunnable = new Runnable() {
                @Override
                public void run() {
                    LOG.info("Loading calendar_dates.txt");

                    if (feedValidator.calendar_dates.isPresent()) {
                        Iterator<CalendarDate> iterator =
                                feedValidator.calendar_dates.get().iterator();

                        while (iterator.hasNext()) {
                            try {
                                CalendarDate calendarDate = iterator.next();
                                Tuple2 k = (new Tuple2(calendarDate.service_id, calendarDate.date));

                                put(calendar_dates_db, k, calendarDate, CALENDAR_DATES);
                            } catch (ValidationException validationException) {
                                validationExceptionList.add(validationException);
                            }
                        }
                    }

                    LOG.info("Loaded calendar_dates.txt");
                }
            };

            Runnable fareAttributesRunnable = new Runnable() {
                @Override
                public void run() {
                    LOG.info("Loading fare_attributes.txt");

                    if (feedValidator.fare_attributes.isPresent()) {
                        Iterator<FareAttribute> iterator =
                                feedValidator.fare_attributes.get().iterator();

                        while (iterator.hasNext()) {
                            try {
                                FareAttribute fareAttribute = iterator.next();
                                String k = fareAttribute.fare_id;

                                put(fareAttributeMap, k, fareAttribute, FARE_ATTRIBUTES);
                            } catch (ValidationException validationException) {
                                validationExceptionList.add(validationException);
                            }
                        }
                    }

                    LOG.info("Loaded fare_attributes.txt");
                }
            };

            Runnable fareRulesRunnable = new Runnable() {
                @Override
                public void run() {
                    LOG.info("Loading fare_rules.txt");

                    if (feedValidator.fare_rules.isPresent()) {
                        Iterator<FareRule> iterator = feedValidator.fare_rules.get().iterator();

                        while (iterator.hasNext()) {
                            try {
                                FareRule fareRule = iterator.next();
                                String k = fareRule.fare_id;

                                put(fareRuleMap, k, fareRule, FARE_RULES);
                            } catch (ValidationException validationException) {
                                validationExceptionList.add(validationException);
                            }
                        }
                    }

                    LOG.info("Loaded fare_rules.txt");
                }
            };

            Runnable shapesRunnable = new Runnable() {
                @Override
                public void run() {
                    LOG.info("Loading shapes.txt");

                    if (feedValidator.shapes.isPresent()) {
                        Iterator<Shape> iterator = feedValidator.shapes.get().iterator();

                        while (iterator.hasNext()) {
                            try {
                                Shape shape = iterator.next();
                                Tuple2 k = (new Tuple2(shape.shape_id, shape.shape_pt_sequence));

                                put(shapes_db, k, shape, SHAPES);
                            } catch (ValidationException validationException) {
                                validationExceptionList.add(validationException);
                            }
                        }
                    }

                    LOG.info("Loaded shapes.txt");
                }
            };

            Runnable frequenciesRunnable = new Runnable() {
                @Override
                public void run() {
                    LOG.info("Loading frequencies.txt");

                    if (feedValidator.frequencies.isPresent()) {
                        Iterator<Frequency> iterator = feedValidator.frequencies.get().iterator();

                        while (iterator.hasNext()) {
                            try {
                                Frequency frequency = iterator.next();
                                String k = frequency.trip_id;

                                put(frequencyMap, k, frequency, FREQUENCIES);
                            } catch (ValidationException validationException) {
                                validationExceptionList.add(validationException);
                            }
                        }
                    }

                    LOG.info("Loaded frequencies.txt");
                }
            };

            Runnable transfersRunnable = new Runnable() {
                @Override
                public void run() {
                    LOG.info("Loading transfers.txt");

                    if (feedValidator.transfers.isPresent()) {
                        Iterator<Transfer> iterator = feedValidator.transfers.get().iterator();

                        while (iterator.hasNext()) {
                            try {
                                Transfer transfer = iterator.next();
                                Tuple2 k = (new Tuple2(transfer.from_stop_id, transfer.to_stop_id));

                                put(transfers_db, k, transfer, TRANSFERS);
                            } catch (ValidationException validationException) {
                                validationExceptionList.add(validationException);
                            }
                        }
                    }

                    LOG.info("Loaded transfers.txt");
                }
            };

            LOG.info("Starting executor services for asynchronous table loads");
            List<Future<?>> futures = Lists.newArrayListWithCapacity(11);
            ExecutorService mainExecutorService = Executors.newSingleThreadExecutor();
            ExecutorService stopTimesExecutorService = Executors.newSingleThreadExecutor();

            futures.add(mainExecutorService.submit(stopsRunnable));
            futures.add(mainExecutorService.submit(routesRunnable));
            futures.add(mainExecutorService.submit(tripsRunnable));
            futures.add(stopTimesExecutorService.submit(stopTimesRunnable));
            futures.add(mainExecutorService.submit(calendarRunnable));
            futures.add(mainExecutorService.submit(calendarDatesRunnable));
            futures.add(mainExecutorService.submit(fareAttributesRunnable));
            futures.add(mainExecutorService.submit(fareRulesRunnable));
            futures.add(mainExecutorService.submit(shapesRunnable));
            futures.add(mainExecutorService.submit(frequenciesRunnable));
            futures.add(mainExecutorService.submit(transfersRunnable));
            stopTimesExecutorService.shutdown();
            mainExecutorService.shutdown();

            for (Future<?> future : futures) {
                try {
                    Object o = future.get();

                    if (o != null) {
                        throw new IllegalStateException("A Runnable returned non-null result " + o);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException("Execution was interrupted", e);
                } catch (ExecutionException e) {
                    throw new RuntimeException("Unexpected exception occurred during execution", e);
                }
            }
            LOG.info("Executor services have finished");

            LOG.info("Loading feed_info.txt");
            feedInfoOptional = feedValidator.feed_info.isPresent() ?
                    Optional.of(Iterables.getOnlyElement(feedValidator.feed_info.get())) :
                    Optional.<FeedInfo>absent();
            LOG.info("Loaded feed_info.txt");
        } catch (RuntimeException e) {
            LOG.error("Error loading GTFS: {}", e.getMessage());
            throw e;
        }

        LOG.info("Loaded GTFS feed");

        validation_exceptions = Collections.unmodifiableList(unsynchronizedList);
        feed_info = feedInfoOptional;
        transfers = Collections.unmodifiableMap(transfers_db);
        frequencies = Collections.unmodifiableMap(frequencyMap);
        shapes = Collections.unmodifiableMap(shapes_db);
        fare_rules = Collections.unmodifiableMap(fareRuleMap);
        fare_attributes = Collections.unmodifiableMap(fareAttributeMap);
        calendar_dates = Collections.unmodifiableMap(calendar_dates_db);
        calendar = Collections.unmodifiableMap(calendarMap);
        stop_times = Collections.unmodifiableMap(stop_times_db);
        trips = Collections.unmodifiableMap(tripMap);
        routes = Collections.unmodifiableMap(routeMap);
        stops = Collections.unmodifiableMap(stopMap);
        agency = Collections.unmodifiableMap(agencyMap);
    }

    // Bin all trips by the sequence of stops they visit.
    public Set<Entry<List<String>, List<String>>> findPatterns() {
        if (validation_exceptions.isEmpty()) {
            LOG.info("GTFS feed was imported without errors");
        } else {
            LOG.error("GTFS feed is invalid, attempting to run anyway");

            for (ValidationException validationException : validation_exceptions) {
                LOG.error("This error occurred while importing the GTFS feed", validationException);
            }
        }
        LOG.info("Finding trip patterns");

        // A map from a list of stop IDs (the pattern) to a list of trip IDs which fit that pattern.
        Map<List<String>, List<String>> tripsForPattern = Maps.newHashMap();
        int n = 0;
        for (String trip_id : trips.keySet()) {
            if (++n % 100000 == 0) {
                LOG.info("trip {}", human(n));
            }
            Map<Fun.Tuple2, StopTime> tripStopTimes =
                (stop_times_db).subMap(
                    Fun.t2(trip_id, null),
                    Fun.t2(trip_id, Fun.HI)
                );
            List<String> stops = Lists.newArrayList();
            // In-order traversal of StopTimes within this trip. The 2-tuple keys determine ordering.
            for (StopTime stopTime : tripStopTimes.values()) {
                stops.add(stopTime.stop_id);
            }
            // Fetch or create the tripId list for this stop pattern, then add the current trip to that list.
            List<String> trips = tripsForPattern.get(stops);
            if (trips == null) {
                trips = Lists.newArrayList();
                tripsForPattern.put(stops, trips);
            }
            trips.add(trip_id);
        }
        LOG.info("Total routes: {}", routes.size());
        LOG.info("Total trip patterns: {}", tripsForPattern.size());
        LOG.info("Total trips: {}", trips.size());

        return tripsForPattern.entrySet();
    }

    public void closeDb() {
        db.close();
    }

    private static void put(Map map, Tuple2 key, Object value, FeedFile feedFile) {
        if (map.put(key, value) != null) {
            String message = String.format("multiple occurrences of tuple key %s,%s", key.a, key.b);
            throw new ValidationException(feedFile, 0, message);
        }
    }

    private static void put(Map map, String key, Object value, FeedFile feedFile) {
        if (map.put(key, value) != null) {
            throw new ValidationException(feedFile, 0, "multiple occurrences of ID " + key);
        }
    }

    private static void put(Map map, Object value, FeedFile feedFile) {
        if (map.put(null, value) != null) {
            throw new ValidationException(feedFile, 0, "SEVERE: multiple occurrences of null key!");
        }
    }
}
