package chat.multimedia.client.voice;

import distributed.chat.CallError;
import distributed.chat.ChatServicePrx;
import distributed.chat.NotInRoom;
import distributed.chat.RoomNotFound;
import distributed.chat.UserNotFound;

final class IceVoiceSignalingGateway implements VoiceSignalingGateway {

    private final ChatServicePrx chatService;

    IceVoiceSignalingGateway(ChatServicePrx chatService) {
        this.chatService = chatService;
    }

    @Override
    public void startCall(
            String caller,
            String target,
            String ip,
            int port
    ) throws UserNotFound, CallError {
        chatService.startCall(caller, target, ip, port);
    }

    @Override
    public void acceptCall(
            String caller,
            String target,
            String ip,
            int port
    ) throws UserNotFound, CallError {
        chatService.acceptCall(caller, target, ip, port);
    }

    @Override
    public void rejectCall(
            String caller,
            String target
    ) throws UserNotFound, CallError {
        chatService.rejectCall(caller, target);
    }

    @Override
    public void endCall(
            String user,
            String target
    ) throws UserNotFound, CallError {
        chatService.endCall(user, target);
    }

    @Override
    public void startGroupCall(
            String host,
            String roomName
    ) throws RoomNotFound, NotInRoom, CallError {
        chatService.startGroupCall(host, roomName);
    }

    @Override
    public void joinGroupCall(
            String nickname,
            String roomName,
            String ip,
            int port
    ) throws RoomNotFound, NotInRoom, CallError {
        chatService.joinGroupCall(
                nickname,
                roomName,
                ip,
                port
        );
    }

    @Override
    public void leaveGroupCall(
            String nickname,
            String roomName
    ) throws RoomNotFound, CallError {
        chatService.leaveGroupCall(nickname, roomName);
    }

    @Override
    public void endGroupCall(
            String host,
            String roomName
    ) throws RoomNotFound, CallError {
        chatService.endGroupCall(host, roomName);
    }
}
