package minifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author unouser
 */
public class RunnableSocket implements Runnable {

    Socket socket;
    PrintWriter out;
    Scanner in;
    
    public RunnableSocket(Socket inSocket) {
        this.socket = inSocket;
    }

    @Override
    public void run() {
            
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new Scanner(socket.getInputStream());
            
            boolean cont = true;
            while(cont & in.hasNextLine()){
                String line = in.nextLine();
                
                if(line.startsWith("GET "))
                {
                    String[] splits = line.split(" ");
                    String path = splits[1];
                    System.out.println("GET request for " + path);
                    handleRequest(path);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(RunnableSocket.class.getName()).log(Level.SEVERE, null, ex);
        }  finally {
            out.close();
        }
    }

    private void handleRequest(String path){
        
        String actualPath = getPath(path);
        String variables = getVariables(path);
        
        System.out.println(actualPath);
        System.out.println(variables);
        
        if(handleVariables(variables))
        {
            handle302("/");
            return;
        }
        
        if(actualPath.equals("/index.html"))
        {
            
            if(isFile("index.html"))
            {
                try {
                    String toSend = slurp("index.html");
                    toSend = toSend.replace("INTERPOLATE", new String(Files.readAllBytes(new File("table.txt").toPath())));
                    handle200(toSend);
                } catch (Exception ex) {
                    Logger.getLogger(RunnableSocket.class.getName()).log(Level.SEVERE, null, ex);
                    handle501();
                }
            }
            else
                handle404();
        }
        else
        {
            if(isFile("table.txt"))
            {
                try (BufferedReader br = new BufferedReader(new FileReader(new File("table.txt")))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                       if( line.startsWith(actualPath.substring(1) + " @ "))
                       {
                           String destination = line.split("@")[1].trim();
                           handle302(destination);
                           return;
                       }
                    }
                } catch(IOException e)
                {
                    e.printStackTrace();
                    handle501();
                    return;
                }
                
                //If we get here, then the entry wasn't in our table
                handle404();                    
            }
            else
                handle501();
        }
    }

    private void handle404() {
        String response = "Bad news, couldn't find that page.";
            
            
        out.println("HTTP/1.0 404 Not Found");
        out.println("Content-Type: text/html");
        out.println("Content-Length: " + response.length());

        out.println();
        out.println(response);
    }
    
    private void handle302(String redirect)
    {
        out.println("HTTP/1.0 302 Found");
        out.println("Location: " +  redirect);
        out.println("");
    }
    
    private void handle501()
    {
        out.println("HTTP/1.0 501");
        out.println("");
    }

    private void handle200(String content) {
        out.println("HTTP/1.0 200 OK");
        out.println("Content-Type: text/html");
        out.println("Content-Length: " + content.length());

        out.println();
        out.println(content);
    }

    

    private String getPath(String path) {
        
        int index = path.indexOf("?");
        
        if(index != -1) 
            path = path.substring(0, index);
        
        if(path.equals("/"))
            path = "/index.html";
        
        return path;        
    }

    private String getVariables(String path) {
        int index = path.indexOf("?");
        
        if(index == -1) return "";
       
        String variables = "";
        variables = path.substring(index);

        variables = variables.replace("+", " ");
        variables = variables.replace("%40", "@");
        variables = variables.replace("%3A", ":");
        variables = variables.replace("%2F", "/");
        variables = variables.replace("%0D%0A", "\n");
        
        return variables;
    }

    private boolean handleVariables(String variables) {
        if(variables.equals(""))
            return false;
        try {
            variables = variables.substring(variables.indexOf("=") + 1);
            Files.write(Paths.get("./table.txt"), variables.getBytes());
            return true;
        } catch (IOException ex) {
            Logger.getLogger(RunnableSocket.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    private String slurp(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()));
    }
    
    private String slurp(String f) throws IOException {
        return slurp(new File(f));
    }

    private boolean isFile(String indexhtml) {
        File f = new File(indexhtml);
        return f.exists() && !f.isDirectory();
    }
}