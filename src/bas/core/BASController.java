/*
* Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
* Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
*/
package bas.core;

import bas.epl.EsperEngine;
import bas.phone.PhoneController;
import bas.power.PowerController;
import bas.power.PowerFailureEvent;
import bas.power.VoltageChangeEvent;
import bas.rooms.Buzzer;
import bas.rooms.Room;
import bas.rooms.RoomRepository;
import bas.sensors.Sensor;
import bas.sensors.DoorSensor;
import bas.sensors.MovementSensor;
import bas.sensors.SensorController;
import bas.sensors.SensorRepositroy;
import bas.sensors.WindowSensor;
import bas.ui.DebugUI;
import javax.swing.SwingUtilities;

/**
 *
 * @author shadow
 */
public class BASController {
	
	private RoomRepository roomRepository;
	private SensorRepositroy sensorRepository;

	private PhoneController phoneController;
    private PowerController powerController; 
    private SensorController sensorController;

    private EsperEngine powerEngine;
    private EsperEngine sensorEngine;

	private Buzzer buzzer;

	private boolean systemIsOn;
	private boolean alarmTriggered = false;

	public boolean isAlarmTriggered() {
        return alarmTriggered;
    }

    public boolean isSystemOn() {
        return systemIsOn;
    }

    public RoomRepository getRoomRepository() {
        return roomRepository;
    }
	
	
	public void clearAlarms() {
		
	}
	
	public void panicRoom(Room room){
		
	}
	
	public void switchOff(){
		
	}
	
	public void switchOn() {
		
	}
	
	
	private void registerSensors() {
        this.buzzer = new Buzzer();

        this.sensorRepository = new SensorRepositroy();
        this.roomRepository = new RoomRepository();

		Room livingRoom = new Room("Living Room");
		livingRoom.addSensor(new DoorSensor("FrontDoor"));
		livingRoom.addSensor(new WindowSensor("LivingRoomWindow1"));
		livingRoom.addSensor(new MovementSensor("LivingRoomMotion"));

		Room kitchen = new Room("Kitchen");
		kitchen.addSensor(new WindowSensor("KitchenWindow1"));
		kitchen.addSensor(new DoorSensor("BackDoor"));
		
		Room bedroom = new Room("Master Bedroom");
		bedroom.addSensor(new WindowSensor("BedroomWindow1"));
		bedroom.addSensor(new WindowSensor("BedroomWindow2"));
		bedroom.addSensor(new MovementSensor("BedroomMotion"));
		
		Room hallway = new Room("Hallway");
		hallway.addSensor(new MovementSensor("HallwayMotion"));
		
		roomRepository.addRoom(livingRoom);
		roomRepository.addRoom(kitchen);
		roomRepository.addRoom(bedroom);
		roomRepository.addRoom(hallway);

        for (Room r :roomRepository.getRooms()){
            for (Sensor s : r.getSensors()) {
                sensorRepository.addSensor(s);
            }
        }
	}

	private void triggerAlarm(Sensor sensor) {
        this.alarmTriggered = true;
        this.buzzer.switchOn();

        Room room = roomRepository.getRoom(sensor);
        if (room == null) return;
        // Already assigned the room as intruded
        if (room.isIntruded()) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                phoneController.callPolice(room.getName());
            }
        }).start();

        room.switchLightOn();
	}

    private void triggerAlarm(){
        this.alarmTriggered = true;
        this.buzzer.switchOn();

        phoneController.callPolice("POWER");
    }

    private void handleSensorTriggered(Sensor sensor){
        triggerAlarm(sensor);
    }

    private void handleSensorFailure(Sensor sensor){
        Room room = roomRepository.getRoom(sensor);
        if (room == null) return;

        phoneController.callService(room.getName(), sensor.getSensorId());
    }

    private void handlePowerMinorDrop(VoltageChangeEvent event) {
        powerController.enableBackup();
    }

    private void handlePowerMajorDrop(VoltageChangeEvent event) {
        powerController.enableBackup();
        triggerAlarm();
    }

    private void handlePowerVoltageRecovered(VoltageChangeEvent event){
        powerController.disableBackup();
    }

    private void handlePowerFailure(PowerFailureEvent event){
        phoneController.callService("System", event.getFailureSource());
    }

    private void registerEvents() {
        this.sensorEngine = new EsperEngine(
            this::handleSensorTriggered, 
            this::handleSensorFailure
        ); 
        this.powerEngine = new EsperEngine(
            this::handlePowerMinorDrop,
            this::handlePowerMajorDrop,
            this::handlePowerVoltageRecovered,
            this::handlePowerFailure
        );
    }
	
	public BASController(){
		systemIsOn = true;

		registerSensors();
        registerEvents();

        sensorController = new SensorController(this.sensorRepository, this.sensorEngine);
        powerController = new PowerController(this.powerEngine);
		phoneController = new PhoneController();

        sensorController.beginPollCycle();
		
		SwingUtilities.invokeLater(() -> {
            DebugUI ui = new DebugUI(this);
            ui.setVisible(true);
        });
	}
	
	
	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		new BASController();
	}
	
}
