import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;


public class GetHostName {
	public static void main(String [] args) throws UnknownHostException, MalformedURLException{
		URL url = new URL("http://www.comp.nus.edu.sg/~girisha/cs2105-p2/testcases/simple_webpage.html");
		String hostname = url.getHost();
		System.out.println(hostname);
	}
}
