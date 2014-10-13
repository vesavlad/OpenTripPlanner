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

package org.opentripplanner.gtfs.format;

public enum FeedFile {
    AGENCY,
    STOPS,
    ROUTES,
    TRIPS,
    STOP_TIMES,
    CALENDAR,
    CALENDAR_DATES,
    FARE_ATTRIBUTES,
    FARE_RULES,
    SHAPES,
    FREQUENCIES,
    TRANSFERS,
    FEED_INFO;

    @Override
    public String toString() {
        return this.name().toLowerCase() + ".txt";
    }
}
