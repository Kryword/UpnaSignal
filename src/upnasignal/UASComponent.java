package upnasignal;

import javax.security.auth.Subject;

/**
 *
 * @author Cristian Berner
 */
public class UASComponent implements Runnable{
    final int port;
    final int numThreads;
    
    public UASComponent(Subject subject, int numThreads, int port){
        this.port = port;
        this.numThreads = numThreads;
    }
    
    
    @Override
    public void run(){
    }
    
    public void stop(){
    }
}
