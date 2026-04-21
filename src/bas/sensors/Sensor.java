package bas.sensors;

public abstract class Sensor {

    protected final String sensorID;

    protected boolean isTriggered;
    protected boolean isBroken;

    public Sensor(String sensorId) {
        this.isTriggered = false;
        this.isBroken = false;
        this.sensorID = sensorId;
    }

    public String getSensorId() {
        return sensorID;
    }

    public void setIsTriggered(boolean isTriggered) {
        this.isTriggered = isTriggered;
    }

    public boolean getIsTriggred() {
        return isTriggered;
    }

    public void seIsTriggred(boolean isTriggered) {
        this.isTriggered = isTriggered;
    }

    public abstract boolean poll();

    public void forceFailure() {
        this.isBroken = true;
    }

    public boolean isBroken() {
        return isBroken;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[triggred = " + isTriggered + ", broken = " + isBroken + "]";
    }
}