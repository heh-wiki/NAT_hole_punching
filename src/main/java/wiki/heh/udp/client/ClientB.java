package wiki.heh.udp.client;

import wiki.heh.udp.Message;
import wiki.heh.udp.server.Server;
import wiki.heh.udp.util.JsonUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 客户端
 *
 * @author heh
 * @date 2021/12/28
 */
public class ClientB {
    private final AtomicLong requestId = new AtomicLong(1);

    private DatagramSocket client = null;
    private String clientId;

    public void start() throws Exception {
        client = new DatagramSocket();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    byte[] buffer = new byte[1024];
                    DatagramPacket receievePacket = new DatagramPacket(buffer, buffer.length);
                    try {
                        client.receive(receievePacket);
                        onReceiveMessage(receievePacket);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        System.out.println("Client Start");
    }

    /**
     * 接收消息
     *
     * @param packet 接收到的包
     * @throws Exception
     */
    protected void onReceiveMessage(DatagramPacket packet) throws Exception {
        System.out.println("[Client] Receive message address{" +
                packet.getAddress() + ":" +
                packet.getPort() + "} data:{" +
                new String(packet.getData()).trim() + "}");
        String json = new String(packet.getData(), StandardCharsets.UTF_8).trim();
        Message message = JsonUtil.fromJson(json, Message.class);
        if (message.getCommand().equals(Message.CMD_LOGIN)) {
            clientId = message.getClientId();
        } else if (message.getCommand().equals(Message.CMD_LIST)) {
            Map<String, String> clients = JsonUtil.fromJson(message.getClients(), Map.class);
            if (clients.size() > 1) {
                for (Map.Entry<String, String> entry : clients.entrySet()) {
                    if (entry.getValue().equals(clientId)) {
                        continue;
                    }
                    //找一个客户端 建立P2P连接
                    String[] ipPort = entry.getKey().split(":");
                    int port = Integer.parseInt(ipPort[1]);
                    InetAddress address = InetAddress.getByName(ipPort[0]);
                    message = new Message();
                    message.setRequestId(requestId.getAndIncrement());
                    message.setCommand(Message.CMD_P2P_CONNECT);
                    byte[] data = JsonUtil.toJson(message).getBytes();
                    DatagramPacket dataPack = new DatagramPacket(data, data.length, address, port);
                    client.send(dataPack);
                    break;
                }
            }
        } else if (message.getCommand().equals(Message.CMD_P2P_CONNECT)) {
            message.setData("收到ClientA数据["+message.getData()+"]->"+message.getData().concat("*"));
            byte[] data = JsonUtil.toJson(message).getBytes();
            DatagramPacket dataPack = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
            Thread.sleep(5000);
            client.send(dataPack);
        }
    }

    /**
     * 发送指令
     *
     * @param cmd 指令
     * @throws Exception
     */
    private void send(String cmd) throws Exception {
        Message message = new Message();
        message.setRequestId(requestId.getAndIncrement());
        message.setCommand(cmd);
        byte[] data = JsonUtil.toJson(message).getBytes();
        InetAddress server = InetAddress.getByName(Server.HOST);
        DatagramPacket packet = new DatagramPacket(data, data.length, server, Server.PORT);
        client.send(packet);
    }

    /**
     * 发送登录指令
     *
     * @throws Exception
     */
    public void login() throws Exception {
        send(Message.CMD_LOGIN);
    }

    /**
     * 发送列表指令
     *
     * @throws Exception
     */
    public void list() throws Exception {
        send(Message.CMD_LIST);
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        ClientB clientA = new ClientB();
        clientA.start();
        clientA.login();
        clientA.list();
    }
}
