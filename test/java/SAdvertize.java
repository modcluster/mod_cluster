import java.net.MulticastSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.DatagramPacket;

public class SAdvertize
{
    public static void main(String[] args) throws Exception
    {
        InetAddress group = InetAddress.getByName("224.0.1.105");
        // InetAddress group = InetAddress.getByName("232.0.0.2");
        InetAddress addr =  InetAddress.getByName("10.16.88.178");
        // InetAddress addr =  InetAddress.getByName("172.17.41.254");
        InetSocketAddress addrs = new InetSocketAddress(addr, 23364);

        MulticastSocket s = new MulticastSocket(addrs);
        s.setTimeToLive(29);
        s.joinGroup(group);
        boolean ok = true;
        while (ok) {
            byte[] buf = new byte[1000];
            DatagramPacket recv = new DatagramPacket(buf, buf.length, group, 23364);
            s.send(recv);
            Thread.currentThread().sleep(10000); 
        }
        s.leaveGroup(group);
    }
}
