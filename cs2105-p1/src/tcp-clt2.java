import java.io.*; 
import java.net.*; 

class TCPClient2 { 

    public static void main(String args[]) throws Exception 
    { 
        String sentence; 
        String modifiedSentence; 

	if (args.length != 4) {
		System.out.println("Syntax: hostname port infile outfile");
		System.exit(1);
	}

	String hostname = args[0];
	int port = Integer.parseInt(args[1]);
	String infile = args[2];
	String outfile = args[3];

        BufferedReader inFromUser = 
          // new BufferedReader(new InputStreamReader(System.in)); 
          new BufferedReader(new FileReader(infile)); 

        Socket clientSocket = new Socket(hostname, port); 

        DataOutputStream outToServer = 
          new DataOutputStream(clientSocket.getOutputStream()); 

        BufferedReader inFromServer = new BufferedReader(
          new InputStreamReader(clientSocket.getInputStream())); 

	// Will only read one line from file.
        sentence = inFromUser.readLine(); 
        outToServer.writeBytes(sentence + '\n'); 
        modifiedSentence = inFromServer.readLine(); 

        BufferedWriter outToUser = 
          // new BufferedReader(new InputStreamReader(System.in)); 
          new BufferedWriter(new FileWriter(outfile)); 

	outToUser.write(modifiedSentence);
	outToUser.close();
        clientSocket.close(); 

    } 
}

 

