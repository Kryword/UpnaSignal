/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package app;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;

/**
 *
 * @author Kryword
 */
public class ContactsComponent implements Runnable{

    static private final Logger LOGGER = Logger.getLogger(ContactsComponent.class.getName());
  
  private final Subject subject;
  private final ExecutorService executorForTasks;
  
  protected ContactsComponent (final Subject subject, final int numThreads) {
    
    this.subject = subject;
    this.executorForTasks = Executors.newFixedThreadPool(numThreads);
    
  }
  
  @Override
  public void run () {
    
    LOGGER.info("Arranque de componente ContactsComponent");
    
    //System.err.println("\t<ContactsComponent::run          (thread id " + Thread.currentThread().getId() + ")>");
     
    try {
      
      AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
        
        try {
          
          // Ejecutor solo se pone en servicio por el principal autenticado(Administrador)
          return Subject.doAsPrivileged(subject, (PrivilegedExceptionAction<Void>) () -> {
            
            // Bucle para ejecutar concurrentemente tareas de servicio
            do {
              try {
                executorForTasks.awaitTermination(2500, MILLISECONDS);
              } catch (final InterruptedException ex) {
              }
            } while (!executorForTasks.isShutdown());
            
            // Se espera hasta conclusión de tareas de servicio pendientes
            do {
              try {
                executorForTasks.awaitTermination(2500, MILLISECONDS);
              } catch (final InterruptedException ex) {
              }
            } while (!executorForTasks.isTerminated());
                 
            return null;
            
          }, null);
          
        } catch (final PrivilegedActionException ex) {
          LOGGER.log(Level.SEVERE, "", ex);
          
        }
        
        return null;
        
      });
    } catch (final PrivilegedActionException ex) {
      LOGGER.info("");
    }
        
    LOGGER.info("Componente ContactsComponent detenida");

  }
  
  protected void stop () {
    
    LOGGER.info("Parada de componente ContactsComponent");

    try {

      AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {

        // Ejecutor solo se detiene por el administrador autenticado
        return Subject.doAsPrivileged(subject, (PrivilegedExceptionAction<Void>) () -> {

          executorForTasks.shutdown();

          return null;

        }, null);

      });

    } catch (final PrivilegedActionException ex) {
      LOGGER.log(Level.SEVERE, "", ex);
    }
    
  }
  
  protected <T>Future<T> submit (final Callable<T> task) {
    
    //LOGGER.info("Nueva tarea en LastComponentService");
    
    try {

      return AccessController.doPrivileged((PrivilegedExceptionAction<Future<T>>) () -> {

        try {

          return Subject.doAsPrivileged(subject, (PrivilegedExceptionAction<Future<T>>) () -> {
            try {
              return executorForTasks.submit(task);
            } catch (final RejectedExecutionException ex) {
              // Ocurre cuando el servicio ejecutor ha sido ya parado con shutdown()
              LOGGER.log(Level.SEVERE, "Componente ContactsComponent no disponible");
              return null;
            }

          }, null);

        } catch (final PrivilegedActionException ex) {
          LOGGER.log(Level.SEVERE, "", ex);
          return null;
        }
        
      });

    } catch (PrivilegedActionException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return null;
    }

  }
    
}
