package app;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;

/**
 *
 * @author Cristian Berner
 */


final class AppImplementation {

  static private final Logger LOGGER = Logger.getLogger(AppImplementation.class.getName());
  
  // Singleton
  static private AppImplementation APP_IMPLEMENTATION = null;
  
  // Sujeto autenticado (administrador)
  private final Subject subject;  
  // Servicio ejecutor para diferentes componentes de servicio en ejecución concurrente
  private final ExecutorService executorForServiceComponents;
  private final ContactsComponent contactsComponent;
  private final UASComponent uasComponent;
  
  

  // Constructor/consultor
  static protected AppImplementation getService (final Subject subject, final int numThreads) {
    
    if (APP_IMPLEMENTATION == null)
      APP_IMPLEMENTATION = new AppImplementation(subject, numThreads);
    
    return APP_IMPLEMENTATION;
    
  }
  
  // Consultor
  static protected AppImplementation getService () {
    return APP_IMPLEMENTATION;
  }  
  
  // Constructor privado
  private AppImplementation (final Subject subject, final int numThreads) {
    
    this.subject = subject;
    // Unico componente del servicio
    // Ejecutor para ejecución concurrente de componentes del servicio
    this.contactsComponent = new ContactsComponent(subject, numThreads);
    this.uasComponent = new UASComponent(subject, numThreads);
    
    this.executorForServiceComponents = Executors.newFixedThreadPool(2);    

  }
  
  // Arranque de componentes de servicio
  protected void start () {
    
    LOGGER.info("Arranque de componentes de servicio");
    
    try {

      // Necesario para poder ejecutar Subject.doAsPrivileged()
      AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {

        try {

          // Ejecutor para componentes solo se pone en servicio por el administrador autenticado
          return Subject.doAsPrivileged(subject, (PrivilegedExceptionAction<Void>) () -> {
            
            // Se ponen en ejecución los componentes ContactsComponent y UASComponent
            executorForServiceComponents.execute(contactsComponent);
            executorForServiceComponents.execute(uasComponent);
            return null;

          }, null);

        } catch (final PrivilegedActionException ex) {
          LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }

        return null;

      });
    } catch (final PrivilegedActionException ex) {
      LOGGER.info(ex.getMessage());
      return;
    }
    
    LOGGER.info("Componentes de servicio arrancadas");    

  }
  
  // Parada de componentes de servicio
  protected void stop () {
    
    LOGGER.info("Parada de componentes de servicio");
    
    try {

      // Necesario para poder ejecutar Subject.doAsPrivileged()
      AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {

        try {

          // Componentes solo detienen bajo peticion del administrador autenticado
          return Subject.doAsPrivileged(subject, (PrivilegedExceptionAction<Void>) () -> {

            // Se ordena parada de las componentes contactsComponent y uasComponent
            contactsComponent.stop();
            uasComponent.stop();

            // Se ordena parada de ejecutor de componentes
            executorForServiceComponents.shutdown();
            
            // Se espera hasta que todas las componentes del servicio se hayan detenido
            do {
              try {
                executorForServiceComponents.awaitTermination(2500, MILLISECONDS);
              } catch (final InterruptedException ex) {
              }
            } while (!executorForServiceComponents.isTerminated());

            return null;

          }, null);

        } catch (final PrivilegedActionException ex) {
          LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }

        return null;

      });
    } catch (final PrivilegedActionException ex) {
      LOGGER.info(ex.getMessage());
      return;
    }
    
    LOGGER.info("Componentes de servicio detenidas");

  }
  
  // Acceso a componente LastToken
  protected ContactsComponent getContactsComponent () {
    return contactsComponent;
  }
  
  protected UASComponent getUASComponent () {
    return uasComponent;
  }
  
  // Acceso a sujeto administrador
  protected Subject getSubject () {
    return subject;
  }
  
}