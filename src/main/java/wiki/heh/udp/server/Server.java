package wiki.heh.udp.server;

import wiki.heh.udp.Message;
import wiki.heh.udp.util.JsonUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author heh
 * @date 2021/12/28
 */
public class Server {

    public static final String HOST = "127.0.0.1";
    public static final int PORT = 9999;

    private static final int MAX_PACKET_SIZE = 1024;

    private DatagramSocket server;
    private Map<String, String> clients;
    private AtomicInteger clientId = new AtomicInteger(1);


    public Server() {
        clients = new ConcurrentHashMap<>();
    }


    public void start() throws SocketException {
        server = new DatagramSocket(PORT);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        byte[] buffer = new byte[MAX_PACKET_SIZE];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        server.receive(packet);
                        onReceivePacket(packet);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        System.out.println("Server Start port:" + PORT);
    }

    /**
     * 接收包
     *
     * @param packet
     * @throws Exception
     */
    public void onReceivePacket(DatagramPacket packet) throws Exception {
        System.out.println("[Server] Receive message address{" +
                packet.getAddress() + ":" +
                packet.getPort() + "} data:{" +
                new String(packet.getData()).trim() + "}");
        InetAddress clientAddress = packet.getAddress();
        int clientPort = packet.getPort();
        String key = clientAddress.getHostAddress() + ":" + clientPort;
        //新增一个客户端
        if (!clients.containsKey(key)) {
            clients.put(key, clientId.getAndIncrement() + "");
        }
        String json = new String(packet.getData(), StandardCharsets.UTF_8).trim();
        Message message = JsonUtil.fromJson(json, Message.class);
        String cmd = message.getCommand();
        if (cmd.equals(Message.CMD_LOGIN)) {
            message.setClientId( clients.get(key));
        } else if (cmd.equals(Message.CMD_LIST)) {
            message.setClients( JsonUtil.toJson(clients));
        } else {
            throw new IllegalArgumentException(cmd.concat(" not support"));
        }
        byte[] data = JsonUtil.toJson(message).getBytes();
        packet = new DatagramPacket(data, data.length, clientAddress, clientPort);
        server.send(packet);
    }

    /**
     * 服务端测试类
     *
     * @param args
     * @throws SocketException
     */
    public static void main(String[] args) throws SocketException {
        Server server = new Server();
        server.start();
    }
}
