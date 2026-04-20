package bas.power;

import java.util.function.Consumer;

public class VoltageSensor {

    private Consumer<Float> onVoltageChange;
    private float originalVoltage;
    private float currentVoltage;
    private Runnable onFailure;

    public void setOnVoltageChange(float changedVoltage) {
        onVoltageChange.accept(changedVoltage);
        currentVoltage = changedVoltage;
    }

    public void setOnFailure() {
        onFailure.run();
    }
}
