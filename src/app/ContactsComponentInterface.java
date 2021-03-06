package app;

import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;

/**
 *
 * @author Cristian Berner
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
    
    public Future<Boolean> addContact (final String nickname, final byte[] verifier, final String myNickname, final byte[] ipAddress, final byte[] salt){
        final ContactsComponent contactsComponent = AppImplementation.getService().getContactsComponent();
        final Subject subject = AppImplementation.getService().getSubject();
        final Callable<Boolean> task = new AddContact(subject, nickname, verifier, myNickname, ipAddress, salt);
        return contactsComponent.submit(task);
    }
    
    public Future<byte[]> getVerifier (final String nickname){
        final ContactsComponent contactsComponent = AppImplementation.getService().getContactsComponent();
        final Subject subject = AppImplementation.getService().getSubject();
        final Callable<byte[]> task = new GetVerifier(subject, nickname);
        return contactsComponent.submit(task);
    }
    
    public Future<DataToSendSignal> getDataToSendSignal (final String nickname){
        final ContactsComponent contactsComponent = AppImplementation.getService().getContactsComponent();
        final Subject subject = AppImplementation.getService().getSubject();
        final Callable<DataToSendSignal> task = new GetDataToSendSignal(subject, nickname);
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
                        final String selectStatement = "SELECT nickname FROM Contacts WHERE nickname=?";
                        try (final PreparedStatement preparedStatement = connection.prepareStatement(selectStatement);) {
                            preparedStatement.setString(1, nickname);
                            if (preparedStatement.execute()) {
                                final ResultSet rs = preparedStatement.getResultSet();
                                return rs.next();
                            }

                        } catch (final SQLException ex) {
                            LOGGER.log(Level.SEVERE, "error al realizar consulta", ex);
                        }
                        
                    } catch (final SQLTimeoutException ex) {
                        LOGGER.log(Level.SEVERE, "timeout al abrir la conexion", ex);
                    } catch (final SQLException ex) {
                        LOGGER.log(Level.SEVERE, "error al abrir la conexion", ex);
                    }
                    //En caso de que no consiga ejecutarse devuelvo null.
                    return null;
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
        private final String myNickname;
        private final byte[] ipAddress;
        private final byte[] salt;
        private final byte[] key;
        
        protected AddContact(final Subject subject, final String nickname, final byte[] verifier, final String myNickname, final byte[] ipAddress, final byte[] salt){
            this.subject = subject;
            this.nickname = nickname;
            this.verifier = verifier;
            this.myNickname = myNickname;
            this.ipAddress = ipAddress;
            this.salt = salt;
            byte[] tempKey = null;
            try{
                MessageDigest md = MessageDigest.getInstance("SHA1", "BC");
                tempKey = md.digest(nickname.getBytes());
            }catch(NoSuchAlgorithmException| NoSuchProviderException ex){
                LOGGER.log(Level.SEVERE, "Algoritmo o proveedor incorrectos o inexistentes", ex);
            }finally{
                key = tempKey;
            }
        }
        
        @Override
        public Boolean call() throws PrivilegedActionException {
            LOGGER.log(Level.INFO, "\t<AddContact::call\t(thread id {0})>", Thread.currentThread().getId());
            return AccessController.doPrivileged((PrivilegedExceptionAction<Boolean>)() ->{
                return Subject.doAsPrivileged(subject, (PrivilegedExceptionAction<Boolean>) () ->{
                    
                    
                    try (final Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                        
                        final String insertStatement = "INSERT INTO Contacts(contactkey, nickname, verifier, myNickname, ipAddress, salt) VALUES (?,?,?,?,?,?)";
                        final PreparedStatement preparedInsertStatement = connection.prepareStatement(insertStatement);
                        preparedInsertStatement.setBytes(1, key);
                        preparedInsertStatement.setString(2, nickname);
                        preparedInsertStatement.setBytes(3, verifier);
                        preparedInsertStatement.setString(4, myNickname);
                        preparedInsertStatement.setBytes(5, ipAddress);
                        preparedInsertStatement.setBytes(6, salt);
                        return preparedInsertStatement.executeUpdate() > 0;
                        
                    } catch (final SQLTimeoutException ex) {
                        LOGGER.log(Level.SEVERE, "timeout al abrir la conexion", ex);
                    } catch (final SQLException ex) {
                        LOGGER.log(Level.SEVERE, "error al abrir la conexion", ex);
                    }
                    //En caso de que no consiga ejecutarse devuelvo null.
                    return null;
                }, null);
            });
            
        }
        
    }
    /**
     * Clase confinada encargada de obtener el verificador del usuario
     * No hace ninguna comprobación de la existencia del usuario en la base de
     * datos
     */
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
    
    /**
     * Clase confinada encargada de obtener los datos necesarios para hacer el
     * mensaje de signal. Obtiene nickname propio, sal y dirección ip y las junta
     * en una estructura DataToSendSignal. Devuelve esta estructura como resultado
     */
    final class GetDataToSendSignal implements Callable<DataToSendSignal>{
        
        private final Subject subject;
        private final String nickname;
        
        protected GetDataToSendSignal(final Subject subject, final String nickname){
            this.subject = subject;
            this.nickname = nickname;
        }
        
        @Override
        public DataToSendSignal call() throws PrivilegedActionException {
            LOGGER.log(Level.INFO, "\t<GetDataToSendSignal::call\t(thread id {0})>", Thread.currentThread().getId());
            return AccessController.doPrivileged((PrivilegedExceptionAction<DataToSendSignal>)() ->{
                return Subject.doAsPrivileged(subject, (PrivilegedExceptionAction<DataToSendSignal>) () ->{
                    
                    
                    try (final Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                        
                        final String insertStatement = "SELECT verifier FROM Contacts WHERE nickname=?";
                        final PreparedStatement preparedInsertStatement = connection.prepareStatement(insertStatement);
                        preparedInsertStatement.setString(1, nickname);
                        if (preparedInsertStatement.execute()) {

                            final ResultSet rs = preparedInsertStatement.getResultSet();
                            final DataToSendSignal result;
                            if (rs.next()){
                                final String nick = rs.getString("myNickname");
                                final byte[] salt = rs.getBytes("salt");
                                final byte[] ip = rs.getBytes("ipAdress");
                                result = new DataToSendSignal(nick, salt, ip);
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
