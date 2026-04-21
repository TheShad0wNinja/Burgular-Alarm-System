package bas.power;

import bas.epl.EsperEngine;

public class PowerController {

    private VoltageSensor mainPowerSensor;
    private BackupBattery battery;
    private boolean backupEnabled;
    private EsperEngine engine;

    public PowerController(EsperEngine engine) {
        this.engine = engine;
        this.battery = new BackupBattery();
        this.backupEnabled = false;

        this.mainPowerSensor = new VoltageSensor(this::handleVoltageChange, this::handleSensorFailure);

    }

    public BackupBattery getBattery() {
        return battery;
    }

    public boolean isBackupEnabled() {
        return backupEnabled;
    }
    
    public void enableBackup() {
        this.backupEnabled = true;
        battery.turnOn();
    }

    public void disableBackup() {
        this.backupEnabled = false;
        battery.turnOff();
    }

    public void setMainPowerSensor(VoltageSensor mainPowerSensor) {
        this.mainPowerSensor = mainPowerSensor;
    }

    public void setBattery(BackupBattery battery) {
        this.battery = battery;
    }

    private void handleVoltageChange(float current, float original) {
        if (mainPowerSensor.hasFailed()) return;
        engine.sendVoltageChangeEvent(current, original, backupEnabled);
    }
    private void handleSensorFailure() {
        engine.sendPowerFailureEvent("VoltageSensor");
    }
}
