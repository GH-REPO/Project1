package info.tinyapps.huges.services;

import java.util.concurrent.TimeUnit;

public class StaticConfig {
    public static final long LOC_UPDATE_INTERVAL = 1500;
    public static final long LOC_FASTEST_UPDATE = 1000;
    public static final long LOC_TOO_OLD = 30 *1000;
    public static final long CHECK_MAX_RETRIES = 30;
    public static final long NEXT_RUN = 60000;
    public static final String ATTR_BEACON_ID = "custom:beacon_id";
    public static final long UPLOAD_INTERVAL = TimeUnit.HOURS.toMillis(1);
}

