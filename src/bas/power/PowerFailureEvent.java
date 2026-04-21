package bas.power;

public class PowerFailureEvent {

    private final String failureSource;
    private final long timeStamp;


    public PowerFailureEvent(String failureSource) {
        this.failureSource = failureSource;
        this.timeStamp = System.currentTimeMillis();
    }


    public String getFailureSource() {
        return failureSource;
    }
    public long getTimestamp() {
        return timeStamp;
    }
    @Override
    public String toString() {
        return "PowerFailureEvent[source=" + failureSource + "]";
    }
}
