package bas.sensors;

import java.util.Random;

public class WindowSensor extends Sensor {

    private static final double Failure_Probability = 0.001;
    private final Random random = new Random();

    public WindowSensor(String windowID) {
        super(windowID);
    }

    @Override
    public boolean poll() {
        if (isBroken() || random.nextDouble() < Failure_Probability) {
            setBroken(true);
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "WindowSensor[ID = " + sensorID + ", triggried - " + getIsTriggred() + "]";
    }
}