package Ex1;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClientFTP {

    public static void main(String[] args) {

        String adresseServeur = "localhost";
        int port = 21;

        try {
            Socket socket = new Socket(adresseServeur, port);

            ObjectOutputStream fluxSortie = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream fluxEntree = new ObjectInputStream(socket.getInputStream());

            Scanner scanner = new Scanner(System.in);

            System.out.println("Connexion au serveur réussie.");
            System.out.println("Commandes disponibles :");
            System.out.println("USER <login>");
            System.out.println("PASS <password>");
            System.out.println("QUIT");

            boolean continuer = true;

            while (continuer) {

                System.out.print("> ");
                String commande = scanner.nextLine();

                fluxSortie.writeObject(commande);
                fluxSortie.flush();

                String reponse = (String) fluxEntree.readObject();

                System.out.println(reponse);

                if (commande.equalsIgnoreCase("QUIT") && reponse.startsWith("200")) {
                    continuer = false;
                }
            }

            scanner.close();
            fluxEntree.close();
            fluxSortie.close();
            socket.close();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
