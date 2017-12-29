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
import static java.util.concurrent.TimeUnit.MILLISECONDS;
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
            final ExecutorService executorForSockets = Executors.newFixedThreadPool(numThreads);
            final ExecutorService executorForInputMessages = Executors.newSingleThreadExecutor();
            final Callable<Socket> waitConnectionTask = () -> serverSocket.accept();
            
            while (isRunning){
                Socket socket = null;
                final Future<Socket> future = executorForSockets.submit(waitConnectionTask);
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
                    final HeadingMessageProcessor hMessage = new HeadingMessageProcessor(socket);
                    executorForInputMessages.submit(hMessage);
                }
            }
            executorForSockets.shutdown();
            executorForInputMessages.shutdown();
                int j = 0;
                do {
                    boolean x;
                    try {
                        x = executorForInputMessages.awaitTermination(2500, MILLISECONDS);
                    } catch (final InterruptedException ex) {
                        break;
                    }
                    LOGGER.log(Level.INFO, "** Intento {0} {1}", new Object[]{j, x});
                    ++j;
                } while (!executorForInputMessages.isTerminated());
        } catch (IOException ex) {
            Logger.getLogger(UASComponent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void stop(){
        isRunning = false;
    }
}
