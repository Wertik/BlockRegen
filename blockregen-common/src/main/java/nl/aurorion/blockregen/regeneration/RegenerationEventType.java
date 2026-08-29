package nl.aurorion.blockregen.regeneration;

// Type of the event being handled.
public enum RegenerationEventType {
    BLOCK_BREAK,
    TRAMPLING,
    // Picking up a liquid (water, lava,...) with a bucket.
    BUCKET_FILL,
    // 1.16+
    HARVEST
}
