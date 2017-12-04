package simulator;

import app.AppLogin;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author Cristian Berner
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
        
        
        Runnable client0 = new CheckClient(0, "cristian");
        
        Runnable client1 = new AddClient(1, "cristian", "labops");
        
        Runnable client2 = new CheckClient(2, "cristian");
        
        Runnable client3 = new GetClientVerifier(3, "cristian");
        
        Runnable client4 = new AddClient(4, "paco", "1234");
        
        Runnable client5 = new GetClientVerifier(5, "paco");
        
        executorForSimulation.execute(client0);
        executorForSimulation.execute(client1);
        executorForSimulation.execute(client2);
        executorForSimulation.execute(client3);
        executorForSimulation.execute(client4);
        executorForSimulation.execute(client5);
    }
}
