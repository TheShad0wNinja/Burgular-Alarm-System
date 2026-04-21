package bas.ui;

import bas.core.BASController;
import bas.phone.Audio;
import bas.phone.CallRequest;
import bas.phone.CallType;
import bas.phone.Phone;
import bas.phone.PhoneController;
import bas.power.PowerController;
import bas.rooms.Room;
import bas.sensors.Sensor;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DebugUI extends JFrame {

    private final BASController controller;
    private final List<Runnable> refreshTasks = new ArrayList<>();
    private ConcurrentLinkedQueue<CallRequest> callQueue;
    private UIPhoneSpy phoneSpy;
    private PowerController powerController;
    private VoltageSensorSpy powerSpy;

    public DebugUI(BASController controller) {
        this.controller = controller;
        injectPhoneSpy();
        injectPowerSpy();
        initUI();
        startRefreshTimer();
    }

    private void injectPowerSpy() {
        try {
            Field pcField = BASController.class.getDeclaredField("powerController");
            pcField.setAccessible(true);
            this.powerController = (PowerController) pcField.get(controller);

            if (powerController != null) {
                Field vsField = PowerController.class.getDeclaredField("mainPowerSensor");
                vsField.setAccessible(true);
                Object originalSensor = vsField.get(powerController);
                this.powerSpy = new VoltageSensorSpy(originalSensor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void injectPhoneSpy() {
        try {
            Field pcField = BASController.class.getDeclaredField("phoneController");
            pcField.setAccessible(true);
            PhoneController pc = (PhoneController) pcField.get(controller);

            if (pc != null) {
                Field queueField = PhoneController.class.getDeclaredField("callQueue");
                queueField.setAccessible(true);
                this.callQueue = (ConcurrentLinkedQueue<CallRequest>) queueField.get(pc);

                Field phoneField = PhoneController.class.getDeclaredField("phone");
                phoneField.setAccessible(true);
                Phone originalPhone = (Phone) phoneField.get(pc);

                this.phoneSpy = new UIPhoneSpy(originalPhone);
                phoneField.set(pc, this.phoneSpy);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initUI() {
        setTitle("BAS Debug UI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        
        JLabel sysStatusLabel = new JLabel("System: OFF");
        sysStatusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        sysStatusLabel.setOpaque(true);
        sysStatusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel alarmStatusLabel = new JLabel("Global Alarm: OFF");
        alarmStatusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        alarmStatusLabel.setOpaque(true);
        alarmStatusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        topPanel.add(sysStatusLabel);
        topPanel.add(alarmStatusLabel);
        add(topPanel, BorderLayout.NORTH);

        refreshTasks.add(() -> {
            if (controller.isSystemOn()) {
                sysStatusLabel.setText("System: ON");
                sysStatusLabel.setBackground(new Color(144, 238, 144)); // Light green
                sysStatusLabel.setForeground(Color.BLACK);
            } else {
                sysStatusLabel.setText("System: OFF");
                sysStatusLabel.setBackground(Color.LIGHT_GRAY);
                sysStatusLabel.setForeground(Color.BLACK);
            }

            if (controller.isAlarmTriggered()) {
                alarmStatusLabel.setText("Global Alarm: TRIGGERED");
                alarmStatusLabel.setBackground(Color.RED);
                alarmStatusLabel.setForeground(Color.WHITE);
            } else {
                alarmStatusLabel.setText("Global Alarm: OK");
                alarmStatusLabel.setBackground(new Color(144, 238, 144)); // Light green
                alarmStatusLabel.setForeground(Color.BLACK);
            }
        });

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(0, 2, 5, 5)); // 2 columns for rooms
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        List<Room> rooms = controller.getRoomRepository().getRooms();
        for (Room room : rooms) {
            JPanel roomPanel = createRoomPanel(room);
            mainPanel.add(roomPanel);
        }

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        JPanel eastPanel = new JPanel(new BorderLayout());
        eastPanel.setPreferredSize(new Dimension(220, 0));
        JPanel powerPanel = createPowerPanel();
        JPanel phonePanel = createPhonePanel();
        eastPanel.add(powerPanel, BorderLayout.NORTH);
        eastPanel.add(phonePanel, BorderLayout.CENTER);
        add(eastPanel, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(850, 450));
        setPreferredSize(new Dimension(900, 500));
    }

    private JPanel createPowerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Power System"));

        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        JLabel voltageLabel = new JLabel("Voltage: 220.0V");
        voltageLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        infoPanel.add(voltageLabel);
        
        JLabel backupLabel = new JLabel("Backup Battery: OFF");
        backupLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 10, 5));
        infoPanel.add(backupLabel);
        panel.add(infoPanel, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JButton btnDropMinor = new JButton("Set 180V (Minor Drop)");
        btnDropMinor.setFocusPainted(false);
        btnDropMinor.addActionListener(e -> {
            if (powerSpy != null) powerSpy.setVoltage(180f);
        });
        
        JButton btnDropMajor = new JButton("Set 0V (Major Drop)");
        btnDropMajor.setFocusPainted(false);
        btnDropMajor.addActionListener(e -> {
            if (powerSpy != null) powerSpy.setVoltage(0f);
        });
        
        JButton btnRecover = new JButton("Set 220V (Recover)");
        btnRecover.setFocusPainted(false);
        btnRecover.addActionListener(e -> {
            if (powerSpy != null) powerSpy.setVoltage(220f);
        });
        
        JButton btnFail = new JButton("Force Sensor Fail");
        btnFail.setFocusPainted(false);
        btnFail.addActionListener(e -> {
            if (powerSpy != null) powerSpy.forceFail();
        });

        btnPanel.add(btnDropMinor);
        btnPanel.add(btnDropMajor);
        btnPanel.add(btnRecover);
        btnPanel.add(btnFail);
        
        panel.add(btnPanel, BorderLayout.CENTER);
        
        refreshTasks.add(() -> {
            if (powerSpy != null) {
                voltageLabel.setText("Voltage: " + powerSpy.getCurrentVoltage() + "V" + (powerSpy.hasFailed() ? " (FAILED)" : ""));
                if (powerSpy.hasFailed()) {
                    voltageLabel.setForeground(Color.RED);
                } else {
                    voltageLabel.setForeground(Color.BLACK);
                }
            }
            if (powerController != null) {
                boolean backup = powerController.isBackupEnabled();
                backupLabel.setText("Backup Battery: " + (backup ? "ON" : "OFF"));
                backupLabel.setForeground(backup ? Color.BLUE : Color.BLACK);
            }
        });

        return panel;
    }

    private JPanel createPhonePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Phone System"));

        JLabel currentCallLabel = new JLabel("<html>Current Call:<br/><i>Idle</i></html>");
        currentCallLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
        panel.add(currentCallLabel, BorderLayout.NORTH);

        DefaultListModel<String> queueModel = new DefaultListModel<>();
        JList<String> queueList = new JList<>(queueModel);
        queueList.setEnabled(false);
        JScrollPane queueScroll = new JScrollPane(queueList);
        queueScroll.setBorder(BorderFactory.createTitledBorder("Call Queue"));
        panel.add(queueScroll, BorderLayout.CENTER);

        refreshTasks.add(() -> {
            if (phoneSpy != null) {
                currentCallLabel.setText("<html>Current Call:<br/><i>" + phoneSpy.getStatus() + "</i></html>");
            }
            if (callQueue != null) {
                queueModel.clear();
                for (CallRequest req : callQueue) {
                    queueModel.addElement("[" + req.getType() + "] " + req.getCallMsg());
                }
                if (queueModel.isEmpty()) {
                    queueModel.addElement("Queue empty");
                }
            }
        });

        return panel;
    }

    private JPanel createRoomPanel(Room room) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), room.getName()));

        JLabel lightLabel = new JLabel("Light Status: OFF");
        lightLabel.setOpaque(true);
        lightLabel.setBackground(Color.DARK_GRAY);
        lightLabel.setForeground(Color.WHITE);
        lightLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(lightLabel);

        refreshTasks.add(() -> {
            boolean isLightOn = room.getLight().isOn();
            lightLabel.setText(isLightOn ? "Light Status: ON" : "Light Status: OFF");
            lightLabel.setBackground(isLightOn ? Color.YELLOW : Color.DARK_GRAY);
            lightLabel.setForeground(isLightOn ? Color.BLACK : Color.WHITE);
        });

        panel.add(Box.createRigidArea(new Dimension(0, 5)));

        for (Sensor originalSensor : room.getSensors()) {
            SensorSpy sensor = new SensorSpy(originalSensor);
            JPanel sensorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));

            JLabel sensorLabel = new JLabel(originalSensor.getSensorId() + " ");
            sensorLabel.setPreferredSize(new Dimension(135, 20));
            sensorLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            sensorLabel.setOpaque(true);
            sensorLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

            Font btnFont = new Font("Arial", Font.PLAIN, 11);
            Insets btnInsets = new Insets(2, 6, 2, 6);

            JButton btnToggle = new JButton("Toggle");
            btnToggle.setFont(btnFont);
            btnToggle.setMargin(btnInsets);
            btnToggle.setFocusPainted(false);
            btnToggle.addActionListener(e -> {
                sensor.toggleTrigger();
            });

            JButton btnFail = new JButton("Force Fail");
            btnFail.setFont(btnFont);
            btnFail.setMargin(btnInsets);
            btnFail.setFocusPainted(false);
            btnFail.addActionListener(e -> {
                sensor.forceFail();
            });

            sensorPanel.add(sensorLabel);
            sensorPanel.add(btnToggle);
            sensorPanel.add(btnFail);

            panel.add(sensorPanel);

            // Update state periodically
            refreshTasks.add(() -> {
                if (sensor.isBroken()) {
                    sensorLabel.setBackground(Color.ORANGE);
                    sensorLabel.setForeground(Color.BLACK);
                    sensorLabel.setText(originalSensor.getSensorId() + " [Failed]");
                } else if (sensor.getIsTriggered()) {
                    sensorLabel.setBackground(Color.RED);
                    sensorLabel.setForeground(Color.WHITE);
                    sensorLabel.setText(originalSensor.getSensorId() + " [Trig]");
                } else {
                    sensorLabel.setBackground(new Color(144, 238, 144)); // Light green
                    sensorLabel.setForeground(Color.BLACK);
                    sensorLabel.setText(originalSensor.getSensorId() + " [OK]");
                }
            });
        }

        return panel;
    }

    private void startRefreshTimer() {
        Timer timer = new Timer(500, e -> {
            for (Runnable task : refreshTasks) {
                task.run();
            }
        });
        timer.start();
    }

    private static class VoltageSensorSpy {
        private final Object voltageSensor;
        public VoltageSensorSpy(Object voltageSensor) {
            this.voltageSensor = voltageSensor;
        }
        public void setVoltage(float v) {
            try {
                Field curVoltField = voltageSensor.getClass().getDeclaredField("currentVoltage");
                curVoltField.setAccessible(true);
                float old = (float) curVoltField.get(voltageSensor);
                
                Field origVoltField = voltageSensor.getClass().getDeclaredField("originalVoltage");
                origVoltField.setAccessible(true);
                origVoltField.set(voltageSensor, old);
                
                curVoltField.set(voltageSensor, v);
                
                Field onVoltField = voltageSensor.getClass().getDeclaredField("onVoltageChange");
                onVoltField.setAccessible(true);
                java.util.function.BiConsumer<Float, Float> onVolt = (java.util.function.BiConsumer<Float, Float>) onVoltField.get(voltageSensor);
                if (onVolt != null) onVolt.accept(old, v);
            } catch (Exception e) { e.printStackTrace(); }
        }
        public void forceFail() {
            try {
                Field failedField = voltageSensor.getClass().getDeclaredField("hasFailed");
                failedField.setAccessible(true);
                failedField.set(voltageSensor, true);
                
                Field onFailField = voltageSensor.getClass().getDeclaredField("onFailure");
                onFailField.setAccessible(true);
                Runnable onFail = (Runnable) onFailField.get(voltageSensor);
                if (onFail != null) onFail.run();
            } catch (Exception e) { e.printStackTrace(); }
        }
        public float getCurrentVoltage() {
            try {
                Field curVoltField = voltageSensor.getClass().getDeclaredField("currentVoltage");
                curVoltField.setAccessible(true);
                return (float) curVoltField.get(voltageSensor);
            } catch (Exception e) { return 220f; }
        }
        public boolean hasFailed() {
            try {
                Field failedField = voltageSensor.getClass().getDeclaredField("hasFailed");
                failedField.setAccessible(true);
                return (boolean) failedField.get(voltageSensor);
            } catch (Exception e) { return false; }
        }
    }

    private static class SensorSpy {
        private final Sensor sensor;
        public SensorSpy(Sensor sensor) {
            this.sensor = sensor;
        }
        public void toggleTrigger() {
            try {
                Field f = Sensor.class.getDeclaredField("isTriggered");
                f.setAccessible(true);
                f.set(sensor, !getIsTriggered());
            } catch (Exception e) {}
        }
        public void forceFail() {
            try {
                Field f = Sensor.class.getDeclaredField("isBroken");
                f.setAccessible(true);
                f.set(sensor, true);
            } catch (Exception e) {}
        }
        public boolean getIsTriggered() {
            try {
                Field f = Sensor.class.getDeclaredField("isTriggered");
                f.setAccessible(true);
                return (boolean) f.get(sensor);
            } catch (Exception e) { return false; }
        }
        public boolean isBroken() {
            try {
                Field f = Sensor.class.getDeclaredField("isBroken");
                f.setAccessible(true);
                return (boolean) f.get(sensor);
            } catch (Exception e) { return false; }
        }
    }

    private static class UIPhoneSpy extends Phone {
        private final Phone original;
        private String status = "Idle";

        public UIPhoneSpy(Phone original) {
            this.original = original;
        }

        public String getStatus() {
            return status;
        }

        @Override
        public boolean ringPhone(CallType type) {
            status = "Ringing " + type + "...";
            return original.ringPhone(type);
        }

        @Override
        public void beginCall(Audio audio) {
            status = "Speaking: " + audio.getValue();
            original.beginCall(audio);
        }

        @Override
        public void endCall() {
            status = "Call Ended.";
            original.endCall();
        }

        @Override
        public boolean getIsInCall() {
            return original.getIsInCall();
        }

        @Override
        public void setIsInCall(boolean isInCall) {
            original.setIsInCall(isInCall);
        }
    }
}