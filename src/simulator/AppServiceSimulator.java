/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator;

import app.AppLogin;
import app.ContactsComponentInterface;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import verifier.VerifierCalculator;

/**
 *
 * @author Kryword
 */
public class AppServiceSimulator {
    
    
    public static void main(final String[] args) throws InterruptedException{
        final ExecutorService executorForSimulation = Executors.newFixedThreadPool(8);
        
        AppLogin login = new AppLogin();
        executorForSimulation.execute(login);
        
        // Delay para asegurar que servicio ha terminado fase de arranque de componentes
        final Object X = new Object();
        synchronized (X) {
            X.wait(1500);
        }
        
        // Primer cliente simula un isInContacts con nickname cristian y si no
        // lo está añade un nuevo cliente llamado cristian a la base de datos
        // mediante addContact.
        Runnable client1 = () -> {
            try {
                ContactsComponentInterface contactsInterface = new ContactsComponentInterface();
                Future<Boolean> future = contactsInterface.isInContacts("cristian");
                Boolean isInContacts = future.get();
                System.out.println("Client 1:: <ThreadID:" + Thread.currentThread().getId() + "> Result<isInContacts(cristian)>:" + isInContacts);
                System.out.flush();
                if(!isInContacts){
                    VerifierCalculator vCalculator = new VerifierCalculator();
                    final SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
                    final byte[] salt = new byte[16];
                    sr.nextBytes(salt);
                    Future<Boolean> futureAdd = contactsInterface.addContact("cristian",vCalculator.getVerifier("cristian", "labops", salt));
                    Boolean addContact = futureAdd.get();
                    
                    if(addContact){
                        System.out.println("Client1:: <ThreadID: " + Thread.currentThread().getId() + "> Result<addContact(cristian)>:Añadido correctamente");
                    }else{
                        System.out.println("Client1:: <ThreadID: " + Thread.currentThread().getId() + "> Result<addContact(cristian)>:No ha sido añadido");
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
        };
        
        Runnable client2 = () -> {
            try {
                ContactsComponentInterface contactsInterface = new ContactsComponentInterface();
                Future<Boolean> future = contactsInterface.isInContacts("cristian");
                Boolean result = future.get();
                System.out.println("Client 1:: <ThreadID:" + Thread.currentThread().getId() + "> Result<isInContacts(cristian)>:" + result);
                System.out.flush();
            } catch (InterruptedException ex) {
                System.err.println("Cliente interrumpido.");
                Logger.getLogger(AppServiceSimulator.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                System.err.println("Error en la ejecución del cliente.");
                Logger.getLogger(AppServiceSimulator.class.getName()).log(Level.SEVERE, null, ex);
            }
        };
        
        executorForSimulation.execute(client1);
    }
}
