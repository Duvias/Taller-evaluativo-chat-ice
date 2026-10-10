package chat.multimedia.client.voice;

import distributed.chat.CallError;
import distributed.chat.UserNotFound;

interface VoiceSignalingGateway {

    void startCall(
            String caller,
            String target,
            String ip,
            int port
    ) throws UserNotFound, CallError;

    void acceptCall(
            String caller,
            String target,
            String ip,
            int port
    ) throws UserNotFound, CallError;

    void rejectCall(
            String caller,
            String target
    ) throws UserNotFound, CallError;

    void endCall(
            String user,
            String target
    ) throws UserNotFound, CallError;
}
