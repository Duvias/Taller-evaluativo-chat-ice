package chat.multimedia.client.voice;

import distributed.chat.CallError;
import distributed.chat.NotInRoom;
import distributed.chat.RoomNotFound;
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

    void startGroupCall(
            String host,
            String roomName
    ) throws RoomNotFound, NotInRoom, CallError;

    void joinGroupCall(
            String nickname,
            String roomName,
            String ip,
            int port
    ) throws RoomNotFound, NotInRoom, CallError;

    void leaveGroupCall(
            String nickname,
            String roomName
    ) throws RoomNotFound, CallError;

    void endGroupCall(
            String host,
            String roomName
    ) throws RoomNotFound, CallError;
}
