package bas.sensors;

import bas.epl.EsperEngine;
import bas.epl.RTSConstraints;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class SensorController implements Runnable {

    private final SensorRepository sensorRepo;
    private final EsperEngine esperEngine;
    private final Set<Sensor> failedSensors = new HashSet<>();

    private Thread pollThread;
    private volatile boolean running = false;

    public SensorController(SensorRepository sensorRepo, EsperEngine esperEngine) {

        this.sensorRepo = sensorRepo;
        this.esperEngine = esperEngine;

    }

    public void beginPollCycle() {
        if (running) {
            System.out.println("[SensorController] Poll Cycle already running.");
            return;
        }
        running = true;
        pollThread = new Thread(this, "SensorPollThread");
        pollThread.setDaemon(true);
        pollThread.start();
        System.out.printf("[SensorController] Poll cycle started "
            + "(interval = %dms   triggered-deadline = %dms)%n",
            RTSConstraints.SENSOR_POLL_INTERVAL_MS,
            RTSConstraints.SENSOR_TRIGGERED_RESPONSE_MS);
    }

    public void stopPollCycle() {
        running = false;
        if (pollThread != null) {
            pollThread.interrupt();
        }
        System.out.println("[SensorController] Poll cycle stopped.");
    }

    @Override
    public void run() {
        while (running) {
            long cycleStart = System.currentTimeMillis();
            List<Sensor> sensors = sensorRepo.getSensors();
            if (!sensors.isEmpty()) {
                pollDevices(sensors);
            }

            long elapsed = System.currentTimeMillis() - cycleStart;

            if (elapsed > RTSConstraints.SENSOR_POLL_INTERVAL_MS) {
                System.out.printf("[RT WARNING] Poll Cycle took %d ms   (budget = %d ms)%n",
                    elapsed, RTSConstraints.SENSOR_POLL_INTERVAL_MS);
            }

            long sleepTime = RTSConstraints.SENSOR_POLL_INTERVAL_MS - elapsed;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public void pollDevices(List<Sensor> sensors) {
        for (Sensor sensor : sensors) {

            long pollStart = System.currentTimeMillis();
            boolean operational = sensor.poll();
            long pollElasped = System.currentTimeMillis() - pollStart;

            if (!operational) {
                if (!failedSensors.contains(sensor)) {
                    failedSensors.add(sensor);
                    handleFailure(sensor);
                }
                continue;
            }

            if (sensor.getIsTriggred()) {

                long dispatchStart = System.currentTimeMillis();
                esperEngine.sendTriggeredEvent(sensor);
                long dispatchedElasped = System.currentTimeMillis() - dispatchStart;

                long totalResponse = pollElasped + dispatchedElasped;

                System.out.printf("[SensorController] Intrusion DETECTED: %s "
                    + "(response = %dms   deadline = %dms)%n",
                    sensor, totalResponse,
                    RTSConstraints.SENSOR_TRIGGERED_RESPONSE_MS);

                if (totalResponse > RTSConstraints.SENSOR_TRIGGERED_RESPONSE_MS) {
                    System.out.printf("[RT WARNING] TRIGGERD response MISSED DEADLINE: "
                        + "%dms > %dms sensor = %s%n",
                        totalResponse,
                        RTSConstraints.SENSOR_TRIGGERED_RESPONSE_MS, sensor);
                }
            } else {
                System.out.println("[SensorController] Intrusion NOT detected: " + sensor);
            }
        }
    }

    public void pollDevices() {
        pollDevices(sensorRepo.getSensors());
    }

    public void handleFailure(Sensor sensor) {

        long start = System.currentTimeMillis();
        esperEngine.sendFailureEvent(sensor);

        long elsasped = System.currentTimeMillis() - start;

        System.out.printf("[SensorController] Sensor FAILURE: %s "
            + "(dspatch = %dms deadline = %dms) %n",
            sensor, elsasped,
            RTSConstraints.FAILURE_NOTIFY_RESPONSE_MS);
        if (elsasped > RTSConstraints.FAILURE_NOTIFY_RESPONSE_MS) {
            System.out.printf(
                "[RT WARNING] Failure notification MISSED DEADLINE: "
                + "%dms > %dms sensor = %s%n",
                elsasped, RTSConstraints.FAILURE_NOTIFY_RESPONSE_MS, sensor);
        }
    }
}
