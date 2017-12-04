package app;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/**
 *
 * @author Cristian Berner
 */
public class AppLogin implements Runnable{
    
    //static public void main (final String[] args) {
    @Override
    public void run () {
    
        try {
          //System.err.println("\t<ServiceLogin::run           (thread id " + Thread.currentThread().getId() + ")>");

            AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {

                final LoginContext lc;
                try {
                    lc = new LoginContext("LTS");
                } catch (final LoginException ex) {
                    System.err.println("No configuration entry to create specified LoginContext");
                    return null;
                } catch (final SecurityException ex) {
                    System.err.println("No permission to create specified LoginContext");
                    return null;
                }

                try {

                    // 1: intento de autenciación.
                    lc.login();

                    // 2: se recupera el sujeto resultante de la autenticación.
                    final Subject subject = lc.getSubject();

                    // 3: servicio sólo arranca si se consigue una autenticación positiva
                    // 3.a: AppImplementation es innaccesible desde fuera del paquete
                    final AppImplementation app = AppImplementation.getService(subject, 10);

                    // 3.b: servicio se arranca como usuario autenticado;
                    //      metodo run() el ejecutor para tareas de servicio
                    app.start();

                    // Interfaz de control de servicio
                    menu();

                    // Parada de servicio; se completan tareas en ejecucion
                    // pero no se admiten más tareas.
                    app.stop();

                } catch (final LoginException ex) {
                    System.err.println("Authentication failed on app start");
                    return null;
                }

                // Se elimina adminstrador autenticado y credenciales asociadas
                try {
                    lc.logout();
                } catch (final LoginException ex) {
                    System.err.println("Cannot remove LoginContext");
                }

                return null;
            });
        } catch (PrivilegedActionException ex) {
          Logger.getLogger(AppLogin.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.exit(0);

    }

    static void menu () {

        final Scanner scanner = new Scanner(System.in);
        int opcion;
        do {

          System.out.println("Options:");
          System.out.println("  0 - Exit");
          System.out.print("Introduce Option:");
          opcion = scanner.nextInt();
          scanner.nextLine();

        } while (opcion != 0);

    }
}
