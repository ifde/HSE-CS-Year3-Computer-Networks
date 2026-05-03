package mydrive.protocol;

public class ClientIdMessage implements Message {
    private String clientId;

    public ClientIdMessage(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }
}
