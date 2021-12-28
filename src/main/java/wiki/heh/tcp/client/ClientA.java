package wiki.heh.tcp.client;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

/**
 * @author heh
 * @date 2021/12/28
 */
public class ClientA {

    private static InetAddress mediatorIP;
    private static int mediatorTcpDiscussionPort;
    private static int mediatorTcpPunchPort;

    private Socket socketDiscussion, socketClientPunch;
    private ServerSocket socketServerPunch;

    private final BufferedReader inDiscussion;
    private final BufferedOutputStream outDiscussion;

    private BufferedReader inPunch;
    private BufferedOutputStream outPunch;

    private String message = "";
    private String[] tokens = null;
    private boolean respRead;
    private volatile boolean runningHole;

    private Thread readOnHole, listenOnHole, writeOnHole;

    /**
     * 客户端A构造
     *
     * @param ip                服务器的ip地址
     * @param tcpDiscussionPort 连接服务器的tcp端口
     * @param tcpPunchPort      tcp 打洞的端口
     * @throws IOException 异常情况
     */
    public ClientA(InetAddress ip, int tcpDiscussionPort, int tcpPunchPort) throws IOException {

        //创建一个socket连接到服务器
        try {
            socketDiscussion = new Socket(ip, tcpDiscussionPort);
            socketClientPunch = new Socket(ip, tcpPunchPort);
        } catch (IOException ex) {
            System.err.println("Exception creating a socket: " + ex);
        }

        this.runningHole = true;

        //创建输入和输出流
        inDiscussion = new BufferedReader(new InputStreamReader(socketDiscussion.getInputStream()));
        outDiscussion = new BufferedOutputStream(socketDiscussion.getOutputStream());

        inPunch = new BufferedReader(new InputStreamReader(socketClientPunch.getInputStream()));
        outPunch = new BufferedOutputStream(socketClientPunch.getOutputStream());

        System.out.println("Read on hole");
        readOnHole();

        System.out.println("sending initial tcp punch message");

        //发送消息，服务器获取消息发送的所有信息（本地端口、远程端口和ip）
        byte[] sendData = "one".getBytes();
        outPunch.write(sendData);
        outPunch.write('\n');
        outPunch.flush();
    }

    private void readOnHole() throws IOException {
        this.readOnHole = new Thread(new Runnable() {
            @Override
            public void run() {
                //创建一个循环来读取来自服务器的 TCP 响应
                while (!respRead) {
                    try {
                        //等待消息
                        message = inDiscussion.readLine();

                        tokens = message.split("~~");  //split response into tokens for IP and Port

                        System.out.println("****************************************");
                        System.out.println("My PUBLIC IP seen by server: " + tokens[0]);
                        System.out.println("My PUBLIC TCP PORT seen by server: " + tokens[1]);
                        System.out.println("My LOCAL  TCP PORT seen by server: " + tokens[2]);
                        System.out.println("****************************************\n");

                        System.out.println("****************************************");
                        System.out.println("CLIENT B PUBLIC IP seen by server: " + tokens[3]);
                        System.out.println("CLIENT B PUBLIC TCP PORT seen by server: " + tokens[4]);
                        System.out.println("CLIENT B LOCAL  TCP PORT seen by server: " + tokens[5]);
                        System.out.println("****************************************");

                        respRead = true;

                        //ACK SERVER
                        outDiscussion.write("ackOne".getBytes());
                        outDiscussion.write('\n');
                        outDiscussion.flush();

                        //收到所有需要的信息 -> 继续打孔
                        proceedHolePunching(InetAddress.getByName(tokens[3].trim()), Integer.parseInt(tokens[1].trim()), Integer.valueOf(tokens[2]));
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
        });

        this.readOnHole.start();
    }

    private void listenConnectionHole(int localPort) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("监听打洞端口: " + localPort);
                    socketServerPunch = new ServerSocket(localPort);
                    socketClientPunch = socketServerPunch.accept();
                    inPunch = new BufferedReader(new InputStreamReader(socketClientPunch.getInputStream()));
                    outPunch = new BufferedOutputStream(socketClientPunch.getOutputStream());
                } catch (Exception e) {
                    inPunch = null;
                    outPunch = null;
                }
            }
        }).start();
    }

    private void listenDataOnHole(String addr, int port) {
        this.listenOnHole = new Thread(new Runnable() {
            @Override
            public void run() {
                while (runningHole) {
                    try {
                        message = inPunch.readLine();

                        System.out.println("Received: " + message.trim() + ", From: IP " + addr + " Port " + port);
                    } catch (IOException ex) {
                        System.err.println("Error " + ex);
                    }
                }
            }
        });
        this.listenOnHole.start();
    }

    private void writeDataOnHole() {
        this.writeOnHole = new Thread(new Runnable() {
            @Override
            public void run() {
                int j = 0;
                String msg;
                //create Loop to send udp packets
                while (runningHole) {
                    try {
                        msg = "I AM CLIENT A " + j;
                        outPunch.write(msg.getBytes());
                        outPunch.write('\n');
                        outPunch.flush();
                        j++;
                        Thread.sleep(2000);
                    } catch (IOException e) {
                        System.err.println("IOException");
                    } catch (Exception e) {
                        System.err.println("SleepException");
                    }
                }
            }
        });

        this.writeOnHole.start();
    }

    private void proceedHolePunching(InetAddress addrToConnect, int portToConnect, int localPort) throws IOException {
        if (this.socketClientPunch != null) {
            outPunch = null;
            inPunch = null;
            String addr = addrToConnect.getHostAddress().trim();

            System.out.println("开始监听端口 : " + localPort);
            listenConnectionHole(localPort);

            System.out.println("尝试链接到 : " + addr + ":" + portToConnect);
            try {
                //Close this socket actually connected to the mediator
                socketClientPunch.setReuseAddress(true);
                socketClientPunch.close();

                //Create a new one
                socketClientPunch = new Socket();
                socketClientPunch.setReuseAddress(true);

                //绑定到同一个地址
                socketClientPunch.bind(new InetSocketAddress(localPort));

                //连接到远程客户端
                socketClientPunch.connect(new InetSocketAddress(addrToConnect, portToConnect));

                //Init in and out
                inPunch = new BufferedReader(new InputStreamReader(socketClientPunch.getInputStream()));
                outPunch = new BufferedOutputStream(socketClientPunch.getOutputStream());


            } catch (ConnectException ce) {
                System.out.println("Punch: Connection refused");
            }

            if (outPunch != null && inPunch != null) {
                System.out.println("Punch: Connected to : " + addr + ":" + portToConnect);
                listenDataOnHole(addr, portToConnect);
                writeDataOnHole();
            } else {
                System.err.println("Error when attempting to connect");
            }
        }
    }

    //Entry point
    public static void main(String[] args) throws IOException {

        if (args.length > 0) {//Give args
            try {
                //Get first param server mediator ip address
                mediatorIP = InetAddress.getByName(args[0].trim());
                //Get second params udp port
                mediatorTcpDiscussionPort = Integer.parseInt(args[1].trim());
                //Get third params tcp port
                mediatorTcpPunchPort = Integer.parseInt(args[2].trim());
            } catch (Exception ex) {
                System.err.println("Error in input");
                System.out.println("USAGE: java ClientA mediatorIP mediatorTcpDiscussionPort mediatorTcpPunchPort");
                System.out.println("Example: java ClientA 127.0.0.1 9000 9001");
                System.exit(0);
            }
        } else {//Give no args
            System.out.println("ClientA running with default ports 9000 and 9001");

            //by default use localhost
            mediatorIP = InetAddress.getByName("127.0.0.1");
            //default port for tcp
            mediatorTcpDiscussionPort = 9000;
            //default port for udp
            mediatorTcpPunchPort = 9001;

        }
        new ClientA(mediatorIP, mediatorTcpDiscussionPort, mediatorTcpPunchPort);
    }
}