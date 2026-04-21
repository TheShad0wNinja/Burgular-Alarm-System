package bas.power;

public class VoltageChangeEvent {

    private final float currentVoltage;
    private final float originalVoltage;
    private final float dropPercent;
    private final boolean backupEnabled;
    private final long timeStamp;

    public VoltageChangeEvent(float currentVoltage, float originalVoltage, boolean backupEnabled) {
        this.currentVoltage = currentVoltage;
        this.originalVoltage = originalVoltage;
        this.dropPercent = originalVoltage > 0 ? ((originalVoltage - currentVoltage) / originalVoltage) * 100f : 0f;
        this.backupEnabled = backupEnabled;
        this.timeStamp = System.currentTimeMillis();

    }

    public float   getCurrentVoltage() {
        return currentVoltage;
    }
    public float   getOriginalVoltage() {
        return originalVoltage;
    }
    public float   getDropPercent() {
        return dropPercent;
    }
    public boolean getBackupEnabled() {
        return backupEnabled;
    }
    public long    getTimestamp() {
        return timeStamp;
    }

    @Override
    public String toString() {
        return String.format("VoltageChangeEvent[drop=%.1f%%  backupOn=%b]",
                dropPercent, backupEnabled);
    }

}
