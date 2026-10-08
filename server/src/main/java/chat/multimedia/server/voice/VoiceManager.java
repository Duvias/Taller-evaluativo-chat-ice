package chat.multimedia.server.voice;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceManager {

    private final Map<String, CallSession> callsByUser =
            new ConcurrentHashMap<>();

    public synchronized CallSession startCall(
            String caller,
            String target,
            String callerIp,
            int callerPort
    ) {
        if (caller.equals(target)) {
            throw new IllegalStateException(
                    "A user cannot call themselves."
            );
        }

        if (callsByUser.containsKey(caller)) {
            throw new IllegalStateException(
                    caller + " is already in a call."
            );
        }

        if (callsByUser.containsKey(target)) {
            throw new IllegalStateException(
                    target + " is already in a call."
            );
        }

        CallSession session = new CallSession(
                caller,
                target,
                callerIp,
                callerPort
        );

        callsByUser.put(caller, session);
        callsByUser.put(target, session);

        return session;
    }

    public synchronized CallSession acceptCall(
            String caller,
            String target,
            String targetIp,
            int targetPort
    ) {
        CallSession session = callsByUser.get(caller);

        validateCall(session, caller, target);

        if (session.getState() != CallState.RINGING) {
            throw new IllegalStateException(
                    "Call is not waiting for acceptance."
            );
        }

        session.accept(targetIp, targetPort);

        return session;
    }

    public synchronized void rejectCall(
            String caller,
            String target
    ) {
        CallSession session = callsByUser.get(caller);

        validateCall(session, caller, target);
        removeSession(session);
    }

    public synchronized void endCall(
            String user,
            String otherUser
    ) {
        CallSession session = callsByUser.get(user);

        if (session == null) {
            throw new IllegalStateException(
                    user + " is not in a call."
            );
        }

        if (!session.involves(otherUser)) {
            throw new IllegalStateException(
                    "Users are not in the same call."
            );
        }

        removeSession(session);
    }

    public CallSession getCall(String nickname) {
        return callsByUser.get(nickname);
    }

    public boolean isInCall(String nickname) {
        return callsByUser.containsKey(nickname);
    }

    private void validateCall(
            CallSession session,
            String caller,
            String target
    ) {
        if (session == null) {
            throw new IllegalStateException(
                    "Call does not exist."
            );
        }

        if (!session.getCaller().equals(caller)
                || !session.getTarget().equals(target)) {
            throw new IllegalStateException(
                    "Invalid call participants."
            );
        }
    }

    private void removeSession(CallSession session) {
        callsByUser.remove(session.getCaller());
        callsByUser.remove(session.getTarget());
    }
}