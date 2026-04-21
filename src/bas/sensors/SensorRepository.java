package bas.sensors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SensorRepository {

    private final List<Sensor> sensors = Collections.synchronizedList(new ArrayList<>());

    public List<Sensor> getSensors(){
        synchronized (sensors) {
            return new ArrayList<>(sensors);
        }
    }

    public void addSensor(Sensor sensor) {
        if (sensor == null) {
            throw new IllegalArgumentException("Sensor must not be null");
        }
        sensors.add(sensor);
        System.out.println("[SensorRepository] Added: " + sensor);
    }

    public boolean removeSensor(Sensor sensor){
        boolean removed = sensors.remove(sensor);
        if (removed) {
            System.out.println("[SensorRepository] Removed: " + sensor);
        }
        return removed;
    }

    public int size(){
     return sensors.size();
    }
}