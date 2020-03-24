package java;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.net.SocketAddress;
import java.io.IOException;
import java.io.Closeable;
import java.net.Socket;

public class Connection implements Closeable {
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final Socket socket;

    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new ObjectOutputStream (socket.getOutputStream()) ;
        this.in = new ObjectInputStream (socket.getInputStream());
    }

    public SocketAddress getRemoteSocketAddress() { return socket.getRemoteSocketAddress(); }

    public void close() throws IOException {
        socket.close();
        out.close();
        in.close();
    }

    public void send(Message message) throws IOException, ClassNotFoundException {
        synchronized (out) { out.writeObject(message); }
    }

    public Message receive() throws IOException, ClassNotFoundException {
        synchronized (in) { return (Message) in.readObject(); }
    }
}
