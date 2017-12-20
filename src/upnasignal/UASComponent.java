package upnasignal;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;

/**
 *
 * @author Cristian Berner
 */
public class UASComponent implements Runnable{
    final int port;
    final int numThreads;
    Boolean isRunning;
    
    static private final Logger LOGGER = Logger.getLogger(UASComponent.class.getName());
    
    public UASComponent(Subject subject, int numThreads, int port){
        this.port = port;
        this.numThreads = numThreads;
        isRunning = true;
    }
    
    
    @Override
    public void run(){
        try(final ServerSocket serverSocket = new ServerSocket(port)){
            final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            
            final Callable<Socket> waitConnectionTask = () -> serverSocket.accept();
            
            while (isRunning){
                Socket socket = null;
                final Future<Socket> future = executor.submit(waitConnectionTask);
                do{
                    try{
                        socket = future.get(100, MICROSECONDS);
                    }catch(ExecutionException e){
                        LOGGER.log(Level.SEVERE, "Socket dejó de ejecutarse");
                    }catch(InterruptedException e){
                        LOGGER.log(Level.SEVERE, "Socket Interrumpido");
                    }catch(TimeoutException e){
                        LOGGER.log(Level.SEVERE, "Socket Tiemout");
                    }
                }while(!future.isDone() && isRunning);
                
                if (future.isDone()){
                    LOGGER.info("Mensaje entrante");
                }
            }
            executor.shutdown();
        } catch (IOException ex) {
            Logger.getLogger(UASComponent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void stop(){
        isRunning = false;
    }
}
