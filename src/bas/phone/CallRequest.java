/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bas.phone;

/**
 *
 * @author lenovo
 */
public class CallRequest {
  private CallType type;
    private String callMsg; 

    public void setCallMsg(String callMsg) {
        this.callMsg = callMsg;
    }

    public void setType(CallType type) {
        this.type = type;
    }

    public String getCallMsg() {
        return callMsg;
    }

    public CallType getType() {
        return type;
    }
    
      
}
