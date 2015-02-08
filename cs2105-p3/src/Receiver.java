import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class Receiver {
	static int pkt_size = 540;
	static int content_size = 500;
	static int out_size = 10;
	int seq,totalLength,hasWrite=0;
	Vector<byte []> storeBuff = new Vector<byte []>();
	Vector<Integer> storeSeq = new Vector <Integer>();
	FileOutputStream output = null;
	
	FileOutputStream createFile(String filePathName) throws IOException {
		File myFile = new File(filePathName);
		if (!myFile.exists())
			myFile.createNewFile();
		FileOutputStream output = new FileOutputStream(myFile);
		return output;
	}
	
	void buffWrite() throws IOException {
		int index = storeSeq.indexOf(hasWrite+content_size);
		if(index>=0){
			output.write(storeBuff.get(index));
			hasWrite = hasWrite+content_size;
			storeBuff.remove(index);
			storeSeq.remove(index);
			buffWrite();
		}
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
	
	public int byteArrayToInt(byte[] b, int index) 
	{
	    return   b[3+index] & 0xFF |
	            (b[2+index] & 0xFF) << 8 |
	            (b[1+index] & 0xFF) << 16 |
	            (b[0+index] & 0xFF) << 24;
	}
	
	boolean checksumOk(byte [] data) {
		Checksum checksum = new CRC32();
		checksum.update(data,0,content_size);
		long longChecksum = checksum.getValue(); 
		ByteBuffer buffer = ByteBuffer.allocate(8);
	    buffer.putLong(longChecksum);
	    byte [] newChecksum = buffer.array();
	    byte [] oldChecksum = new byte[8];
	    System.arraycopy(data, 530, oldChecksum, 0, 8);
	    //byte [] oldChecksum = Arrays.copyOfRange(data, 530, 538);
	    return Arrays.equals(newChecksum,oldChecksum);
	}
	
	public Receiver(int sk2_dst_port, int sk3_dst_port, String filePath) throws IOException {
		DatagramSocket sk2, sk3;
		System.out.println("sk2_dst_port=" + sk2_dst_port + ", "
				+ "sk3_dst_port=" + sk3_dst_port + ".");
		
		String fileName,filePathName;
		boolean finishSignal = true;

		boolean fileCreated = false;
		
		// create sockets
		try {
			sk2 = new DatagramSocket(sk2_dst_port);
			sk3 = new DatagramSocket();
			try {
				byte[] in_data = new byte[pkt_size];
				byte[] out_data = new byte[out_size];
				InetAddress dst_addr = InetAddress.getByName("127.0.0.1");
				int i = 0;
				while (finishSignal) {
					// receive packet
					in_data = new byte[pkt_size];
					DatagramPacket in_pkt = new DatagramPacket(in_data,
							pkt_size);
					sk2.receive(in_pkt);
					if (checksumOk(in_data)){
						byte [] temp_data = new byte[content_size];
						//temp_data = Arrays.copyOfRange(in_data, 0, content_size);	
						System.arraycopy(in_data, 0, temp_data, 0, content_size);
						seq = byteArrayToInt(in_data, 520);
						System.out.println("seq: "+seq+" hasWrite before : "+hasWrite);
						
						if(fileCreated == false){
							byte [] fileNameByte = new byte[100];
							System.arraycopy(in_data, 500, fileNameByte, 0, 20);
							//seq = byteArrayToInt(in_data, 520);
							fileName = new String(fileNameByte);
							filePathName = filePath.concat(fileName);
							System.out.println(filePathName);
							fileCreated = true;
							output = createFile(filePathName);
							totalLength = byteArrayToInt(in_data, 524);
							
						}
						
						if (seq>hasWrite) {
							if(seq<=hasWrite+content_size){
								output.write(temp_data);
								hasWrite = hasWrite + content_size;
								buffWrite();
							}
							else {
								if(!storeSeq.contains(seq)){
									storeSeq.add(seq);
									storeBuff.add(temp_data);
								}
								
							}
						}
						
						
						System.out.println(in_pkt.getLength()+ " "+ i);
						System.out.println("hasWrite: "+hasWrite);
						// send received packet
						//byte [] seqByte = new byte[4];
						//System.arraycopy(in_data, 500, seqByte, 0, 4);
						System.arraycopy(in_data, 520, out_data, 0, 4);
						byte [] ack = new byte[1];
						ack[0] = (byte) 7;
						System.arraycopy(ack, 0, out_data, 4, 1);
						
						DatagramPacket out_pkt = new DatagramPacket(out_data,
								out_data.length, dst_addr, sk3_dst_port);
						sk3.send(out_pkt);
						if(hasWrite >= totalLength){
							finishSignal = false;
							out_data = intToByteArray(-77);
							out_pkt = new DatagramPacket(out_data,
									out_data.length, dst_addr, sk3_dst_port);
							for(int j=0 ;j<10;j++)
								sk3.send(out_pkt);
						}
					} else {
						System.out.println("corrupt");
						//byte [] seqByte = new byte[4];
						//System.arraycopy(in_data, 520, seqByte, 0, 4);
						System.arraycopy(in_data, 520, out_data, 0, 4);
						byte [] nack = new byte[1];
						nack[0] = (byte) 17;
						System.arraycopy(nack, 0, out_data, 4, 1);
						
						DatagramPacket out_pkt = new DatagramPacket(out_data,
								out_data.length, dst_addr, sk3_dst_port);
						sk3.send(out_pkt);
					}	
				}
				
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			} finally {
				sk2.close();
				sk3.close();
				output.flush();
				output.close();
			}
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public static void main(String[] args) throws NumberFormatException, IOException {
		// parse parameters
		if (args.length != 3) {
			System.err
					.println("Usage: java TestReceiver sk2_dst_port, sk3_dst_port");
			System.exit(-1);
		} else
			new Receiver(Integer.parseInt(args[0]),
					Integer.parseInt(args[1]),args[2]);
	}
}
