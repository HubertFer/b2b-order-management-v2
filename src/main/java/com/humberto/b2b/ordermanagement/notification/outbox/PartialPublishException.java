package com.humberto.b2b.ordermanagement.notification.outbox;

public class PartialPublishException extends RuntimeException {

    private final int published;

    public PartialPublishException(int published, Throwable cause) {
        super("Published " + published + " messages before failing", cause);
        this.published = published;
    }

    public int published() {
        return published;
    }
}
