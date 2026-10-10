package chat.multimedia.client.voice;

import distributed.chat.CallError;
import distributed.chat.ChatServicePrx;
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
}
