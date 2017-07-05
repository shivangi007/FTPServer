//package ftpclient;
import java.net.*;
import java.io.*;
import java.util.*;

public class newServer extends Thread {
   private static ServerSocket serverSocket;
   static private Socket server;
   
   static HashMap<String, Integer> users = new HashMap<String, Integer>();
   public static void serverr(int port) throws IOException {
      serverSocket = new ServerSocket(port);
      serverSocket.setSoTimeout(30000);
   }

   
   public static void main(String [] args) throws IOException {
      int port = Integer.parseInt(args[0]);
      try {
         serverr(port); //connection succesful 
         System.out.println("Connection Successful!! Waiting for client on port " + 
            serverSocket.getLocalPort() + "..."); //gives the port the class is instantiated with, same for client
         
      }catch(IOException e) {
         e.printStackTrace();
      }
      while(true){
         try{
            server = serverSocket.accept(); //for the socket connection to remai open
            new serverThread(server).start();
         }
         catch(SocketTimeoutException e){;} //in case the socket goes idle for too long
      }
   }
}

class serverThread extends Thread {
   private Socket server;
   private InputStream dataIs;
   private DataInputStream in;
   private DataOutputStream out;
   private OutputStream dataOs;
   public serverThread(Socket sserver) throws IOException{
      this.server = sserver;
   }

   public void run() {
      while(true) {
         try {
            HashMap<String, String> hm = new HashMap<String,String>();  // Eligible users and their passwords are stored in a hash table
            hm.put("admin","12345");
            hm.put("sumeyye","12345");
            hm.put("jasleen","12345");
            hm.put("shivangi","12345");
            File dir;
            String command,response;
            Boolean exitFlag=false;
            BufferedReader sc = new BufferedReader(new InputStreamReader(System.in));    
            
            System.out.println("Just connected to " + server.getRemoteSocketAddress()); //gets  the address connected to
            in = new DataInputStream(server.getInputStream());
            
            System.out.println(in.readUTF());
            out = new DataOutputStream(server.getOutputStream());
            dataOs = server.getOutputStream();
            while(true){                   //  USER PASS commands server side implementation
               String[] credUser = in.readUTF().split(" ");            //  read the whole command (i.e. user admin ) that the client has sent and split from space character
               switch (credUser[0].toLowerCase()) {						// switch on the command
                  case "user" : 
                     if (hm.containsKey(credUser[1])) {					//  if given username exist in the hash table, send space character to client so that
                        out.writeUTF("");								//  they are prompted to perform PASS  command
                        String[] credPass = in.readUTF().split(" ");    //  read from client (their PASS command) and split from space character
                        if (credPass[0].equalsIgnoreCase("pass")) {     //  if the command is pass, check if the password is correct
                           
                           if (hm.get(credUser[1]).equals(credPass[1])) {   //  hm.get(credUser[1]) retrieve stored password for the username, compare with provided one 
                              out.writeUTF("1");							// passwords match, credentials are OK so send client 1 character	
                              exitFlag=true;								// make the exitFlag true
                           }
                           else {
                              out.writeUTF("0");						//    if the passwords don't match , send 0 to client for bad credential.
                           }
                        }
                        else {
                           out.writeUTF("0");						//  if the command is not PASS,   send 0 to client for bad credential.
                        }
                     }
                     else{									// username doesn't exist in the table, send 0 to client for bad credential.
                        out.writeUTF("0");
                     }
                     break;									
                  case "pass" :                 //  you should first enter the USER command before PASS
                     out.writeUTF("0");
                     break;

               }
               if (exitFlag) {						//     if exitFlag is true, break the while loop
                  break;
               }
            }

            while(true){
               exitFlag = false;                                // exitFlag is false
               String[] cmd = in.readUTF().split(" ");			//  split  the command received from the client 
               switch (cmd[0].toLowerCase()) {					// switch on command and send a message to the client according to the command
                  case "port" :
                     out.writeUTF("portc");
                     break;
                  case "retr" : //to retrieve the file from server
                     out.writeUTF("recieve");  //in order to continue the client process
                     int lengt=cmd.length-1;   //cmd line can have more than one files
                     for(int k=1;k<=lengt;k++){  //create a new thread for a new file
                       File sfile = new File(cmd[k]);
                          byte[] buff = new byte[4096];
                          try{
                             InputStream fileStream = new FileInputStream(sfile); //file is saved in a new filestream
                             out.writeUTF(sfile.length()+"");
                             int recv;            
                             while ((recv = fileStream.read(buff, 0, buff.length)) > 0) { //file written in client 4096 bytes a time
                                dataOs.write(buff,0,recv);
                             }
                          }
                          catch(NullPointerException n){
                             out.writeUTF("-1");
                          }
                     }
                   
                     break;
                  case "stor" :      //stores the file to server
                     out.writeUTF("store");  // for the client to proceed
                     int leng=cmd.length-1;  //cmd line can have more than one parameter
                     for(int s=1;s<=leng;s++){ // to create a separate connection for each file
                     DataInputStream dis = new DataInputStream(server.getInputStream()); 
                     FileOutputStream fos = new FileOutputStream(cmd[s]);
                        byte[] buffer = new byte[4096];
                        dataIs = server.getInputStream(); //get the server stream to write file

                        File outFile = new File(cmd[s]);       
                        OutputStream fileStream = new FileOutputStream(outFile);
                        
                        //read and write the file data
                        long len = 0L;
                        response = in.readUTF();
                        if (response.equals("-1")) {
                           break;
                        }
                        long size = Long.valueOf(response);
                        int recv = 0;
                        if (size > 0) {
                           while(len + recv < size) {
                              len += recv;
                              recv = dataIs.read(buffer, 0, buffer.length); //data is read 4096 bytes at a time
                              fileStream.write(buffer,0,recv);
                           }
                        }
                        fileStream.close();
                     }
                     
                 
                     break;
                  case "noop" : //used as a ping for server to check if alive
                     out.writeUTF("OK");  //sends the client a string
                     break;
                           
                  case "" :
                     out.writeUTF("inc");
                     break;
                  case "quit" : 					// if the command is quit, send the client a space character
                     out.writeUTF("");
                     exitFlag=true;    				// set exitFlag to true 
                     break;
                  case "type":
                 	 out.writeUTF("type");
                     break;
           		  case "mode":
                     out.writeUTF("mode");
                     break;  
                  case "pwd":
                     dir = new File(".");    //creates a dummy directory for present loc
                     out.writeUTF("\nAbsolute path: " + dir.getAbsolutePath()); //gets the absolute path of the dummy directory
                     break;
                  case "list":     //lists all files as the unix command ls
                     dir=null;
                     try{
                        dir = new File(cmd[1]);
                        StringBuilder sb = new StringBuilder();
                        sb.append("\nName: " + dir.getName());
                        sb.append("\nAbsolute path: " + dir.getAbsolutePath());
                        sb.append("\nSize: " + dir.length());
                        sb.append("\nLast modified: " + dir.lastModified());
                        out.writeUTF(sb.toString());
                        break;
                     }
                     catch(NullPointerException n){   //for no files in the directory
                        out.writeUTF("File not found!");
                        break;
                     }
                     catch(ArrayIndexOutOfBoundsException a){
                        dir = new File(".");
                        StringBuilder sb = new StringBuilder();
                        File[] filesList = dir.listFiles();
                        for (File file : filesList) {
                            if (file.isFile()) {
                                sb.append(file.getName()+"\n");
                            }
                        }
                        out.writeUTF(sb.toString());
                     }
                     break;
                  default :
                     out.writeUTF("incr");
               }    
               if (exitFlag) {
                  break; 
               }           
            }

            server.close();
            break;
         }catch(SocketTimeoutException s) {
            System.out.println("Socket timed out!"); //if the socket sits idle for too long
            break;
         }catch(IOException e) {
            e.printStackTrace();
            break;
         }
      }

   }

}