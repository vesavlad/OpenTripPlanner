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
import static java.lang.System.identityHashCode;
import static java.util.Arrays.deepEquals;
import static java.util.Arrays.deepHashCode;

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

    @Override
    public int hashCode() {
        final int hash1 = identityHashCode(feedFile);
        final int hash2 = line;
        final int hash3 = column.hashCode();

        final Throwable cause = getCause();
        final int hash4 = cause != null ? cause.hashCode() : 0;

        final String message = getMessage();
        final int hash5 = message != null ? message.hashCode() : 0;

        StackTraceElement[] stackTrace = getStackTrace();
        final int hash6 = stackTrace != null ? deepHashCode(stackTrace) : 0;

        return (3 * hash1) + (5 * hash2) + (7 * hash3) + (11 * hash4) + (13 * hash5) + (17 * hash6);
    }

    @Override
    public boolean equals (Object object) {
        if (object instanceof ValidationException) {
            ValidationException that = (ValidationException) object;

            if (this.feedFile != that.feedFile) return false;
            if (this.line != that.line) return false;

            if (!column.equals(that.column)) return false;

            Throwable cause = getCause();
            if (cause == null) {
                if (that.getCause() != null) return false;
            } else {
                if (!cause.equals(that.getCause())) return false;
            }

            String message = getMessage();
            if (message == null) {
                if (that.getMessage() != null) return false;
            } else {
                if (!message.equals(that.getMessage())) return false;
            }

            StackTraceElement[] stackTrace = getStackTrace();
            if (stackTrace == null) {
                if (that.getStackTrace() != null) return false;
            } else {
                if (!(deepEquals(stackTrace, that.getStackTrace()))) return false;
            }

            return true;
        } else {
            return false;
        }
    }
}
