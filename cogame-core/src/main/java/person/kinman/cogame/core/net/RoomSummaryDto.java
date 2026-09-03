package person.kinman.cogame.core.net;

/**
 * 房间大厅简要信息 DTO
 */
public class RoomSummaryDto {
    private String roomId;
    private String hostName;
    private int boardSize;
    private int playerCount; // 1 or 2
    private boolean hasPassword;
    private String status; // "WAITING" or "PLAYING"

    public RoomSummaryDto() {}

    public RoomSummaryDto(String roomId, String hostName, int boardSize, int playerCount, boolean hasPassword, String status) {
        this.roomId = roomId;
        this.hostName = hostName;
        this.boardSize = boardSize;
        this.playerCount = playerCount;
        this.hasPassword = hasPassword;
        this.status = status;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public void setBoardSize(int boardSize) {
        this.boardSize = boardSize;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public boolean isHasPassword() {
        return hasPassword;
    }

    public void setHasPassword(boolean hasPassword) {
        this.hasPassword = hasPassword;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
