package wiki.heh;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Scanner;

/**
 * UDP客户端
 *
 * @author heh
 * @date 2021/12/28
 */
public class ClientA {
    private static int serverPort = 8888;
    private static String serverIp = "127.0.0.1";

    public static void main(String[] args) throws Exception {
        SocketAddress server = new InetSocketAddress(serverIp, serverPort);
        DatagramSocket ds = new DatagramSocket();
        String massage = "getIp";
        byte[] buff = massage.getBytes();
        byte[] buffer = new byte[8 * 1024];
        DatagramPacket dp = new DatagramPacket(buff, 0, buff.length, server);
        ds.send(dp);

        System.out.print("请输入message:");
        Scanner sc = new Scanner(System.in);
        dp.setData(buffer);
        ds.receive(dp);
        String message = new String(dp.getData(), 0, dp.getLength());
        System.out.println("本机端口、IP" + message);


        // 开启一个线程
        new Thread(
                new DatagramSocketThread(ds)
        ).start();

        while (true) {
            massage = sc.next();
            buff = massage.getBytes();
            dp.setData(buff);
            ds.send(dp);
            System.out.println("发送成功,message=" + message);
        }
    }

}

/**
 * 数据报socket
 */
//class DatagramSocketThread implements Runnable {
//
//    DatagramPacket dp = null;
//    DatagramSocket ds = null;
//    String message = "";
//
//    public DatagramSocketThread(DatagramSocket ds) {
//        this.ds = ds;
//    }
//
//    @Override
//    public void run() {
//        try {
//            byte[] bytes = new byte[1024];
//            byte[] buffer = new byte[1024];
//
//            dp = new DatagramPacket(bytes, bytes.length);
//            while (true) {
//                dp.setData(buffer);
//                ds.receive(dp);
//                message = new String(dp.getData());
//                System.out.println("接收成功 massage=" + message);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        System.out.println("线程死亡....");
//    }
//}
