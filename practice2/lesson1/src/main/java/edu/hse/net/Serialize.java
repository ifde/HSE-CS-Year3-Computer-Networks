package edu.hse.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class Serialize {

    public static final class User implements Serializable {
        private static final long serialVersionUID = 1L;

        public final String username;

        public User(String username) {
            this.username = username;
        }
    }

    public static final class Message implements Serializable {
        private static final long serialVersionUID = 1L;

        public final User author;
        public final String text;

        public Message(User author, String text) {
            this.author = author;
            this.text = text;
        }
    }

    private static String serialize() {
        try {
            User user = new User("andrei");

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);

            for (int i = 0; i < 10; i++) {
                objectStream.writeObject(new Message(user, "Message " + i));
            }

            objectStream.flush();

            return Base64.getEncoder()
                    .encodeToString(byteStream.toByteArray());
        } catch (Exception ignored) {
            return "";
        }
    }

    private static List<Message> deserialize(String base64) {
        List<Message> result = new ArrayList<>();

        try {
            byte[] data = Base64.getDecoder().decode(base64);
            ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
            ObjectInputStream objectStream = new ObjectInputStream(byteStream);

            for (int i = 0; i < 10; i++) {
                result.add((Message) objectStream.readObject());
            }
        } catch (Exception ignored) {
            // example only
        }

        return result;
    }

    public static void main(String[] args) {
        String base64 = serialize();
        System.out.printf("base64: %s\n", base64);
        List<Message> messages = deserialize(base64);

        for (Message message : messages) {
            System.out.println(
                    message.text
                            + " | author=" + message.author.toString());
        }
    }
}

