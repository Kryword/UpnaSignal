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
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;

/**
 *
 * @author Kryword
 */
public class RegisterTask {

    static final String INTERACTION = "register";
    final InetAddress inetAddress;
    final int port;
    DataInputStream in;
    DataOutputStream out;
    final String nickname;
    
    protected RegisterTask(InetAddress inetAddress, int port, String nickname){
        this.inetAddress = inetAddress;
        this.port = port;
        this.nickname = nickname;
        this.in = null;
        this.out = null;
    }

    protected RegisterTask(DataInputStream in, DataOutputStream out, String nickname) {
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
            final String messageRegister = messages.getHeadingMessage(nickname, INTERACTION);
            out.writeUTF(messageRegister);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(RegisterTask.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(RegisterTask.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    protected void accept() {
        try{
            Messages messages = new Messages();
            ContactsComponentInterface contactsInterface = new ContactsComponentInterface();
            // Manejo la cabecera register, devuelvo verdadero si el cliente no está registrado ya
            Boolean result = null;
            try{
                Future<Boolean> future = contactsInterface.isInContacts(nickname);
                Boolean isInContacts = future.get();
                result = !isInContacts;
            }catch(ExecutionException|InterruptedException e){
                result = false;
            }finally{
                String message = messages.getResultStatusMessage(result);
                out.writeUTF(message);
                if(result){
                    message = messages.getACKMessage();
                }else{
                    message = messages.getBYEMessage();
                }
                out.writeUTF(message);
            }
            
            try {
                // Segunda parte del protocolo. Envio de la pizca de sal
                SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
                byte[] salt = new byte[16];
                sr.nextBytes(salt);
                String message = messages.getBytesMessage(salt);
                out.writeUTF(message);
                String response = in.readUTF();
                Boolean resultSalt = messages.parseResultStatusMessage(response);
                Boolean ack = messages.parseACKMessage(in.readUTF());
                if (!resultSalt && !ack){
                    in.close();
                    out.close();
                    return;
                }
            } catch (NoSuchAlgorithmException ex) {
                System.err.println("Algoritmo para la sal no existente");
            }
            
            // Tercera parte del protocolo. Recepción del verificador
            result = null;
            try{
                String messageVerifier = in.readUTF();
                byte[] verifier = messages.parseBytesMessage(messageVerifier);
                Future<Boolean> futureAdd = contactsInterface.addContact(nickname,verifier);
                Boolean addContact = futureAdd.get();
                result = addContact;
            }catch(IOException e){
                result = false;
            } catch (InterruptedException ex) {
                Logger.getLogger(RegisterTask.class.getName()).log(Level.SEVERE, null, ex);
                result = false;
            } catch (ExecutionException ex) {
                Logger.getLogger(RegisterTask.class.getName()).log(Level.SEVERE, null, ex);
                result = false;
            }finally{
                String response = messages.getResultStatusMessage(result);
                out.writeUTF(response);
                if(result){
                    out.writeUTF(messages.getACKMessage());
                }else{
                    out.writeUTF(messages.getBYEMessage());
                    in.close();
                    out.close();
                    return;
                }
                
            }
            // Despedida
            out.writeUTF(messages.getBYEMessage());
        }catch(ParserConfigurationException e){
            System.err.println("Error con el parser");
        } catch (IOException ex) {
            Logger.getLogger(RegisterTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
