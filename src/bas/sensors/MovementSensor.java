package bas.sensors;

import java.util.Random;

public class MovementSensor extends Sensor{

   private final String roomID;

    private static final double Failure_Probability = 0.2;
    private final Random random = new Random();

    public MovementSensor(String roomID) {
        this.roomID = roomID;
    }

    public String getRoomID(){
        return roomID;
    }

    @Override
    public boolean poll(){
        if (random.nextDouble() < Failure_Probability) {
            signalFailure();
            return false;
        }
        return true;
    }

    public void simulateMotionDetected(boolean detected) {
        seIsTriggred(detected);
    }

    @Override
    public String toString(){
     return "MovementSensor[room = " + roomID + ", triggred" + isTriggered + "]";
    }
}
