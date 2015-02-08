import java.io.*; 
import java.net.*; 

class DownloadURL {
	 public static void main(String args[]) throws Exception { 
		 String urlAddress = args[0];
		 String fileName = args[1];
		 
		 //add http://
		 if (!urlAddress.regionMatches(0, "http://", 0, 7))
			 urlAddress = "http://".concat(urlAddress);

		 //open or create file
		 File myFile = new File(fileName);
			if (!myFile.exists())
				myFile.createNewFile();
		 FileOutputStream output = new FileOutputStream(myFile);
		 
		 //create connection
		 URL url = new URL(urlAddress);

		 try{ 
			 //write to file
			 InputStream inn = url.openStream();
			 int tempByte;
			 while((tempByte = inn.read()) != -1){
				 output.write(tempByte);
			 }
			 output.close();
			 System.out.println("Successful");
			 
		 } catch (FileNotFoundException e){
			 //page not found
			 System.out.println("404");
			 BufferedOutputStream buff= new BufferedOutputStream(output);
			 buff.write("404".getBytes());
			 buff.flush();   
	         buff.close();
		 } 		 
	 }
}
