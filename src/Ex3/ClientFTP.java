package Ex3;

import java.io.DataInputStream;
import java.io.FileOutputStream;
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
            System.out.println("LIST");
            System.out.println("CWD <dossier>");
            System.out.println("RETR <fichier>");
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

                    if (reponse.startsWith("430") || reponse.startsWith("501")) {
                        continue;
                    }

                    String[] parties = reponse.split(",");

                    String adresseIP = parties[0] + "." + parties[1] + "." + parties[2] + "." + parties[3];

                    int e = Integer.parseInt(parties[4]);
                    int f = Integer.parseInt(parties[5]);

                    int portDonnees = 256 * e + f;

                    socketDonnees = new Socket(adresseIP, portDonnees);

                    continue;
                }

                if (commande.equalsIgnoreCase("LIST")) {

                    if (socketDonnees == null || socketDonnees.isClosed()) {
                        System.out.println("Utilisez PORT ou PASV avant LIST");
                        continue;
                    }

                    fluxSortie.writeObject("LIST");
                    fluxSortie.flush();

                    DataInputStream entreeDonnees = new DataInputStream(socketDonnees.getInputStream());

                    String liste = entreeDonnees.readUTF();

                    System.out.print(liste);

                    String reponse = (String) fluxEntree.readObject();

                    System.out.println(reponse);

                    continue;
                }

                if (commande.toUpperCase().startsWith("RETR ")) {

                    if (socketDonnees == null || socketDonnees.isClosed()) {
                        System.out.println("Utilisez PORT ou PASV avant RETR");
                        continue;
                    }

                    String nomFichier = commande.substring(5);

                    fluxSortie.writeObject(commande);
                    fluxSortie.flush();

                    String reponse = (String) fluxEntree.readObject();

                    System.out.println(reponse);

                    if (!reponse.startsWith("150")) {
                        continue;
                    }

                    DataInputStream entreeFichier = new DataInputStream(socketDonnees.getInputStream());

                    long tailleFichier = entreeFichier.readLong();

                    FileOutputStream fichier = new FileOutputStream(nomFichier);

                    byte[] buffer = new byte[1024];

                    long restant = tailleFichier;

                    while (restant > 0) {

                        int taille = entreeFichier.read(buffer, 0, (int) Math.min(buffer.length, restant));

                        if (taille == -1) {
                            break;
                        }

                        fichier.write(buffer, 0, taille);

                        restant -= taille;
                    }

                    fichier.close();

                    reponse = (String) fluxEntree.readObject();

                    System.out.println(reponse);

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

            if (socketDonnees != null) {
                socketDonnees.close();
            }

            if (serveurDonnees != null) {
                serveurDonnees.close();
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