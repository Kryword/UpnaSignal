/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package upnasignal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;

/**
 *
 * @author Kryword
 */
final class HeadingMessageProcessor implements Runnable{

    //Constantes
    static final String APODO = "Server";
    
    //Variables globales
    Socket socket;
    
    //Constructores
    protected HeadingMessageProcessor (final Socket socket){
        this.socket = socket;
    }
    
    //Métodos
    @Override
    public void run() {
        try {
            Messages messages = new Messages();
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            String message = in.readUTF();
            
            String[] data = messages.parseHeadingMessage(message);
            final String type = data[0];
            final String nickname = data[1];
            
            if(type.compareTo("register") == 0){
                RegisterTask register = new RegisterTask(in, out, nickname, APODO, socket.getInetAddress());
                register.accept();
            }else if(type.compareTo("signal") == 0){
                SignalTask signal = new SignalTask(in, out, nickname);
                signal.accept();
            }
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(HeadingMessageProcessor.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HeadingMessageProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}
