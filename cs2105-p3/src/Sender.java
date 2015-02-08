
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.io.*;



public class Sender {
	static int pkt_size = 540;
	static int pkt_content_size = 500;
	static int send_interval = 500;
	ArrayList <DatagramPacket> windows;
	ArrayList <Integer> window_seq;
	ArrayList <Timer> window_timer;
	InThread th_in;
	OutThread th_out;
	boolean finished;
	public class OutThread extends Thread {
		private DatagramSocket sk_out;
		private int dst_port;
		private int recv_port;
		private String filePath;
		private String fileName;
		private int hasRead;
		private int total;
		private int remaining;
		InputStream input;
		byte[] file_data;
		private int readNum;
		InetAddress dst_addr;
		byte[] out_data;
		byte [] file_name;
		
		class ResendTask extends TimerTask{
			private int _seq;
			public ResendTask(int seq){
				_seq = seq;
			}
			public void run() {
				try {
					System.out.println("timeout");
					resend(this._seq);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}
		}
		
		public OutThread(DatagramSocket sk_out, int dst_port, int recv_port, String filePath, String fileName) {
			this.sk_out = sk_out;
			this.dst_port = dst_port;
			this.recv_port = recv_port;
			this.fileName = fileName;
			this.filePath = filePath;
			this.hasRead = 0;

		}
		
		public DatagramPacket sendNew() throws IOException{

			remaining = total - hasRead;
			if (remaining<pkt_content_size)
				readNum = remaining;
			hasRead += input.read(file_data,hasRead,readNum);
			out_data = new byte[pkt_size];
			//out_data = Arrays.copyOfRange(file_data, hasRead-readNum, hasRead);
			System.arraycopy(file_data, hasRead-readNum, out_data, 0, readNum);
			out_data = addName();
			out_data = addSeq();
			out_data = addLength();
			out_data = addChecksum();
			
			DatagramPacket out_pkt = new DatagramPacket(out_data, pkt_size ,
				dst_addr, dst_port);
			
			sk_out.send(out_pkt);
			return out_pkt;
		}
		
		public void sendNew(int seq) throws IOException{
			for(int i=0; i<10;i++){
				if(window_seq.get(i) == seq){
					window_timer.get(i).cancel();
					windows.set(i, sendNew());
					window_seq.set(i, hasRead);					
					System.out.println("window: "+i);
					Timer timer = new Timer();
					timer.schedule(new ResendTask(hasRead), send_interval);
					window_timer.set(i, timer);
				}
			}
		}
		
		public void resend(int seq) throws IOException {
			for(int i=0; i<10;i++){
				if(window_seq.get(i) == seq){
					DatagramPacket pkt = windows.get(i);
					sk_out.send(pkt);
					window_timer.get(i).cancel();
					Timer timer = new Timer();
					timer.schedule(new ResendTask(seq), 500);
					window_timer.set(i, timer);
					System.out.println("resend: window "+ i);
					break;
				}
			}
		}
		//0-499
		byte [] addName(){
			byte [] out_data2 = new byte[pkt_size];
			System.arraycopy(out_data, 0, out_data2, 0, readNum);
			System.arraycopy(file_name, 0, out_data2, 500, file_name.length);
			return out_data2;
		}
		
		//520-523 
		byte [] addSeq(){
			byte [] seq = intToByteArray(hasRead);
			System.arraycopy(seq, 0, out_data, 520, 4);
			return out_data;
		}
		//524-528
		byte [] addLength() {
			byte [] length = intToByteArray(total);
			System.arraycopy(length, 0, out_data, 524, 4);
			return out_data;
		}
		
		byte [] addChecksum() {
			Checksum checksum = new CRC32();
			checksum.update(out_data,0,pkt_content_size);
			long lngChecksum = checksum.getValue(); 
			ByteBuffer buffer = ByteBuffer.allocate(8);
		    buffer.putLong(lngChecksum);
		    byte [] checksum2 = buffer.array();
		    System.arraycopy(checksum2, 0, out_data, 530, 8);
		    return out_data;
		}
		
		void openFile() throws FileNotFoundException{
			File file = new File(filePath);
			file_data = new byte[(int) file.length()];
			input = new BufferedInputStream(new FileInputStream(file));			
			readNum = pkt_content_size;
			total = file_data.length;
		}
		
		public byte[] intToByteArray(int a)
		{
		    byte[] ret = new byte[4];
		    ret[3] = (byte) (a & 0xFF);   
		    ret[2] = (byte) ((a >> 8) & 0xFF);   
		    ret[1] = (byte) ((a >> 16) & 0xFF);   
		    ret[0] = (byte) ((a >> 24) & 0xFF);
		    return ret;
		}
		
	
		
		public void run() {
			
			try {

				//byte[] out_data;
				dst_addr = InetAddress.getByName("127.0.0.1");
				windows = new ArrayList<DatagramPacket>();
				window_seq = new ArrayList<Integer>();
				window_timer = new ArrayList<Timer>();
				// To register the recv_port at the UnreliNet first
				
				DatagramPacket out_pkt = new DatagramPacket(
						("REG:" + recv_port).getBytes(),
						("REG:" + recv_port).getBytes().length, dst_addr,
						dst_port);
				sk_out.send(out_pkt);
				
				file_name = fileName.getBytes();
				openFile();
				
				
				System.out.print(file_data.length);
				try {
					for (int i=0; i<10;i++){
						window_seq.add(-1);
					}
					for (int i=0; i<10;i++){
						windows.add(sendNew());
						window_seq.set(i,hasRead);
						Timer timer = new Timer();
						timer.schedule(new ResendTask(hasRead), send_interval);
						window_timer.add(timer);
						
					}
					while(finished || hasRead > total){
						sleep(2000);
						//System.out.println(hasRead);
					}
					
					
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					sk_out.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
	
	
	
	public class InThread extends Thread {
		private DatagramSocket sk_in;

		public InThread(DatagramSocket sk_in) {
			this.sk_in = sk_in;
		}

		public void run() {
			try {
				byte[] in_data = new byte[pkt_content_size];
				DatagramPacket in_pkt = new DatagramPacket(in_data,
						in_data.length);
				try {
					while (finished) {
						sk_in.receive(in_pkt);					
						int seq = byteArrayToInt(in_data,0);
						int ack = in_data[4];
						System.out.println("rcv: "+seq + " "+ack);
						if(ack == 7) {
							if(seq>=0){
								th_out.sendNew(seq);
							} else if(seq == -77){
								for (int i=0; i<10;i++){
									window_timer.get(i).cancel();
								}
								finished = false;
								System.out.println("done!");
								
							}
						} else if(ack == 17) {
							th_out.resend(seq);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					sk_in.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		boolean checkAck(int ack){
			if(ack == 7){
				return true;
			} else {
				return false;
			}
		}
		
		public int byteArrayToInt(byte[] b, int index) 
		{
		    return   b[3+index] & 0xFF |
		            (b[2+index] & 0xFF) << 8 |
		            (b[1+index] & 0xFF) << 16 |
		            (b[0+index] & 0xFF) << 24;
		}
	}

	public Sender(int sk1_dst_port, int sk4_dst_port,String filePath, String fileName) {
		DatagramSocket sk1, sk4;
		System.out.println("sk1_dst_port=" + sk1_dst_port + ", "
				+ "sk4_dst_port=" + sk4_dst_port + ".");
		finished = true;
		try {
			// create sockets
			sk1 = new DatagramSocket();
			sk4 = new DatagramSocket(sk4_dst_port);

			// create threads to process data
			
			th_in = new InThread(sk4);
			th_out = new OutThread(sk1, sk1_dst_port, sk4_dst_port, filePath, fileName);

			th_in.start();
			th_out.start();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	
	
	public static void main(String[] args) {
		// parse parameters
		if (args.length != 4) {
			System.err
					.println("Usage: java TestSender sk1_dst_port, sk4_dst_port");
			System.exit(-1);
		} else
			new Sender(Integer.parseInt(args[0]), Integer.parseInt(args[1]),args[2],args[3]);
	}
}
