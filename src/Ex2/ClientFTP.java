package Ex2;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ClientFTP {

    public static void main(String[] args) {

        String adresseServeur = "localhost";
        int port = 2110;
        Socket socketDonnees = null;
        ServerSocket serveurDonnees = null;

        try {
            Socket socket = new Socket(adresseServeur, port);

            ObjectOutputStream fluxSortie = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream fluxEntree = new ObjectInputStream(socket.getInputStream());

            Scanner scanner = new Scanner(System.in);

            System.out.println("Connexion au serveur réussie.");
            System.out.println("Commandes disponibles :");
            System.out.println("USER <login>");
            System.out.println("PASS <password>");
            System.out.println("PORT");
            System.out.println("PASV");
            System.out.println("QUIT");

            boolean continuer = true;

            while (continuer) {

                System.out.print("> ");
                String commande = scanner.nextLine();

                if (commande.equalsIgnoreCase("PORT")) {

                    serveurDonnees = new ServerSocket(0);
                    int portDonnees = serveurDonnees.getLocalPort();
                    String adresseClient = socket.getLocalAddress().getHostAddress().replace(".", ",");
                    int e = portDonnees / 256;
                    int f = portDonnees % 256;
                    String commandePORT = "PORT " + adresseClient + "," + e + "," + f;
                    fluxSortie.writeObject(commandePORT);
                    fluxSortie.flush();
                    socketDonnees = serveurDonnees.accept();
                    String reponse = (String) fluxEntree.readObject();
                    System.out.println(reponse);
                    continue;
                }

                if (commande.equalsIgnoreCase("PASV")) {

                    fluxSortie.writeObject("PASV");
                    fluxSortie.flush();
                    String reponse = (String) fluxEntree.readObject();
                    System.out.println(reponse);
                    String[] parties = reponse.split(",");
                    String adresseIP = parties[0] + "." + parties[1] + "." + parties[2] + "." + parties[3];
                    int e = Integer.parseInt(parties[4]);
                    int f = Integer.parseInt(parties[5]);
                    int portDonnees = 256 * e + f;

                    socketDonnees = new Socket(adresseIP, portDonnees);

                    continue;
                }

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
