package wiki.heh;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;


/**
 * UDP服务类
 *
 * @author heh
 * @date 2021/12/28
 */
public class Server {

    /**
     * map
     */
    public static Map<String, String> map = new HashMap<>();

    public static void main(String[] args) throws Exception {
        String ip = "";
        int prot = 0;

        String str = "";
        String message = "";
        byte[] bytes = new byte[1024];

        DatagramSocket ds = new DatagramSocket(8888);
        DatagramPacket dp = new DatagramPacket(bytes, 0, bytes.length);

        /**包含IP地址及主机名*/
        SocketAddress socketAddress = null;
        /**在InetAdress的基础上加入端口*/
        InetSocketAddress inetSocketAddress = null;

        while (true) {
            System.out.println("wait...");
            ds.receive(dp);
            System.out.println("success");

            message = new String(dp.getData(), 0, dp.getLength());
            System.out.println("message=" + message);

            // 获取网络地址
            inetSocketAddress = (InetSocketAddress) dp.getSocketAddress();
            prot = inetSocketAddress.getPort();
            ip = inetSocketAddress.getAddress().getHostAddress();

            if (message.startsWith("getIp")) {
                // 第一次记录ip和端口
                map.put(String.valueOf(prot), ip);

                str = prot + "," + ip;
                byte[] tmpBytes = str.getBytes();
                dp.setData(tmpBytes, 0, tmpBytes.length);
                ds.send(dp);
                System.out.println(prot + "," + ip);
            }

            // 消息全部分发
            if (!message.equals("getIp")) {
                for (String tmpTargetPort : map.keySet()) {
                    str = "udp:ip=" + map.get(tmpTargetPort) + "prot=" + tmpTargetPort + "massage=" + message;
                    byte[] reply = str.getBytes();

                    SocketAddress sa = new InetSocketAddress(map.get(tmpTargetPort), Integer.valueOf(tmpTargetPort));
                    DatagramPacket target = new DatagramPacket(reply, 0, reply.length, sa);
                    ds.send(target);

                    System.out.println("send massage=" + str);
                }
            }
            dp.setData(bytes, 0, bytes.length);
        }

    }

}
