package ftpclient;
import java.net.*;
import java.io.*;
public class MultiThreadClientCascade extends Thread { 
   
   public static void main(String [] args) {
      BufferedReader sc = new BufferedReader(new InputStreamReader(System.in)); // user input
      try {
         System.out.print("PORT <SP> <host-port> <CRLF>\n>>> ");
         String[] command;
         Socket client;
         while(true){
            command = sc.readLine().split(" ");							// split user's command
            if (!command[0].equalsIgnoreCase("port")) {					// check if the command is port
               System.out.print("Invalid command!\n>>> ");
            }
            else{
               System.out.println(command[0]+" "+command[1]);
               String[] server = command[1].split(":");                 //  split host and port from : (colon)
               System.out.println("Connecting to " + server[0] + " on port " + server[1]);
               try{
                  client = new Socket(server[0], Integer.parseInt(server[1]));   // connect to the socket 
                  break;
               
               }catch(ConnectException c){
                  System.out.print("Server not found\n>>> ");
               }
            }
         }
    
         try{
           
            new clientThread(client).start();        // spawn a new thread
            
          }catch(SocketTimeoutException e){;}
      
      }catch(IOException e) {
          e.printStackTrace();
      }
     }
}
         
         
 class clientThread extends Thread{         
         
   private static DataOutputStream out ;        // to write to server
      private static DataInputStream in ;       // to read from server
      private static OutputStream dataOs;		// to write to file (stor command)
      private static InputStream dataIs;		// to read from file (retr command)
      private Socket client;					// the socket for server client connection
      private static byte[] buff = new byte[1024];   // the buffer to write and read in file operations
      private BufferedReader sc = new BufferedReader(new InputStreamReader(System.in));  // user input

      public clientThread(Socket client) throws IOException{ // constructor of the clientThread uses the client socket
            this.client = client;
         }
      public void run() {
         try{
         dataOs = client.getOutputStream();
         dataIs = client.getInputStream();

         System.out.println("Just connected to " + client.getRemoteSocketAddress());
         OutputStream outToServer = client.getOutputStream();
         out = new DataOutputStream(outToServer);
         
         out.writeUTF("Hello from " + client.getLocalSocketAddress());
         InputStream inFromServer = client.getInputStream();
         in = new DataInputStream(inFromServer);
         

         String username,response,reply,cmd;
         System.out.println("Login required\n\nUSER <SP> <username> <CRLF>");
         while(true){ // client part of the USER PASS commands
            response = sc.readLine();   // read user input split two. if the first part is  user or pass
            if (response.split(" ")[0].equalsIgnoreCase("user") || response.split(" ")[0].equalsIgnoreCase("pass")) {
               out.writeUTF(response);    // send the whole command to server
            }else {
               System.out.println("No permission. Login Needed.");   // if the first part is not user or pass display error
               continue;
            }
            response = in.readUTF();    // read corresponding server response
            if (response.equals("1")) {
               break;                    	//  == 1 break the while loop, credentials are OK
            }
            else if (response.equals("0")) {
               System.out.println("Bad Command/Credentials !");   //  == 0 , bad credentials
            }
            else {
               System.out.println("\n\nPASS <SP> <password> <CRLF>");   // user is OK so PASS command is needed
            }                                                          // goes back to top of while loop
         }
         System.out.print("Login success\n\nRETR <SP> <pathname> <CRLF>\nSTOR <SP> <pathname> <CRLF>\nLIST [<SP> <pathname>] <CRLF>TYPE <SP> <type-code> <CRLF>\nSTRU <SP> <structure-code> <CRLF>\nMODE <SP> <mode-code> <CRLF>\nPWD  <CRLF>\nNOOP <CRLF>\nQUIT <CRLF>\n\n>>> ");
         while(true){
            cmd = sc.readLine();            // read user input
            out.writeUTF(cmd);				// send user's input to server
            reply = in.readUTF();			// read server's corresponding response
            
            if (reply.equals("")) {			// if the response is space character, print message and break the while loop
               System.out.println("Thank you and Goodbye !");
               break;
            }
            else if (reply.equalsIgnoreCase("portc")) {
               System.out.print("Already connected to server !\n\n>>> "); //to check if the client is connected to the server
            }
            else if (reply.equalsIgnoreCase("store")) { //store command sends sendit string back
               try{
            	   String[] command = cmd.split(" "); //split to get the filenames in order
            	   int len=command.length-1;
            	   fileTransferThread[] threads=new fileTransferThread[100]; //instantiate the thread array 
            	   for(int i=1;i<=len;i++){ //spawn a new thread for every filename
            		   threads[i-1]= new fileTransferThread(client,command[i],0);
            	   }
            	   for (int i=1;i<=len;i++){ //start the thread after we create it for concurrency
            		   threads[i-1].start();
            	   }
                   System.out.print(">>> ");              
               }
               catch(NullPointerException n){ //in case it can't find the file
                  out.writeUTF("0");
                  System.out.println("File not found");
               }
            }
            else if (reply.equalsIgnoreCase("recieve")) { //retr command expects this string
               String[] command = cmd.split(" "); //get the cmd split command to get all filenames
          	   int len=command.length-1;
          	   fileTransferThread[] threads=new fileTransferThread[100]; //create an array for each cmd line input
          	   for(int i=1;i<=len;i++){
          		   threads[i-1]= new fileTransferThread(client,command[i],1);
          	   }
          	   for (int i=1;i<=len;i++){
          		   threads[i-1].start(); //start the thread after we create for maximum concurrency
          	   }
                 System.out.print(">>> ");  
               //  new fileTransferThread(client,cmd.split(" ")[1],1).start();
               //System.out.print(">>> ");
               

            }
            else if (reply.equalsIgnoreCase("inc")) {
               System.out.print("No Command provided !\n\n>>> ");
            }
            else if (reply.equalsIgnoreCase("incr")) {
               System.out.print("Not a valid command !\n\n>>> ");
            }
            else if (reply.equalsIgnoreCase("type")){
               System.out.print("ASCII text");
            }
            else if (reply.equalsIgnoreCase("mode")){
               System.out.print("Stream");
            }
            else{
               System.out.println(reply+"\n");
               System.out.print(">>> ");
            }
         }        
         client.close();       //close the socket
      
      }catch(IOException e){;}
}
   
}

class fileTransferThread extends Thread{ //create a new thread class for data transfer

   private static DataOutputStream out ;
      private static DataInputStream in ;
      private static OutputStream dataOs;
      private static InputStream dataIs;
      private Socket client;
      private static byte[] buff = new byte[1024]; //for the buffer to be read or written at a time
      private BufferedReader sc = new BufferedReader(new InputStreamReader(System.in));
      private String fileName;
      private int transferType;
      public fileTransferThread(Socket client, String fileName,int transferType) throws IOException{// to instantiate the thread with 
            this.client = client; //client port
            this.fileName = fileName; //filename
            this.transferType = transferType; //if it is retr or stor
         }

      public void run(){
         try{
         dataOs = client.getOutputStream(); //create client output stream
         dataIs = client.getInputStream(); //create client input stream

         OutputStream outToServer = client.getOutputStream(); //create server input stream
         out = new DataOutputStream(outToServer); 
         
         InputStream inFromServer = client.getInputStream(); //create server output stream
         in = new DataInputStream(inFromServer);

         if (transferType == 0) //for store
               stor(fileName);
         else if(transferType == 1) //for retr
               retr(fileName);

         }catch( IOException e){;}

      }


   private static boolean stor(String fileName) throws IOException {
      boolean result = false;  //to set returning boolean as false
     // File dir = new File(".");
	 // String path=dir.getAbsolutePath()+"'\'";
      File inFile = new File(fileName); 
      try {
    	  
         FileInputStream fileInputStream = new FileInputStream(inFile); //get the file path 
         out.writeUTF(inFile.length()+"");
         int recv = 0;
         while ((recv = fileInputStream.read(buff, 0, buff.length)) > 0) { //read 1024 bytes a time
            dataOs.write(buff,0,recv);
         }
         dataOs.flush(); //flush memory to stop memory loss
         fileInputStream.close();
         System.out.print("File Transfered successfully !\n\n>>> ");
         
      } catch (FileNotFoundException e) {
         System.out.print("File " + fileName + " was not found!\n\n>>> ");
         out.writeUTF("-1");
      } catch (IOException e) {
         System.out.print("Problem transfering file for put: \n\n>>> " + e);
      }

      return result;
   }


   private static boolean retr(String fileName) throws IOException {
      boolean result = false; //set returning boolean as false
      
      File outFile = new File(fileName);
      try {
         FileOutputStream fileOutputStream = new FileOutputStream(outFile); //get the absolute path of the file
         String response = in.readUTF();
         if (response.equals("-1")) {
             System.out.print("File Not Found!\n\n>>> ");
            return false;
         }
         long size = Long.valueOf(response);
         long len = 0;
         int recv = 0;
         if (size > 0) {
            while (len + recv < size) {
               len += recv;
               recv = dataIs.read(buff,0,buff.length);               
               fileOutputStream.write(buff,0,recv); //write 1024 bytes a time
            }
         }
         fileOutputStream.close();
         System.out.print("File Received successfully !\n\n>>> ");
         
      } catch (FileNotFoundException e) {
         System.out.print("File " + fileName + " was not found!\n\n>>> ");
         out.writeUTF("-1");
      } catch (IOException e) {
         System.out.print("Problem transfering file for put: \n\n>>> " + e);
      }

      
      return result;
   }



}