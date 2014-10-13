package org.opentripplanner.gtfs.validator;

import org.opentripplanner.gtfs.format.FeedFile;

public class ValidationException extends RuntimeException {
    final public FeedFile feedFile;

    public ValidationException(FeedFile feedFile, String string) {
        super(feedFile + ": " + string);
        this.feedFile = feedFile;
    }

    public ValidationException(FeedFile feedFile, Throwable throwable) {
        super(feedFile + ": " + throwable.getMessage(), throwable);
        this.feedFile = feedFile;
    }
}
