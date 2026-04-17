package bas.sensors;

import java.util.Random;

public class DoorSensor extends Sensor{

    private final String doorID;

    private static final double Failure_Probability = 0.2;
    private final Random random = new Random();

    public DoorSensor(String doorID){
        this.doorID = doorID;
    }

    public String getDoorID() {
        return doorID;
    }

    @Override
    public boolean poll(){
        if (random.nextDouble() < Failure_Probability) {
            signalFailure();;
            return false;
        }
        return true;
    }

    public void simulateOpenDoor(boolean open) {
        seIsTriggred(open);
    }

    @Override
    public String toString(){
        return "DoorSensor[ID = " + doorID + ", triggred" + isTriggered + "]";
    }
}
