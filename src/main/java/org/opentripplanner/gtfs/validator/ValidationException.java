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

import com.google.common.base.Optional;
import org.opentripplanner.gtfs.format.FeedFile;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;

public class ValidationException extends RuntimeException {
    final public FeedFile feedFile;
    final public int line;
    final public Optional<String> column;

    public ValidationException(FeedFile feedFile, int line, String string) {
        super(message(feedFile, line, null, string));
        this.feedFile = feedFile;
        this.line = line;
        column = absent();
    }

    public ValidationException(FeedFile feedFile, int line, String column, String string) {
        super(message(feedFile, line, column, string));
        this.feedFile = feedFile;
        this.line = line;
        this.column = fromNullable(column);
    }

    public ValidationException(FeedFile feedFile, int line, String column, Throwable throwable) {
        super(message(feedFile, line, column, throwable.toString()), throwable);
        this.feedFile = feedFile;
        this.line = line;
        this.column = fromNullable(column);
    }

    private static String message(FeedFile feedFile, int line, String column, String string) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(feedFile);

        if (line > 0){
            stringBuilder.append(':');
            stringBuilder.append(line);

            if (column != null) {
                stringBuilder.append(':');
                stringBuilder.append(column);
            }
        }

        stringBuilder.append(": ");
        return stringBuilder.append(string).toString();
    }
}
