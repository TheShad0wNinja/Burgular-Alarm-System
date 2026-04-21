package bas.sensors;

public abstract class Sensor {

    protected final String sensorID;

    private boolean isTriggered;
    private boolean isBroken;

    public Sensor(String sensorId) {
        this.isTriggered = false;
        this.isBroken = false;
        this.sensorID = sensorId;
    }

    public String getSensorId() {
        return sensorID;
    }

    public boolean getIsTriggred() {
        return isTriggered;
    }

    public void disableIsTriggered(){
        isTriggered = false;
    }

    public abstract boolean poll();

    public boolean isBroken() {
        return isBroken;
    }

    protected void setBroken(boolean isBroken) {
        this.isBroken = isBroken;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[triggred = " + isTriggered + ", broken = " + isBroken + "]";
    }
}