/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator;

import app.ContactsComponentInterface;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Kryword
 * 
 * El segundo tipo de cliente se encargará de verificar si existe un contacto y
 * lo mostrará en la salida.
 */
public class CheckClient implements Runnable{
    final String nickname;
    final int id;

    public CheckClient(int id, final String nickname) {
        this.nickname = nickname;
        this.id = id;
    }



    @Override
    public void run(){
        try {
            ContactsComponentInterface contactsInterface = new ContactsComponentInterface();
            Future<Boolean> future = contactsInterface.isInContacts(nickname);
            Boolean isInContacts = future.get();
            System.out.println("ClientID "+ id +":: <ThreadID:" + Thread.currentThread().getId() + "> Result<isInContacts(" + nickname + ")>:" + isInContacts);
            System.out.flush();
        } catch (InterruptedException ex) {
            System.err.println("Cliente interrumpido.");
            Logger.getLogger(AppServiceSimulator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            System.err.println("Error en la ejecución del cliente.");
            Logger.getLogger(AppServiceSimulator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
