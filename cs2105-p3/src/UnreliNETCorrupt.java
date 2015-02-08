import java.net.*;
import java.util.*;

public class UnreliNETCorrupt {
	static int buf_size = 1500;
	static float corrupt_pct = 0.1f;
	static float byte_corrupt_pct = 0.3f;
	static HashSet<Integer> registeredSenders;

	// define thread which is used to handle one-direction of communication
	public class UnreliThread extends Thread {
		private DatagramSocket sk_in, sk_out;
		private Random rnd = new Random();
		private Random rnd_byte = new Random();
		private HashSet<Integer> dst_port_list;

		public UnreliThread(DatagramSocket in, DatagramSocket out,
				HashSet<Integer> dpList) {
			sk_in = in;
			sk_out = out;
			dst_port_list = dpList;
		}

		public void run() {
			try {
				byte[] in_data = new byte[buf_size];
				InetAddress dst_addr = InetAddress.getByName("127.0.0.1");
				DatagramPacket in_pkt = new DatagramPacket(in_data,
						in_data.length);

				while (true) {
					// read data from the incoming socket
					sk_in.receive(in_pkt);

					// To initialize the acknowledgment port number
					String receivedString = new String(in_pkt.getData(), 0, in_pkt.getLength()).trim();
					
					// New register sender
					if (receivedString.startsWith("REG:")) {
						try {
							String[] portString = receivedString.split(":");
							if (portString.length != 2) {
								System.out.println("Port String Error");
								continue;
							}
							// Add the dst port to the registered send list
							registeredSenders.add(Integer
									.parseInt(portString[1]));

						} catch (Exception e) {
							e.printStackTrace();
							System.out.println("Registration Failed");
						}
						continue;
					}

					// check the length of the packet
					if (in_pkt.getLength() > 1000) {
						System.err.println("Error: received packet of length "
								+ in_pkt.getLength() + " from "
								+ in_pkt.getAddress().toString() + ":"
								+ in_pkt.getPort());
						System.exit(-1);
					}

					// decide if to corrupt the packet or not
					if (rnd.nextFloat() <= corrupt_pct) {
						for (int i = 0; i < in_pkt.getLength(); ++i)
							if (rnd_byte.nextFloat() <= byte_corrupt_pct)
								in_data[i] = (byte) ((in_data[i] + 1) % 10);
						System.out.println("Packet Corrupted");
					}

					// Broadcast data to all registered senders
					for (Integer dst_port : dst_port_list) {
						// write data to the outgoing socket
						DatagramPacket out_pkt = new DatagramPacket(in_data, in_pkt.getLength(), dst_addr, dst_port);
						sk_out.send(out_pkt);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}

	public UnreliNETCorrupt(int sk1_dst_port, int sk2_dst_port, int sk3_dst_port) {
		DatagramSocket sk1, sk2;
		System.out.println("sk1_dst_port=" + sk1_dst_port + ", "
				+ "sk2_dst_port=" + sk2_dst_port + ", sk3_dst_port="
				+ sk3_dst_port + " .");

		try {
			// Create socket sk1 and sk2
			sk1 = new DatagramSocket(sk1_dst_port);
			sk2 = new DatagramSocket();

			// Create the thread to receive from sk1 and send to sk2, the
			// receiverList has only one member sk2.
			HashSet<Integer> receiverList = new HashSet<Integer>();
			receiverList.add(sk2_dst_port);
			// create threads to process sender's incoming data
			UnreliThread th1 = new UnreliThread(sk1, sk2, receiverList);
			th1.start();

			// Create the thread to receive from sk3 and send ack to all
			// registered senders, senders are maintained by static member
			// registeredSenders
			DatagramSocket sk3 = new DatagramSocket(sk3_dst_port);
			DatagramSocket sk4 = new DatagramSocket();
			// At the beginning registered sender set is empty
			registeredSenders = new HashSet<Integer>();
			UnreliThread th2 = new UnreliThread(sk3, sk4, registeredSenders);
			th2.start();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static void main(String[] args) {
		// parse parameters
		if (args.length != 3) {
			System.err
					.println("Usage: java UnreliNETDrop sk1_dst_port, sk2_dst_port, sk3_dst_port");
			System.exit(-1);
		} else {
			new UnreliNETCorrupt(Integer.parseInt(args[0]),
					Integer.parseInt(args[1]), Integer.parseInt(args[2]));
		}
	}
}
