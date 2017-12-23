package simulator;

import app.ContactsComponentInterface;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import verifier.VerifierCalculator;

/**
 *
 * @author Cristian Berner
 */

/**
* Primer tipo de cliente simula un isInContacts con nickname y si no encuentra
* al usuario añade un nuevo cliente llamado nickname a la base de datos
* mediante addContact.
*/
public final class AddClient implements Runnable{
    final String pass;
    final String nickname;
    final int id;
    final String myNickname;
    final byte[] ipAddress;
    final byte[] salt;

    public AddClient(int id, final String nickname, final String password, final String myNickname, final byte[] ipAddress, final byte[] salt) {
        this.nickname = nickname;
        this.pass = password;
        this.id = id;
        this.myNickname = nickname;
        this.ipAddress = ipAddress;
        this.salt = salt;
    }



    @Override
    public void run(){
        try {
            ContactsComponentInterface contactsInterface = new ContactsComponentInterface();
            Future<Boolean> future = contactsInterface.isInContacts(nickname);
            Boolean isInContacts = future.get();
            System.out.println("ClientID "+ id +":: <ThreadID:" + Thread.currentThread().getId() + "> Result<isInContacts(" + nickname + ")>:" + isInContacts);
            System.out.flush();
            if(!isInContacts){
                VerifierCalculator vCalculator = new VerifierCalculator();
                final SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
                final byte[] salt = new byte[16];
                sr.nextBytes(salt);
                Future<Boolean> futureAdd = contactsInterface.addContact(nickname,vCalculator.getVerifier(nickname, pass, salt), myNickname, ipAddress, salt);
                Boolean addContact = futureAdd.get();
                if(addContact){
                    System.out.println("ClientID " + id + ":: <ThreadID:" + Thread.currentThread().getId() + "> Result<addContact(" + nickname + ")>: Añadido correctamente");
                }else{
                    System.out.println("ClientID " + id + ":: <ThreadID:" + Thread.currentThread().getId() + "> Result<addContact(" + nickname + ")>: No ha sido añadido");
                }
            }
        } catch (InterruptedException ex) {
            System.err.println("Cliente interrumpido.");
            Logger.getLogger(AppServiceSimulator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            System.err.println("Error en la ejecución del cliente.");
            Logger.getLogger(AppServiceSimulator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            System.err.println("Error: Algoritmo para generación de números aleatorios inexistente");
            Logger.getLogger(AppServiceSimulator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
