package bas.epl;

public class RTSConstraints {

    private RTSConstraints(){};

    public static final long SENSOR_POLL_INTERVAL_MS = 500L;

    public static final long SENSOR_TRIGGERED_RESPONSE_MS = 100L;

    public static final long POWER_DROP_MINOR_RESPONSE_MS = 50L;

    public static final long POWER_DROP_MAJOR_RESPONSE_MS = 150L;

    public static final long PANIC_BUTTON_RESPONSE_MS = 100L;

    public static final long CLEAR_ALARAMS_RESPONSE_MS = 1000L;

    public static final long INTRUSION_FULL_RESPONSE_MS = 2000L;

    public static final long FAILURE_NOTIFY_RESPONSE_MS = 3000L;

}