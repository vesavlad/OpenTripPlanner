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

/**
 * All entities must be from a single feed namespace.
 */
public class GTFSFeed {

    private static final Logger LOG = LoggerFactory.getLogger(GTFSFeed.class);

    DB db = DBMaker.newTempFileDB()
            .transactionDisable()
            .asyncWriteEnable()
            .compressionEnable()
            .make(); // db.close();

    public final List<ValidationException> validationExceptions =
            Collections.synchronizedList(Lists.<ValidationException>newArrayList());

    public final Map<String, Agency>        agency          = Maps.newHashMap();
    public final Map<String, Stop>          stops           = Maps.newHashMap();
    public final Map<String, Route>         routes          = Maps.newHashMap();
    public final Map<String, Trip>          trips           = Maps.newHashMap();

    // Map from 2-tuples of (trip_id, stop_sequence) to stoptimes.
    public final Map<Tuple2, StopTime> stop_times = db.getTreeMap("stop_times");

    public final Map<String, Calendar>      calendar        = Maps.newHashMap();
    public final Map<String, CalendarDate>  calendar_dates  = Maps.newHashMap();
    public final Map<String, FareAttribute> fare_attributes = Maps.newHashMap();
    public final Map<String, FareRule>      fare_rules      = Maps.newHashMap();
    public final Map<String, Shape>         shapes          = Maps.newHashMap();
    public final Map<String, Frequency>     frequencies     = Maps.newHashMap();
    public final Map<Tuple2, Transfer>      transfers       = Maps.newHashMap();

    public final Optional<FeedInfo>         feed_info                          ;

    public GTFSFeed(String file, Deduplicator dedup) {
        LOG.info("Loading GTFS feed");

        try (Feed feed = new Feed(file)) {
            final FeedValidator feedValidator = new FeedValidator(feed,  dedup);

            LOG.info("Loading agency.txt");
            Iterator<Agency> iterator = feedValidator.agency.iterator();
            if (iterator.hasNext()) {
                Agency agency = iterator.next();
                Optional<String> agencyId = agency.agency_id;

                if (agencyId.isPresent()) {
                    this.agency.put(agencyId.get(), agency);
                } else {
                    this.agency.put(null, agency);
                }

                if (iterator.hasNext()) {
                    if (!agencyId.isPresent()) {
                        throw new ValidationException(AGENCY, 2, "multiple agencies, no agency_id");
                    } else {
                        while (iterator.hasNext()) {
                            try {
                                agency = iterator.next();

                                this.agency.put(agency.agency_id.get(), agency);
                            } catch (ValidationException validationException) {
                                validationExceptions.add(validationException);
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

                            stops.put(k, stop);
                        } catch (ValidationException validationException) {
                            validationExceptions.add(validationException);
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

                            routes.put(k, route);
                        } catch (ValidationException validationException) {
                            validationExceptions.add(validationException);
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

                            trips.put(k, trip);
                        } catch (ValidationException validationException) {
                            validationExceptions.add(validationException);
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

                            stop_times.put(k, stopTime);
                        } catch (ValidationException validationException) {
                            validationExceptions.add(validationException);
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

                                GTFSFeed.this.calendar.put(k, calendar);
                            } catch (ValidationException validationException) {
                                validationExceptions.add(validationException);
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
                                String k = calendarDate.service_id;

                                calendar_dates.put(k, calendarDate);
                            } catch (ValidationException validationException) {
                                validationExceptions.add(validationException);
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

                                fare_attributes.put(k, fareAttribute);
                            } catch (ValidationException validationException) {
                                validationExceptions.add(validationException);
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

                                fare_rules.put(k, fareRule);
                            } catch (ValidationException validationException) {
                                validationExceptions.add(validationException);
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
                                String k = shape.shape_id;

                                shapes.put(k, shape);
                            } catch (ValidationException validationException) {
                                validationExceptions.add(validationException);
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

                                frequencies.put(k, frequency);
                            } catch (ValidationException validationException) {
                                validationExceptions.add(validationException);
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

                                transfers.put(k, transfer);
                            } catch (ValidationException validationException) {
                                validationExceptions.add(validationException);
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
            feed_info = feedValidator.feed_info.isPresent() ?
                    Optional.of(Iterables.getOnlyElement(feedValidator.feed_info.get())) :
                    Optional.<FeedInfo>absent();
            LOG.info("Loaded feed_info.txt");
        } catch (RuntimeException e) {
            LOG.error("Error loading GTFS: {}", e.getMessage());
            throw e;
        }

        LOG.info("Loaded GTFS feed");
    }

    // Bin all trips by the sequence of stops they visit.
    public Set<Entry<List<String>, List<String>>> findPatterns() {
        if (validationExceptions.isEmpty()) {
            LOG.info("GTFS feed was imported without errors");
        } else {
            LOG.error("GTFS feed is invalid, attempting to run anyway");

            for (ValidationException validationException : validationExceptions) {
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
                ((BTreeMap<Tuple2, StopTime>) stop_times).subMap(
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
        LOG.info("Total patterns: {}", tripsForPattern.keySet().size());

        return tripsForPattern.entrySet();
    }

}
