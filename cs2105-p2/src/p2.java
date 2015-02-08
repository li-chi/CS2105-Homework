import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Vector;

public class p2 {
	
	static Vector<String> sub1;
	static Vector<String> sub2;
	static Vector<String> redir1;
	static Vector<String> redir2;
	
	
	public static void main(String [] args) throws IOException{
		System.out.println("Proxy opens");
		int portNum = Integer.parseInt(args[0]);
		ServerSocket welcomeSocket = new ServerSocket(portNum);
		String fileName1 = new String(args[1]);
		String fileName2 = new String(args[2]);
	
		BufferedReader buff1 = new BufferedReader(new FileReader(fileName1));
		BufferedReader buff2 = new BufferedReader(new FileReader(fileName2));
		
		sub1 = new Vector<String>();
		sub2 = new Vector<String>();
		redir1 = new Vector<String>();
		redir2 = new Vector<String>();
		
		String line;
		while((line=buff1.readLine())!=null){
			sub1.add(getFirstWord(line));
			sub2.add(getSecondWord(line));
		}
		
		while((line=buff2.readLine())!=null){
			redir1.add(getFirstWord(line));
			redir2.add(getSecondWord(line));
		}
		
		while(true) {
			Socket clientSocket = welcomeSocket.accept(); 
			System.out.println("connected");
			proxy_func(clientSocket);
			
		}
	}
	
	public static void proxy_func(Socket clientSocket) throws IOException{
		 OutputStream outputToClient = clientSocket.getOutputStream();
		 BufferedReader readFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

		 String line = readFromClient.readLine();
		 
		 String url = getSecondWord(line);
		 
		 url = redirectURL(url);
		 
		 String hostName = getHostName(url);
		 String pathName = getPathName(url);
		 
		 Socket serverSocket = new Socket(hostName, 80); 
		 System.out.println("This is url: "+hostName);
		 System.out.println("This is path: "+pathName);
		 
		 
		 System.out.println("Write to server: ");
		 DataOutputStream outToServer = 
		          new DataOutputStream(serverSocket.getOutputStream());
		 
		 String firstLine = "GET "+pathName + " HTTP/1.0\n";
		 
	
		 outToServer.writeBytes(firstLine); 
		 System.out.print(firstLine);
		 for (int i=0; i<8; i++){
			 line = readFromClient.readLine();
			 System.out.print(line+'\n');
			 outToServer.writeBytes(line+'\n'); 
		 }
		 //readFromClient.close();

		 outToServer.writeBytes("\n"); 

		 
	
		 InputStream inputFromServer = serverSocket.getInputStream();

		 //outputToServer.write(Content_Length); //forword the HTTP requests to server host.
		 /*
		 BufferedReader br = new BufferedReader(new InputStreamReader(serverSocket.getInputStream())); 
		 String t;
		 while((t = br.readLine()) != null) 
			 System.out.println(t);
		 outputToClient.write(t);
		 br.close();
		 */
		 

		 byte[] data = new byte[1024];
		 int size;
		 //System.out.println("data: "+ inputFromServer.readLine());
		while ((size = inputFromServer.read()) != -1){   //get the response data from server
			 outputToClient.write(size); //forward the data to your firefox
		}
		
		readFromClient.close();
		outputToClient.close();
		outToServer.close();
		inputFromServer.close();
	}
	
	
	public static String getFirstWord(String line) {
		return line.split(" ")[0];
	}
	
	public static String getSecondWord(String line) {
		return line.split(" ")[1];
	}
	
	public static String getHostName(String string_url) throws MalformedURLException{
		URL url = new URL(string_url);
		String hostname = url.getHost();
		return hostname;
	}
	
	public static String getPathName(String string_url) throws MalformedURLException{
		URL url = new URL(string_url);
		String pathname = url.getPath();
		return pathname;
	}
	
	public static String redirectURL(String url){
		int index = redir1.indexOf("http://"+url);
		System.out.println(url);
		if(index == -1)
			return url;
		else 
			return redir2.get(index);
	}
}
