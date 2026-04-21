package bas.power;

import bas.epl.EsperEngine;
import java.util.function.Consumer;

public class VoltageSensor {

    private Consumer<Float> onVoltageChange;
    private float originalVoltage;
    private float currentVoltage;
    private Runnable onFailure;
    private EsperEngine esperEngine;

    public void setEsperEngine(EsperEngine esperEngine) {
        this.esperEngine = esperEngine;
    }

    public void setOnVoltageChange(float changedVoltage, boolean backupEnabled) {
        onVoltageChange.accept(changedVoltage);
        esperEngine.sendVoltageChangeEvent(currentVoltage, originalVoltage, backupEnabled);
        currentVoltage = changedVoltage;
    }

    public void setOnFailure(Runnable onFailure) {
        this.onFailure = onFailure;
    }

    public void onFailure() {
        onFailure.run();
        esperEngine.sendPowerFailureEvent("VoltageSensor");
    }
}
