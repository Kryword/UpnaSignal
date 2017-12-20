/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package upnasignal;

import app.ContactsComponentInterface;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;

/**
 *
 * @author Kryword
 */
public class SignalTask {

    static final String INTERACTION = "signal";
    final InetAddress inetAddress;
    final int port;
    DataInputStream in;
    DataOutputStream out;
    final String nickname;
    
    SignalTask(InetAddress inetAddress, int port, String nickname) {
        this.inetAddress = inetAddress;
        this.port = port;
        this.nickname = nickname;
        this.in = null;
        this.out = null;
    }

    SignalTask(DataInputStream in, DataOutputStream out, String nickname) {
        this.in = in;
        this.out = out;
        this.nickname = nickname;
        this.inetAddress = null;
        this.port = -1;
    }

    protected void init(){
        try {
            Socket socket = new Socket(inetAddress, port);
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            
            Messages messages = new Messages();
            // Primera parte del protocolo
            out.writeUTF(messages.getHeadingMessage(nickname, INTERACTION));
            
            String response = in.readUTF();
            Boolean result = messages.parseResultStatusMessage(response);
            Boolean bye = messages.parseBYEMessage(in.readUTF());
            if (!result && bye){
                System.out.println("Cliente cierra conexión.");
                socket.close();
                return;
            }
            
            // Segunda parte del protocolo Calculo de A
            
            
        } catch (IOException ex) {
            Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    protected void accept() {
        try{
            // Primera parte del protocolo. Verifico si el usuario está en la base de datos y si no devuelvo falso
            Messages messages = new Messages();
            ContactsComponentInterface contactsInterface = new ContactsComponentInterface();
            Boolean result = null;
            try{
                Future<Boolean> future = contactsInterface.isInContacts(nickname);
                Boolean isInContacts = future.get();
                result = isInContacts;
            } catch (InterruptedException ex) {
                Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
                result = false;
            } catch (ExecutionException ex) {
                Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
                result = false;
            }finally{
                out.writeUTF(messages.getResultStatusMessage(result));
                if (result){
                    out.writeUTF(messages.getACKMessage());
                }else{
                    out.writeUTF(messages.getBYEMessage());
                    in.close();
                    out.close();
                    return;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(SignalTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
