import java.net.MulticastSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;

public class Advertize
{
    public static void main(String[] args) throws Exception
    {
        InetAddress group = InetAddress.getByName("224.0.1.105");
        MulticastSocket s = new MulticastSocket(23364);
        s.setTimeToLive(16);
        s.joinGroup(group);
        boolean ok = true;
        System.out.println("ready waiting...");
        while (ok) {
            byte[] buf = new byte[1000];
            DatagramPacket recv = new DatagramPacket(buf, buf.length);
            s.receive(recv);
            String data = new String(buf);
            System.out.println("received: " + data);
            System.out.println("received from " + recv.getSocketAddress());
        }
        s.leaveGroup(group);
    }
}
