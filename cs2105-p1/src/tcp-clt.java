import java.io.*; 

import java.net.*; 

class TCPClient { 

    public static void main(String argv[]) throws Exception 
    { 
        String sentence; 
        String modifiedSentence; 

        BufferedReader inFromUser = 
          new BufferedReader(new InputStreamReader(System.in)); 

        // Socket clientSocket = new Socket("suna.comp.nus.edu.sg", 6789); 
        Socket clientSocket = new Socket("localhost", 21050); 

        DataOutputStream outToServer = 
          new DataOutputStream(clientSocket.getOutputStream()); 

        BufferedReader inFromServer = new BufferedReader(
          new InputStreamReader(clientSocket.getInputStream())); 

        sentence = inFromUser.readLine(); 
        System.out.println("READ " + sentence); 
        outToServer.writeBytes(sentence + '\n'); 

        modifiedSentence = inFromServer.readLine(); 

        System.out.println("FROM SERVER: " + modifiedSentence); 

        clientSocket.close(); 

    } 
}

 

