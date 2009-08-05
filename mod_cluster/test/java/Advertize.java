import java.net.MulticastSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;

public class Advertize
{
    public static void main(String[] args) throws Exception
    {
        InetAddress group = InetAddress.getByName("228.5.6.7");
        MulticastSocket s = new MulticastSocket(23364);
        s.joinGroup(group);
        byte[] buf = new byte[1000];
        DatagramPacket recv = new DatagramPacket(buf, buf.length);
        s.receive(recv);
        String data = new String(buf);
        System.out.println("received: " + data);
        s.leaveGroup(group);
    }
}
