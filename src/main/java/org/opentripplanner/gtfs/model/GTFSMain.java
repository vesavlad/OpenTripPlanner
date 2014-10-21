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

import org.opentripplanner.routing.trippattern.Deduplicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GTFSMain {

    private static final Logger LOG = LoggerFactory.getLogger(GTFSMain.class);

    //static final String INPUT = "/var/otp/graphs/dc/2014-02-07-WMATA.gtfs.zip";
    static final String INPUT = "/var/otp/graphs/nl/gtfs-nl.zip";
    //static final String INPUT = "/var/otp/graphs/trimet/gtfs.zip";        
    
    public static void main (String[] args) {
        GTFSFeed feed = new GTFSFeed(INPUT, new Deduplicator());
        feed.findPatterns();
        feed.closeDb();
    }

}
