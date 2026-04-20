package bas.sensors;

import java.sql.Time;
import java.sql.Timestamp;

public class SensorFailureEvent {

    private final Sensor sensor;
    private final String sensorType;
    private final long timeStamp;

    public  SensorFailureEvent(Sensor sensor) {
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

    @Override
    public String toString() {
        return "SensorFailureEvent[Type = " + sensorType + ", time = " + timeStamp + "]";
    }
}
