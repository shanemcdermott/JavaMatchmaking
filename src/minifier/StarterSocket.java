package minifier;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;


public class StarterSocket implements Runnable{
    
    Socket socket;
    PrintWriter out;
    Scanner in;
    
    /**
     * Create an instance of this object with a reference to the socket we need
     * @param inSocket The socket to process
     */
    public StarterSocket(Socket inSocket) {
        this.socket = inSocket;
    }

    /**
     * The actual processing of our socket.
     * We grab the GET header line and then process the provided path
     */
    @Override
    public void run() {            
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new Scanner(socket.getInputStream());
            
            boolean cont = true;
            while(cont & in.hasNextLine()){
                String line = in.nextLine();
                System.out.println("Received: " + line);
                if(line.startsWith("GET "))
                {

                    String[] splits = line.split(" ");
                    String path = splits[1];
                    System.out.println("GET request for " + path);
                    handleRequest(path);
                }
                else if(line.startsWith("POST "))
                {
                    String[] splits = line.split(" ");
                    String path = splits[1];
                    System.out.println("POST submit for " + path);
                    handleSubmit(path);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }  finally {
            out.close();
        }
    }

    /**
     * Do something with the path we got from the GET request
     * @param path The requested path (potentially including variables)
     */
    private void handleRequest(String path){
        
        try {
            path = path.substring(1);
            
            if(path.equals(""))
            {
                path = "index.html";
            }
            
            if(path.equals("index.html"))
            {
                if(isFile(path))
                {
                    String file = slurp(path);
                    handle200(file);
                }
                else
                {
                    handle404();
                }
            }
            else{
                if(path.startsWith("regServer"))
                {
                    try {
                        JSONObject json = new JSONObject();
                        int val = 10;
                        json.put("customInt", val);
                        handle200(json);
                    }catch(Exception e)
                    {
                        e.printStackTrace();
                    }

                }
                else if(path.startsWith("edit/"))
                {
                    String wikiName = path.substring(5);
                    String edit = slurp("edit.html");


                    String content = "";
                    if(isFile(wikiName + ".txt")) {
                        content = slurp(wikiName + ".txt");
                    }
                        String replaced = edit.replaceAll("BATMAN", content);

                        replaced = replaced.replace("ROBIN", wikiName);

                        handle200(replaced);

                }
                else if(path.startsWith("submit/"))
                {
                   
                    path = path.substring(7);
                    
                    
                    ///Write edits variable to file system.
                    String wikiName = path.substring(0, path.indexOf("?"));
                    String parameters = path.substring(path.indexOf("?")+1);
                    String[] keyValue = parameters.split("=");
                    
                    String newContents = keyValue[1];
                    newContents = newContents.replaceAll("\\+", " ");
                    
                    Files.write(Paths.get(wikiName + ".txt"), newContents.getBytes());
                    
                    handle302("/" + wikiName);
                }
                else
                {
                    String generic = slurp("generic.html");

                    if(isFile(path + ".txt"))
                    {
                        String content = slurp(path + ".txt");
                        String replaced = generic.replace("BATMAN", content);

                        replaced = replaced.replace("ROBIN", path);

                        handle200(replaced);
                    }
                    else
                    {
                        handle302("/edit/" + path);
                    }
                }
            }
            
        } catch (IOException ex) {
            Logger.getLogger(StarterSocket.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }

    private void handleSubmit(String path)
    {
        try {
            path = path.substring(1);

            if(path.equals(""))
            {
                path = "index.html";
            }

            if(path.equals("index.html"))
            {
                if(isFile(path))
                {
                    String file = slurp(path);
                    handle200(file);
                }
                else
                {
                    handle404();
                }
            }
            else
            {
                if(path.startsWith("unreal"))
                {
                    try {
                        JSONObject json = new JSONObject();
                        int val = 10;
                        json.put("sessionID", val);
                        handle200(json);
                    }catch(Exception e)
                    {
                        e.printStackTrace();
                    }

                }

            }

        } catch (IOException ex) {
            Logger.getLogger(StarterSocket.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Respond with 404
     */
    private void handle404() {
        String response = "Bad news, couldn't find that page.";
            
            
        out.println("HTTP/1.0 404 Not Found");
        out.println("Content-Type: text/html");
        out.println("Content-Length: " + response.length());

        out.println();
        out.println(response);
    }
    
    /**
     * Respond with a 302 redirect
     * @param redirect The location we should redirect the browser to
     */
    private void handle302(String redirect)
    {
        out.println("HTTP/1.0 302 Found");
        out.println("Location: " +  redirect);
        out.println("");
    }
    
    /**
     * Respond with a 501
     */
    private void handle501()
    {
        out.println("HTTP/1.0 501");
        out.println("");
    }

    /**
     * Respond with a 200 OK
     * @param content The body of the message
     */
    private void handle200(String content) {
        out.println("HTTP/1.0 200 OK");
        out.println("Content-Type: text/html");
        out.println("Content-Length: " + content.length());

        out.println();
        out.println(content);
    }

    private void handle200(JSONObject content)
    {
        String json = content.toString();
        String s = String.format("HTTP/1.0 200 OK\nContent-Type: application/json\nContent-Length: %d\n\n%s", json.length(), content);
       System.out.println("Sending: " + s);
       System.out.println("End Sending transcript");
        out.println(s);
        /* out.println("HTTP/1.0 200 OK");
        out.println("Content-Type: application/json");
        out.println("Content-Length: " + content.length());

        out.println();
        out.println(content);*/
    }


   
    /**
     * Read an entire file into a string
     * @param f The file to read
     * @return The contents of the file as a string
     * @throws IOException 
     */
    private String slurp(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()));
    }
    
    /**
     * Read the entire contents on a file into a string
     * @param f The name of the file
     * @return The contents of the file as a string
     * @throws IOException 
     */    
    private String slurp(String f) throws IOException {
        return slurp(new File(f));
    }

    /**
     * Determine if the string is the path to a file
     * @param path The name of the file
     * @return true if it is a file, false if the file doesn't exist or the path is a directory
     */
    private boolean isFile(String path) {
        File f = new File(path);
        return f.exists() && !f.isDirectory();
    }
}