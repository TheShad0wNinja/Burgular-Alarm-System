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

    public boolean hasFailed(){
        return hasFailed;
    }


}
