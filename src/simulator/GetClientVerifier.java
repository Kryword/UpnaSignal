package simulator;

import app.ContactsComponentInterface;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Berner
 * 
 * El tercer tipo de cliente buscará si existe un contacto en la base de datos y
 * de existirlo recuperará su verifier y lo mostrará en la salida.
 */
public class GetClientVerifier implements Runnable{
    final String nickname;
    final int id;

    public GetClientVerifier(int id, final String nickname) {
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
            if(isInContacts){
                Future<byte[]> futureVerifier = contactsInterface.getVerifier(nickname);
                byte[] verifier = futureVerifier.get();
                System.out.println("Client"+ id +":: <ThreadID:" + Thread.currentThread().getId() + ">Result<getVerifier("+ nickname +")>:" + Arrays.toString(verifier));
                System.out.flush();
            }
        } catch (InterruptedException ex) {
            System.err.println("Cliente interrumpido.");
            Logger.getLogger(AppServiceSimulator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            System.err.println("Error en la ejecución del cliente.");
            Logger.getLogger(AppServiceSimulator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
