module distributed::chat
{
    sequence<string> StringSeq;
    sequence<byte> ByteSeq;

    enum TargetType
    {
        User,
        Room
    }

    exception UserAlreadyExists
    {
        string nickname;
    }

    exception UserNotFound
    {
        string nickname;
    }

    exception RoomAlreadyExists
    {
        string roomName;
    }

    exception RoomNotFound
    {
        string roomName;
    }

    exception NotInRoom
    {
        string nickname;
        string roomName;
    }

    exception CallError
    {
        string reason;
    }

    struct FileChunk
    {
        string transferId;
        string fileName;
        long totalSize;
        int chunkIndex;
        int totalChunks;
        string sender;
        ByteSeq data;
    }

    interface ClientCallback
    {
        void userConnected(string nickname);
        void userDisconnected(string nickname);

        void receivePrivateMessage(string sender, string message);
        void receiveRoomMessage(string roomName, string sender, string message);

        void receiveFileChunk(
            TargetType targetType,
            string target,
            FileChunk chunk
        );

        void incomingCall(
            string caller,
            string ip,
            int port
        );

        void callAccepted(
            string user,
            string ip,
            int port
        );

        void callRejected(string user);
        void callEnded(string user);

        void groupCallStarted(
            string roomName,
            string host,
            string relayIp,
            int relayPort
        );

        void groupCallEnded(string roomName);
        void groupCallParticipantJoined(string roomName, string nickname);
        void groupCallParticipantLeft(string roomName, string nickname);

        void systemMessage(string message);
    }

    interface ChatService
    {
        void login(
            string nickname,
            ClientCallback* callback
        ) throws UserAlreadyExists;

        void logout(string nickname);

        StringSeq getConnectedUsers();

        void sendPrivateMessage(
            string sender,
            string receiver,
            string message
        ) throws UserNotFound;

        void createRoom(
            string roomName
        ) throws RoomAlreadyExists;

        void joinRoom(
            string nickname,
            string roomName
        ) throws RoomNotFound;

        void leaveRoom(
            string nickname,
            string roomName
        ) throws RoomNotFound, NotInRoom;

        StringSeq listRooms();

        void sendRoomMessage(
            string sender,
            string roomName,
            string message
        ) throws RoomNotFound, NotInRoom;

        void sendFileChunk(
            string sender,
            TargetType targetType,
            string target,
            FileChunk chunk
        ) throws UserNotFound, RoomNotFound, NotInRoom;

        void startCall(
            string caller,
            string target,
            string ip,
            int port
        ) throws UserNotFound, CallError;

        void acceptCall(
            string caller,
            string target,
            string ip,
            int port
        ) throws UserNotFound, CallError;

        void rejectCall(
            string caller,
            string target
        ) throws UserNotFound, CallError;

        void endCall(
            string user,
            string target
        ) throws UserNotFound, CallError;

        void startGroupCall(
            string host,
            string roomName
        ) throws RoomNotFound, NotInRoom, CallError;

        void joinGroupCall(
            string nickname,
            string roomName,
            string ip,
            int port
        ) throws RoomNotFound, NotInRoom, CallError;

        void leaveGroupCall(
            string nickname,
            string roomName
        ) throws RoomNotFound, CallError;

        void endGroupCall(
            string host,
            string roomName
        ) throws RoomNotFound, CallError;
    }
}
