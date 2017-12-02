package App;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.Test;
import static org.junit.Assert.*;
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
            final Statement statement = connection.createStatement();                    
            final String selectStatement = "SELECT nickname FROM Contacts WHERE nickname = '" + "cris" + "'";

            if (statement.execute(selectStatement)) {
                final ResultSet rs = statement.executeQuery(selectStatement);
                result = rs.next();
                System.out.println("Result = " + result);
            }
        }catch(SQLException | ClassNotFoundException e){
            System.err.println("Error en SQL o Clase del driver");
            e.printStackTrace();
        }
        assertEquals(true, result);
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
