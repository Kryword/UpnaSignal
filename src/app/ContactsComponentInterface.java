/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package app;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import org.bouncycastle.crypto.digests.SHA1Digest;

/**
 *
 * @author Kryword
 * 
 * Contacts table
 * create table "CRISTIAN".CONTACTS(
 *      CONTACTKEY CHAR(20) FOR BIT DATA not null primary key,
 *      NICKNAME VARCHAR(20) not null,
 *      VERIFIER VARCHAR(1056) FOR BIT DATA not null
 *  )
 */
public class ContactsComponentInterface {
    static private final Logger LOGGER = Logger.getLogger(ContactsComponentInterface.class.getName());
    /**
     * Estos datos deberían estar en un archivo protegido y encriptado
     */
    static private final String DB_DRIVER = "org.apache.derby.jdbc.ClientDriver";
    static private final String DB_URL = "jdbc:derby://localhost:1527/UpnaSignal;";
    static private final String DB_USER = "cristian";
    static private final String DB_PASSWORD = "labops";
    
    
    public Future<Boolean> isInContacts (final String nickname){
        final ContactsComponent contactsComponent = AppImplementation.getService().getContactsComponent();
        final Subject subject = AppImplementation.getService().getSubject();
        final Callable<Boolean> task = new IsInContacts(subject, nickname);
        return contactsComponent.submit(task);
    }
    
    public Future<Boolean> addContact (final String nickname, final byte[] verifier){
        final ContactsComponent contactsComponent = AppImplementation.getService().getContactsComponent();
        final Subject subject = AppImplementation.getService().getSubject();
        final Callable<Boolean> task = new AddContact(subject, nickname, verifier);
        return contactsComponent.submit(task);
    }
    
    public Future<byte[]> getVerifier (final String nickname){
        final ContactsComponent contactsComponent = AppImplementation.getService().getContactsComponent();
        final Subject subject = AppImplementation.getService().getSubject();
        final Callable<byte[]> task = new GetVerifier(subject, nickname);
        return contactsComponent.submit(task);
    }
    
    /**
     * Clase confinada IsInContacts, está encargada de tener el método para
     * acceder a la base de datos y obtener si un contacto ya está registrado
     */
    final class IsInContacts implements Callable<Boolean>{
        private final Subject subject;
        private final String nickname;
        
        protected IsInContacts(final Subject subject, final String nickname){
            this.subject = subject;
            this.nickname = nickname;
        }
        
        @Override
        public Boolean call() throws PrivilegedActionException {
            LOGGER.log(Level.INFO, "\t<IsInContacts::call\t(thread id {0})>", Thread.currentThread().getId());
            return AccessController.doPrivileged((PrivilegedExceptionAction<Boolean>)() ->{
                return Subject.doAsPrivileged(subject, (PrivilegedExceptionAction<Boolean>) () ->{
                    try (final Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                        // Hay que cambiar esto por prepared statement en caso de inyección SQL
                        try (final Statement statement = connection.createStatement()) {
                             
                            final String selectStatement = "SELECT COUNT(1) FROM Contacts WHERE nickname = '" + nickname + "'";

                            if (statement.execute(selectStatement)) {

                                final ResultSet rs = statement.getResultSet();

                                final Boolean result = rs.getBoolean(1);
                                return result;
                            }

                        } catch (final SQLException ex) {
                            LOGGER.log(Level.SEVERE, "error al realizar consulta", ex);
                        }
                        
                    } catch (final SQLTimeoutException ex) {
                        LOGGER.log(Level.SEVERE, "timeout al abrir la conexion", ex);
                    } catch (final SQLException ex) {
                        LOGGER.log(Level.SEVERE, "error al abrir la conexion", ex);
                    }
                    //En caso de que no consiga ejecutarse devuelvo falso.
                    return false;
                }, null);
            });
            
        }
        
    }
    
    /**
     * Clase confinada AddContact, encargada de añadir un nuevo contacto a la
     * base de datos. Devuelve true si se realiza el ingreso correctamente y 
     * false en caso contrario.
     */
    final class AddContact implements Callable<Boolean>{

        private final Subject subject;
        private final String nickname;
        private final byte[] verifier;
        private final byte[] key;
        
        protected AddContact(final Subject subject, final String nickname, final byte[] verifier){
            this.subject = subject;
            this.nickname = nickname;
            this.verifier = verifier;
            SHA1Digest s = new SHA1Digest();
            s.doFinal(nickname.getBytes(), 0);
            key = s.getEncodedState();
        }
        
        @Override
        public Boolean call() throws PrivilegedActionException {
            LOGGER.log(Level.INFO, "\t<AddContact::call\t(thread id {0})>", Thread.currentThread().getId());
            return AccessController.doPrivileged((PrivilegedExceptionAction<Boolean>)() ->{
                return Subject.doAsPrivileged(subject, (PrivilegedExceptionAction<Boolean>) () ->{
                    
                    
                    try (final Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                        
                        final String insertStatement = "INSERT INTO Contacts(key, nickname, verifier) VALUES (?,?,?)";
                        final PreparedStatement preparedInsertStatement = connection.prepareStatement(insertStatement);
                        preparedInsertStatement.setBytes(1, key);
                        preparedInsertStatement.setString(2, nickname);
                        preparedInsertStatement.setBytes(3, verifier);
                        if (preparedInsertStatement.execute()) {

                            final ResultSet rs = preparedInsertStatement.getResultSet();

                            final Boolean result = rs.next();
                            return result;
                        }
                        
                    } catch (final SQLTimeoutException ex) {
                        LOGGER.log(Level.SEVERE, "timeout al abrir la conexion", ex);
                    } catch (final SQLException ex) {
                        LOGGER.log(Level.SEVERE, "error al abrir la conexion", ex);
                    }
                    //En caso de que no consiga ejecutarse devuelvo falso, aunque lo lógico sería volver a intentarlo.
                    return false;
                }, null);
            });
            
        }
        
    }
    
    final class GetVerifier implements Callable<byte[]>{
        
        private final Subject subject;
        private final String nickname;
        
        protected GetVerifier(final Subject subject, final String nickname){
            this.subject = subject;
            this.nickname = nickname;
        }
        
        @Override
        public byte[] call() throws PrivilegedActionException {
            LOGGER.log(Level.INFO, "\t<GetVerifier::call\t(thread id {0})>", Thread.currentThread().getId());
            return AccessController.doPrivileged((PrivilegedExceptionAction<byte[]>)() ->{
                return Subject.doAsPrivileged(subject, (PrivilegedExceptionAction<byte[]>) () ->{
                    
                    
                    try (final Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                        
                        final String insertStatement = "SELECT verifier FROM Contacts WHERE nickname=?";
                        final PreparedStatement preparedInsertStatement = connection.prepareStatement(insertStatement);
                        preparedInsertStatement.setString(1, nickname);
                        if (preparedInsertStatement.execute()) {

                            final ResultSet rs = preparedInsertStatement.getResultSet();
                            final byte[] result;
                            if (rs.next()){
                                result = rs.getBytes("verifier");
                            }else{
                                result = null;
                            }
                            return result;
                        }
                        
                    } catch (final SQLTimeoutException ex) {
                        LOGGER.log(Level.SEVERE, "timeout al abrir la conexion", ex);
                    } catch (final SQLException ex) {
                        LOGGER.log(Level.SEVERE, "error al abrir la conexion", ex);
                    }
                    //En caso de que no consiga ejecutarse devuelvo nulo
                    return null;
                }, null);
            });
            
        }
        
    }
    
    
}
