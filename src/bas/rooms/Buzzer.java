/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bas.rooms;

/**
 *
 * @author shadow
 */
public class Buzzer {
	private boolean isRinging;

    public Buzzer() {
        this.isRinging = false;
    }

    public void switchOn(){
        if (!isRinging)
            isRinging = true;
    }

    public void switchOff(){
        if (isRinging)
            isRinging = false;
    }
	
}
