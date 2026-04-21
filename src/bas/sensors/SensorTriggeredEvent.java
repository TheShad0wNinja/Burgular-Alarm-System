package bas.sensors;

public class SensorTriggeredEvent {

    private final Sensor sensor;
    private final String sensorType;
    private final long timeStamp;

    public SensorTriggeredEvent(Sensor sensor) {
        this.sensor = sensor;
        this.sensorType = sensor.getClass().getSimpleName();
        this.timeStamp = System.currentTimeMillis();
    }

    public Sensor getSensor() {
        return sensor;
    }

    public String getSensorType() {
        return sensorType;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public boolean getIsTriggered() {
        return sensor.getIsTriggred();
    }

    @Override
    public String toString() {
        return "SensorTriggredEvent[type = " + sensorType + ", time = " + timeStamp + "]";
    }
}
