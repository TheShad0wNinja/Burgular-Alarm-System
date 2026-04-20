package bas.power;

import java.util.function.Consumer;

public class VoltageSensor {

    private Consumer<Float> onVoltageChange;
    private float originalVoltage;
    private float currentVoltage;
    private Runnable onFailure;

    public void setOnVoltageChange(Consumer<Float> onVoltageChange) {
        this.onVoltageChange = onVoltageChange;
    }

    public void setOnFailure(Runnable onFailure) {
        this.onFailure = onFailure;
    }
}
