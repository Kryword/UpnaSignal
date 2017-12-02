/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package app;

import javax.security.auth.Subject;

/**
 *
 * @author kryword
 */
public class UASComponent implements Runnable{
    
    
    public UASComponent(Subject subject, int numThreads){
        
    }
    
    
    @Override
    public void run(){
        System.out.println("Hi I'm an uasComponent");
    }
    
    public void stop(){
    }
}
