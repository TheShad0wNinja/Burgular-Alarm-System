package bas.power;

public class BackupBattery {

    private boolean isPoweringSystem;
    private float currentCharge = 100;

    public void turnOn() {
        isPoweringSystem = true;
    }

    public void turnOff() {
        isPoweringSystem = false;
    }

    public boolean isPoweringSystem() {
        return isPoweringSystem;
    }

    public float getCurrentCharge() {
        return currentCharge;
    }
}
