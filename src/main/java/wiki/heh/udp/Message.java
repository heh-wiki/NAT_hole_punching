package wiki.heh.udp;

/**
 * @author heh
 * @date 2021/12/28
 */
public class Message {
    /**登录*/
    public static final String CMD_LOGIN="LOGIN";
    /**查询客户端列表*/
    public static final String CMD_LIST="LIST";
    /**P2P通信*/
    public static final String CMD_P2P_CONNECT ="P2P-CONNECT";

    private long requestId;

    private String command;

    private String clients;

    private String clientId;

    private String data;

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getClients() {
        return clients;
    }

    public void setClients(String clients) {
        this.clients = clients;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}

