package bas.power;

public class PowerController {

    private VoltageSensor mainPowerSensor;
    private BackupBattery battery;
    private boolean backupEnabled;

    public boolean isBackupEnabled() {
        return backupEnabled;
    }

    public void setMainPowerSensor(VoltageSensor mainPowerSensor) {
        this.mainPowerSensor = mainPowerSensor;
    }

    public void setBattery(BackupBattery battery) {
        this.battery = battery;
    }

    private void handleVoltageChange(float receivedVoltage) {
        mainPowerSensor.setOnVoltageChange(receivedVoltage);
    }

    private void handleSensorFailure(VoltageSensor backupSensor) {
        mainPowerSensor.setOnFailure();
        setMainPowerSensor(backupSensor);
    }
}
