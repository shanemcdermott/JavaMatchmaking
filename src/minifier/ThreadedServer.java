/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package minifier;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 *
 * @author unouser
 */
public class ThreadedServer {
    
    public static void main(String[] args) throws IOException {
        new ThreadedServer();
    }

    public ThreadedServer() throws IOException {
        
        System.out.println("Working Directory = " +
              System.getProperty("user.dir"));
        
        ServerSocket serverSocket = new ServerSocket(5001);
        
        
        while(true)
        {
            Socket socket = serverSocket.accept();
            
            StarterSocket runnableSocket = new StarterSocket(socket);
            
            new Thread(runnableSocket).start();
            
            

            
        }
        
        
        
    }
}
    
    
    
