package chat.multimedia.server.voice;

public class CallSession {

    private final String caller;
    private final String target;

    private final String callerIp;
    private final int callerPort;

    private String targetIp;
    private int targetPort;

    private CallState state;

    public CallSession(
            String caller,
            String target,
            String callerIp,
            int callerPort
    ) {
        this.caller = caller;
        this.target = target;
        this.callerIp = callerIp;
        this.callerPort = callerPort;
        this.state = CallState.RINGING;
    }

    public String getCaller() {
        return caller;
    }

    public String getTarget() {
        return target;
    }

    public String getCallerIp() {
        return callerIp;
    }

    public int getCallerPort() {
        return callerPort;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public CallState getState() {
        return state;
    }

    public void accept(String targetIp, int targetPort) {
        this.targetIp = targetIp;
        this.targetPort = targetPort;
        this.state = CallState.ACTIVE;
    }

    public boolean involves(String nickname) {
        return caller.equals(nickname) || target.equals(nickname);
    }
}