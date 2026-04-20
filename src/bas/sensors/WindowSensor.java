package bas.sensors;

import java.util.Random;

public class WindowSensor extends Sensor {

    private final String windowID;

    private static final double Failure_Probability = 0.2;
    private final Random random = new Random();

    public WindowSensor(String windowID) {
        this.windowID = windowID;
    }

    public String getWindowID(){
        return windowID;
    }

    @Override
    public boolean poll() {
        if (random.nextDouble() < Failure_Probability) {
            signalFailure();
        return false;
        }
        return true;
    }

    public void simulatedWindowOpen(boolean open){
        seIsTriggred(open);
    }

    @Override
    public String toString() {
        return "WindowSensor[ID = " + windowID + ", triggried - " + isTriggered + "]";
    }
}