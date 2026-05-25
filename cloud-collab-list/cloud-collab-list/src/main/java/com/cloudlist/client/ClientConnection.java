package com.cloudlist.client;

import com.cloudlist.net.Message;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Client 端 Socket 通訊類別。
 * UI 執行緒只負責畫面，網路監聽由背景執行緒處理。
 */
public class ClientConnection implements AutoCloseable {
    private final Socket socket;
    private final ObjectOutputStream output;
    private final ObjectInputStream input;
    private Consumer<Message> messageHandler;
    private Consumer<Exception> errorHandler;
    private volatile boolean running = true;

    public ClientConnection(String host, int port) throws IOException {
        socket = new Socket(host, port);
        output = new ObjectOutputStream(socket.getOutputStream());
        output.flush();
        input = new ObjectInputStream(socket.getInputStream());
    }

    public void setMessageHandler(Consumer<Message> messageHandler) {
        this.messageHandler = messageHandler;
    }

    public void setErrorHandler(Consumer<Exception> errorHandler) {
        this.errorHandler = errorHandler;
    }

    public void startListening() {
        Thread thread = new Thread(() -> {
            while (running) {
                try {
                    Object obj = input.readObject();
                    if (obj instanceof Message msg && messageHandler != null) {
                        messageHandler.accept(msg);
                    }
                } catch (EOFException ex) {
                    running = false;
                    break;
                } catch (Exception ex) {
                    running = false;
                    if (errorHandler != null) {
                        errorHandler.accept(ex);
                    }
                    break;
                }
            }
        }, "client-socket-listener");
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void send(Message msg) throws IOException {
        output.writeObject(msg);
        output.flush();
        output.reset();
    }

    @Override
    public void close() {
        running = false;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
