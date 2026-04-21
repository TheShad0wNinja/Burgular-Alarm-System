package bas.sensors;

import java.util.Random;

public class DoorSensor extends Sensor{

    private static final double Failure_Probability = 0.001;
    private final Random random = new Random();

    public DoorSensor(String doorID){
        super(doorID);
    }

    @Override
    public boolean poll(){
        if (isBroken || random.nextDouble() < Failure_Probability) {
            isBroken = true;
            return false;
        }
        return true;
    }

    public void simulateOpenDoor(boolean open) {
        seIsTriggred(open);
    }

    @Override
    public String toString(){
        return "DoorSensor[ID = " + sensorID + ", triggred" + isTriggered + "]";
    }
}
