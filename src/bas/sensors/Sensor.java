package bas.sensors;

import java.util.function.Consumer;

public abstract class Sensor {

    protected boolean isTriggered;
    protected Consumer<Sensor> onFailure;
    protected SensorRepositroy repository;

    public Sensor() {
        this.isTriggered = false;
    }

    public void setRepository(SensorRepositroy repository) {
        this.repository = repository;
    }

    public boolean getIsTriggred() {
        return isTriggered;
    }

    public void seIsTriggred(boolean isTriggered) {
        this.isTriggered = isTriggered;
    }

    public Consumer<Sensor> getOnFailure() {
        return onFailure;
    }

    public void setOnFailure(Consumer<Sensor> onFailure) {
        this.onFailure = onFailure;
    }

    public abstract boolean poll();


    protected void signalFailure() {
        if (onFailure != null) {
            onFailure.accept(this);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[triggred = " + isTriggered + "]";
    }

    public boolean remove(Sensor sensor) {
        if (repository != null && sensor != null) {
            return repository.getSensors().remove(sensor);
        }
        return false;
    }
}