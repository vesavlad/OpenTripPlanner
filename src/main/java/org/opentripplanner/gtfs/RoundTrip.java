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

package org.opentripplanner.gtfs;

import com.csvreader.CsvWriter;
import com.google.common.base.Strings;
import org.joda.time.LocalDate;
import org.opentripplanner.gtfs.format.Feed;
import org.opentripplanner.gtfs.model.Agency;
import org.opentripplanner.gtfs.model.CalendarDate;
import org.opentripplanner.gtfs.model.FeedInfo;
import org.opentripplanner.gtfs.model.Route;
import org.opentripplanner.gtfs.model.Shape;
import org.opentripplanner.gtfs.model.Stop;
import org.opentripplanner.gtfs.model.StopTime;
import org.opentripplanner.gtfs.model.Transfer;
import org.opentripplanner.gtfs.model.Trip;
import org.opentripplanner.gtfs.validator.feed.FeedValidator;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;

/**
 * This RoundTrip class is tailored to the OVapi GTFS feed (at http://gtfs.ovapi.nl/new/gtfs-nl.zip)
 */
public class RoundTrip {
    final static private Charset UTF8 = Charset.forName("UTF-8");

    public static void main (String[] args) {
        final long time = System.nanoTime();

        if (args.length < 1) {
            System.err.println("Please specify a GTFS feed input file for parsing and extraction.");
            System.exit(1);
        }

        try (Feed feed = new Feed(args[0])) {
            FeedValidator feedValidator = new FeedValidator(feed);

            {
                CsvWriter csvWriter = new CsvWriter("agency.txt", ',', UTF8);
                try {
                    csvWriter.writeRecord(new String[]{"agency_id", "agency_name", "agency_url",
                            "agency_timezone", "agency_phone"});
                    for (Agency agency : feedValidator.agency) {
                        final String fields[] = new String[5];
                        fields[0] = agency.agency_id.get();
                        fields[1] = agency.agency_name;
                        fields[2] = agency.agency_url.toString();
                        fields[3] = agency.agency_timezone.getID();
                        fields[4] = agency.agency_phone.get();
                        csvWriter.writeRecord(fields);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    System.exit(2);
                } finally {
                    csvWriter.close();
                }
            }

            {
                CsvWriter csvWriter = new CsvWriter("stops.txt", ',', UTF8);
                try {
                    csvWriter.writeRecord(new String[]{"stop_id", "stop_code", "stop_name",
                            "stop_lat", "stop_lon", "location_type", "parent_station",
                            "stop_timezone", "wheelchair_boarding", "platform_code", "zone_id"});
                    Iterator<Map<String, String>> iterator = feed.get("stops.txt").iterator();
                    for (Stop stop : feedValidator.stops) {
                        Map<String, String> row = iterator.next();
                        final String fields[] = new String[11];
                        fields[0] = stop.stop_id;
                        fields[1] = stop.stop_code.get();
                        fields[2] = stop.stop_name;
                        fields[3] = String.valueOf(stop.stop_lat);
                        fields[4] = String.valueOf(stop.stop_lon);
                        fields[5] = stop.location_type.isPresent() ?
                                    String.valueOf(stop.location_type.get()) : "";
                        fields[6] = stop.parent_station.get();
                        fields[7] = stop.stop_timezone.isPresent() ?
                                    stop.stop_timezone.get().getID() : "";
                        fields[8] = stop.wheelchair_boarding.isPresent() ?
                                    String.valueOf(stop.wheelchair_boarding.get()) : "";
                        fields[9] = row.get("platform_code");
                        fields[10] = stop.zone_id.get();
                        csvWriter.writeRecord(fields);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    System.exit(2);
                } finally {
                    csvWriter.close();
                }
            }

            {
                CsvWriter csvWriter = new CsvWriter("routes.txt", ',', UTF8);
                try {
                    csvWriter.writeRecord(new String[]{"route_id", "agency_id", "route_short_name",
                            "route_long_name", "route_desc", "route_type", "route_color",
                            "route_text_color", "route_url"});
                    Iterator<Map<String, String>> iterator = feed.get("routes.txt").iterator();
                    for (Route route : feedValidator.routes) {
                        Map<String, String> row = iterator.next();
                        final String fields[] = new String[9];
                        fields[0] = route.route_id;
                        fields[1] = route.agency_id.get();
                        fields[2] = route.route_short_name;
                        fields[3] = route.route_long_name;
                        fields[4] = route.route_desc.get();
                        fields[5] = String.valueOf(route.route_type);
                        fields[6] = Strings.isNullOrEmpty(row.get("route_color")) ?
                                    "" : String.format("%06x", route.route_color);
                        fields[7] = Strings.isNullOrEmpty(row.get("route_text_color")) ?
                                    "" : String.format("%06x", route.route_text_color);
                        fields[8] = route.route_url.isPresent() ?
                                    route.route_url.get().toString() : "";
                        csvWriter.writeRecord(fields);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    System.exit(2);
                } finally {
                    csvWriter.close();
                }
            }

            {
                CsvWriter csvWriter = new CsvWriter("trips.txt", ',', UTF8);
                try {
                    csvWriter.writeRecord(new String[]{"route_id", "service_id", "trip_id",
                            "realtime_trip_id", "trip_headsign", "trip_short_name",
                            "trip_long_name", "direction_id", "block_id", "shape_id",
                            "wheelchair_accessible" ,"bikes_allowed"});
                    Iterator<Map<String, String>> iterator = feed.get("trips.txt").iterator();
                    for (Trip trip : feedValidator.trips) {
                        Map<String, String> row = iterator.next();
                        final String fields[] = new String[12];
                        fields[0] = trip.route_id;
                        fields[1] = trip.service_id;
                        fields[2] = trip.trip_id;
                        fields[3] = row.get("realtime_trip_id");
                        fields[4] = trip.trip_headsign.get();
                        fields[5] = trip.trip_short_name.get();
                        fields[6] = row.get("trip_long_name");
                        fields[7] = trip.direction_id.get() ? "1" : "0";
                        fields[8] = trip.block_id.get();
                        fields[9] = trip.shape_id.get();
                        fields[10] = trip.wheelchair_accessible.isPresent() ?
                                     String.valueOf(trip.wheelchair_accessible.get()) : "";
                        fields[11] = trip.bikes_allowed.isPresent() ?
                                     String.valueOf(trip.bikes_allowed.get()) : "";
                        csvWriter.writeRecord(fields);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    System.exit(2);
                } finally {
                    csvWriter.close();
                }
            }

            {
                CsvWriter csvWriter = new CsvWriter("stop_times.txt", ',', UTF8);
                try {
                    csvWriter.writeRecord(new String[]{"trip_id", "stop_sequence", "stop_id",
                            "stop_headsign", "arrival_time", "departure_time", "pickup_type",
                            "drop_off_type", "timepoint", "shape_dist_traveled",
                            "fare_units_traveled"});
                    Iterator<Map<String, String>> iterator = feed.get("stop_times.txt").iterator();
                    for (StopTime stopTime : feedValidator.stop_times) {
                        Map<String, String> row = iterator.next();
                        final String fields[] = new String[11];
                        fields[0] = stopTime.trip_id;
                        fields[1] = String.valueOf(stopTime.stop_sequence);
                        fields[2] = stopTime.stop_id;
                        fields[3] = stopTime.stop_headsign.get();
                        fields[4] = time(stopTime.arrival_time);
                        fields[5] = time(stopTime.departure_time);
                        fields[6] = String.valueOf(stopTime.pickup_type.get());
                        fields[7] = String.valueOf(stopTime.drop_off_type.get());
                        fields[8] = row.get("timepoint");
                        fields[9] = stopTime.shape_dist_traveled.isPresent() ?
                                    String.format("%.0f", stopTime.shape_dist_traveled.get()) : "";
                        fields[10] = row.get("fare_units_traveled");
                        csvWriter.writeRecord(fields);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    System.exit(2);
                } finally {
                    csvWriter.close();
                }
            }

            if (feedValidator.calendar_dates.isPresent()) {
                CsvWriter csvWriter = new CsvWriter("calendar_dates.txt", ',', UTF8);
                try {
                    csvWriter.writeRecord(new String[]{"service_id", "date", "exception_type"});
                    for (CalendarDate calendarDate : feedValidator.calendar_dates.get()) {
                        final String fields[] = new String[3];
                        fields[0] = calendarDate.service_id;
                        fields[1] = date(calendarDate.date);
                        fields[2] = String.valueOf(calendarDate.exception_type);
                        csvWriter.writeRecord(fields);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    System.exit(2);
                } finally {
                    csvWriter.close();
                }
            } else {
                System.exit(3);
            }

            if (feedValidator.shapes.isPresent()) {
                CsvWriter csvWriter = new CsvWriter("shapes.txt", ',', UTF8);
                try {
                    csvWriter.writeRecord(new String[]{"shape_id", "shape_pt_sequence",
                            "shape_pt_lat", "shape_pt_lon", "shape_dist_traveled"});
                    for (Shape shape : feedValidator.shapes.get()) {
                        final String fields[] = new String[5];
                        fields[0] = shape.shape_id;
                        fields[1] = String.valueOf(shape.shape_pt_sequence);
                        fields[2] = String.valueOf(Math.IEEEremainder(shape.shape_pt_lat, 1) == 0 ?
                                    String.format("%.0f", shape.shape_pt_lat) :
                                    String.valueOf(shape.shape_pt_lat));
                        fields[3] = String.valueOf(Math.IEEEremainder(shape.shape_pt_lon, 1) == 0 ?
                                    String.format("%.0f", shape.shape_pt_lon) :
                                    String.valueOf(shape.shape_pt_lon));
                        fields[4] = shape.shape_dist_traveled.isPresent() ?
                                    String.format("%.0f", shape.shape_dist_traveled.get()) : "";
                        csvWriter.writeRecord(fields);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    System.exit(2);
                } finally {
                    csvWriter.close();
                }
            } else {
                System.exit(3);
            }

            if (feedValidator.transfers.isPresent()) {
                CsvWriter csvWriter = new CsvWriter("transfers.txt", ',', UTF8);
                try {
                    csvWriter.writeRecord(new String[]{"from_stop_id", "to_stop_id",
                            "from_route_id", "to_route_id", "from_trip_id", "to_trip_id",
                            "transfer_type"});
                    Iterator<Map<String, String>> iterator = feed.get("transfers.txt").iterator();
                    for (Transfer transfer : feedValidator.transfers.get()) {
                        Map<String, String> row = iterator.next();
                        final String fields[] = new String[7];
                        fields[0] = transfer.from_stop_id;
                        fields[1] = transfer.to_stop_id;
                        fields[2] = row.get("from_route_id");
                        fields[3] = row.get("to_route_id");
                        fields[4] = row.get("from_trip_id");
                        fields[5] = row.get("to_trip_id");
                        fields[6] = transfer.transfer_type.get().toString();
                        csvWriter.writeRecord(fields);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    System.exit(2);
                } finally {
                    csvWriter.close();
                }
            } else {
                System.exit(3);
            }

            if (feedValidator.feed_info.isPresent()) {
                CsvWriter csvWriter = new CsvWriter("feed_info.txt", ',', UTF8);
                try {
                    csvWriter.writeRecord(new String[]{"feed_publisher_name",
                            "feed_id", "feed_publisher_url", "feed_lang", "feed_start_date",
                            "feed_end_date", "feed_version"});
                    Iterator<Map<String, String>> iterator = feed.get("feed_info.txt").iterator();
                    for (FeedInfo feedInfo : feedValidator.feed_info.get()) {
                        Map<String, String> row = iterator.next();
                        final String fields[] = new String[7];
                        fields[0] = feedInfo.feed_publisher_name;
                        fields[1] = row.get("feed_id");
                        fields[2] = feedInfo.feed_publisher_url.toString();
                        fields[3] = feedInfo.feed_lang.getLanguage();
                        fields[4] = date(feedInfo.feed_start_date.get());
                        fields[5] = date(feedInfo.feed_end_date.get());
                        fields[6] = feedInfo.feed_version.get();
                        csvWriter.writeRecord(fields);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    System.exit(2);
                } finally {
                    csvWriter.close();
                }
            } else {
                System.exit(3);
            }
        }

        System.out.printf("Work done after %.9f seconds.\n", (System.nanoTime() - time) * 1e-9);
    }

    private static String time(int timestamp) {
        if (timestamp == Integer.MIN_VALUE) {
            return "";
        } else {
            return String.format("%02d:%02d:%02d",
                    timestamp / 60 / 60, timestamp / 60 % 60, timestamp % 60);
        }
    }

    private static String date(LocalDate localDate) {
        return String.format("%04d%02d%02d",
                        localDate.getYear(), localDate.getMonthOfYear(), localDate.getDayOfMonth());
    }
}
