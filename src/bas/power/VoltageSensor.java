package bas.power;

import bas.epl.EsperEngine;
import java.util.function.BiConsumer;

public class VoltageSensor {

    private float originalVoltage;
    private float currentVoltage;
    private Runnable onFailure;
    private BiConsumer<Float, Float> onVoltageChange;
    private boolean hasFailed;

    public VoltageSensor(BiConsumer<Float, Float> onVoltageChange, Runnable onFailure) {
        this.originalVoltage = 220f;
        this.currentVoltage = 220f;
        this.hasFailed = false;

        this.onVoltageChange = onVoltageChange;
        this.onFailure = onFailure;
    }

    // For testing real world would update on it's own
    public void setCurrentVoltage(float v) {
        this.originalVoltage = currentVoltage;
        this.currentVoltage = v;
        onVoltageChange.accept(this.originalVoltage, this.currentVoltage);
    }

    public void forceFailure() {
        onFailure.run();
        this.hasFailed = true;
    }

    public boolean hasFailed(){
        return hasFailed;
    }


}
