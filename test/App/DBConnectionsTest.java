package App;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import org.junit.Test;
import static org.junit.Assert.*;
import verifier.VerifierCalculator;
/**
 *
 * @author Kryword
 */
public class DBConnectionsTest {
    static private final String DB_DRIVER = "org.apache.derby.jdbc.ClientDriver";
    static private final String DB_URL = "jdbc:derby://localhost:1527/UpnaSignal";
    static private final String DB_USER = "cristian";
    static private final String DB_PASSWORD = "labops";
    
    /**
     * Test encargado de verificar la funcionalidad de IsInContacts sobre la
     * base de datos mencionada arriba con el nickname cris
     */
    @Test
    public void DBIsInContacts(){
        Boolean result = null;
        try{
            Class.forName("org.apache.derby.jdbc.ClientDriver");
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                            // Hay que cambiar esto por prepared statement en caso de inyección SQL                   
            final String selectStatement = "SELECT nickname FROM Contacts WHERE nickname=?";
            final PreparedStatement preparedStatement = connection.prepareStatement(selectStatement);
            final String nickname = "cristian";
            preparedStatement.setString(1, nickname);
            if (preparedStatement.execute()) {
                final ResultSet rs = preparedStatement.getResultSet();
                result = rs.next();
                System.out.println("isInContacts:" + result);
            }
        }catch(SQLException | ClassNotFoundException e){
            System.err.println("Error en SQL o Clase del driver");
            e.printStackTrace();
        }
        assertEquals(false, result);
    }
    
    
    @Test
    public void DBAddContact() throws NoSuchAlgorithmException{
        Boolean result = null;
        try{
            Class.forName("org.apache.derby.jdbc.ClientDriver");
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            // Hay que cambiar esto por prepared statement en caso de inyección SQL  
            final String insertStatement = "INSERT INTO Contacts(contactkey, nickname, verifier) VALUES (?,?,?)";
            final PreparedStatement preparedInsertStatement = connection.prepareStatement(insertStatement);
            final VerifierCalculator vc = new VerifierCalculator();
            final SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            final byte[] salt = new byte[16];
            final String nickname = "test";
            final String password = "password";
            sr.nextBytes(salt);
            byte[] verifier = vc.getVerifier(nickname, password, salt);
            byte[] key = null;
            try{
                MessageDigest md = MessageDigest.getInstance("SHA1", "BC");
                key = md.digest(nickname.getBytes());
            }catch(NoSuchAlgorithmException| NoSuchProviderException ex){
            }
            preparedInsertStatement.setBytes(1, key);
            preparedInsertStatement.setString(2, nickname);
            preparedInsertStatement.setBytes(3, verifier);
            result = preparedInsertStatement.executeUpdate() > 0;
        }catch(SQLException | ClassNotFoundException e){
            System.err.println("Error en SQL o Clase del driver");
            e.printStackTrace();
        }
        System.out.println("Result from addContact: " + result);
        assertEquals(false, result);
    }
    
    /**
     * Test encargado de verificar que se da la conexión con la base de datos
     * correctamente y sin excepciones.
     */
    @Test
    public void DBConnection(){
        Connection conn = null;  
        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver");    
            //System.out.println("Connecting to database...");    
            conn = DriverManager.getConnection(DB_URL,DB_USER,DB_PASSWORD);
        } catch (ClassNotFoundException | SQLException e) {    
            //e.printStackTrace(); 
            //System.out.println("Esto ha vuelto a fallar");
        } finally {    
            if (conn != null) {
                //System.out.println("Connection correctly completed.");
                try {
                    conn.close();    
                } catch (SQLException e) {    
                    // ignore
                    //System.out.println("SQLException error");
                }    
            }    
        }
        assertNotNull(conn);
    }
    
}
