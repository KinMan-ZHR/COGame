package person.kinman.cogame.sb.model;

/**
 * 服务器信息模型类
 */
public class ServerInfo {
    private String ip;
    private int port;
    private boolean isConnected;
    
    public ServerInfo(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.isConnected = false;
    }
    
    // Getters and Setters
    public String getIp() {
        return ip;
    }
    
    public void setIp(String ip) {
        this.ip = ip;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public boolean isConnected() {
        return isConnected;
    }
    
    public void setConnected(boolean connected) {
        isConnected = connected;
    }
}
