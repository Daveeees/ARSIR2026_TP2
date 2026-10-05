package Ex3;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class ServeurFTP {

    private static final int PORT = 2110;
    private static final HashMap<String, String> comptes = new HashMap<>();

    public static void main(String[] args) {

        comptes.put("foo", "bar");

        try {
            ServerSocket serveur = new ServerSocket(PORT);
            System.out.println("Serveur FTP démarré sur le port " + PORT);
            System.out.println("En attente de clients...");

            while (true) {
                Socket socketClient = serveur.accept();
                System.out.println("Nouveau client connecté : " + socketClient.getInetAddress());
                Thread threadClient = new Thread(() -> gererClient(socketClient));
                threadClient.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void gererClient(Socket socketClient) {

        Socket socketDonnees = null;
        ServerSocket serveurDonnees = null;

        File dossierData = new File("Data");
        File dossierCourant = dossierData;

        try {
            ObjectOutputStream fluxSortie = new ObjectOutputStream(socketClient.getOutputStream());
            ObjectInputStream fluxEntree = new ObjectInputStream(socketClient.getInputStream());

            String utilisateurCourant = null;
            boolean authentifie = false;
            boolean clientActif = true;

            while (clientActif) {

                String ligne = (String) fluxEntree.readObject();
                String[] parties = ligne.trim().split("\\s+", 2);
                String commande = parties[0].toUpperCase();
                String valeur = "";

                if (parties.length > 1) {
                    valeur = parties[1];
                }

                switch (commande) {

                    case "USER":
                        if (valeur.isEmpty()) {
                            fluxSortie.writeObject("501 Erreur de syntaxe");
                        } else if (authentifie) {
                            fluxSortie.writeObject("430 Un utilisateur est déjà connecté");
                        } else if (valeur.equals("anonymous") || comptes.containsKey(valeur)) {
                            utilisateurCourant = valeur;
                            fluxSortie.writeObject("331 Utilisateur reconnu, en attente du mot de passe");
                            System.out.println("Tentative de connexion avec : " + utilisateurCourant);
                        } else {
                            utilisateurCourant = null;
                            fluxSortie.writeObject("430 Identifiant ou mot de passe incorrect");
                        }

                        fluxSortie.flush();
                        break;

                    case "PASS":
                        if (valeur.isEmpty()) {
                            fluxSortie.writeObject("501 Erreur de syntaxe");
                        } else if (authentifie) {
                            fluxSortie.writeObject("430 Un utilisateur est déjà connecté");
                        } else if (utilisateurCourant == null) {
                            fluxSortie.writeObject("430 Identifiant ou mot de passe incorrect");
                        } else if (utilisateurCourant.equals("anonymous")) {
                            authentifie = true;
                            fluxSortie.writeObject("200 Connexion réussie avec anonymous");
                            System.out.println("Utilisateur connecté : anonymous");
                        } else if (comptes.get(utilisateurCourant).equals(valeur)) {
                            authentifie = true;
                            fluxSortie.writeObject("200 Connexion réussie avec " + utilisateurCourant);
                            System.out.println("Utilisateur connecté : " + utilisateurCourant);
                        } else {
                            fluxSortie.writeObject("430 Identifiant ou mot de passe incorrect");
                        }

                        fluxSortie.flush();
                        break;

                    case "PORT":

                        if (!authentifie) {
                            fluxSortie.writeObject("430 Aucun utilisateur connecté");
                            fluxSortie.flush();
                            break;
                        }

                        if (valeur.isEmpty()) {
                            fluxSortie.writeObject("501 Erreur de syntaxe");
                            fluxSortie.flush();
                            break;
                        }

                        String[] valeurs = valeur.split(",");

                        if (valeurs.length != 6) {
                            fluxSortie.writeObject("501 Erreur de syntaxe");
                            fluxSortie.flush();
                            break;
                        }

                        String adresseIP = valeurs[0] + "." + valeurs[1] + "." + valeurs[2] + "." + valeurs[3];
                        int e = Integer.parseInt(valeurs[4]);
                        int f = Integer.parseInt(valeurs[5]);
                        int portDonnees = 256 * e + f;

                        socketDonnees = new Socket(adresseIP, portDonnees);

                        System.out.println("Connecté en mode PORT :" + adresseIP + ":" + portDonnees);

                        fluxSortie.writeObject("200 Connexion de données établie");
                        fluxSortie.flush();

                        break;

                    case "PASV":

                        if (!authentifie) {
                            fluxSortie.writeObject("430 Aucun utilisateur connecté");
                            fluxSortie.flush();
                            break;
                        }

                        if (!valeur.isEmpty()) {
                            fluxSortie.writeObject("501 Erreur de syntaxe");
                            fluxSortie.flush();
                            break;
                        }

                        serveurDonnees = new ServerSocket(0);

                        int portPassif = serveurDonnees.getLocalPort();
                        int p1 = portPassif / 256;
                        int p2 = portPassif % 256;

                        String adresseServeur = socketClient.getLocalAddress().getHostAddress().replace(".", ",");

                        System.out.println("Connecté en mode PASV :" + adresseServeur + ":" + portPassif);

                        fluxSortie.writeObject(adresseServeur + "," + p1 + "," + p2);
                        fluxSortie.flush();

                        socketDonnees = serveurDonnees.accept();

                        break;

                    case "LIST":

                        if (!authentifie) {
                            fluxSortie.writeObject("430 Aucun utilisateur connecté");
                            fluxSortie.flush();
                            break;
                        }

                        if (socketDonnees == null || socketDonnees.isClosed()) {
                            fluxSortie.writeObject("501 Connexion de données non établie");
                            fluxSortie.flush();
                            break;
                        }

                        File[] fichiers = dossierCourant.listFiles();

                        String liste = "";

                        if (fichiers != null) {
                            for (File fichier : fichiers) {
                                liste += fichier.getName() + "\n";
                            }
                        }

                        DataOutputStream sortieListe = new DataOutputStream(socketDonnees.getOutputStream());

                        sortieListe.writeUTF(liste);
                        sortieListe.flush();

                        fluxSortie.writeObject("200 Liste envoyée");
                        fluxSortie.flush();

                        break;

                    case "CWD":

                        if (!authentifie) {
                            fluxSortie.writeObject("430 Aucun utilisateur connecté");
                            fluxSortie.flush();
                            break;
                        }

                        if (valeur.isEmpty()) {
                            fluxSortie.writeObject("501 Erreur de syntaxe");
                            fluxSortie.flush();
                            break;
                        }

                        File nouveauDossier;

                        if (valeur.equals("..")) {
                            nouveauDossier = dossierCourant.getParentFile();
                        } else {
                            nouveauDossier = new File(dossierCourant, valeur);
                        }

                        if (nouveauDossier != null && nouveauDossier.exists() && nouveauDossier.isDirectory()
                                && nouveauDossier.getCanonicalPath().startsWith(dossierData.getCanonicalPath())) {

                            dossierCourant = nouveauDossier;

                            fluxSortie.writeObject("200 Répertoire courant modifié");

                        } else {

                            fluxSortie.writeObject("501 Dossier introuvable");
                        }

                        fluxSortie.flush();

                        break;

                    case "RETR":

                        if (!authentifie) {
                            fluxSortie.writeObject("430 Aucun utilisateur connecté");
                            fluxSortie.flush();
                            break;
                        }

                        if (socketDonnees == null || socketDonnees.isClosed()) {
                            fluxSortie.writeObject("501 Connexion de données non établie");
                            fluxSortie.flush();
                            break;
                        }

                        if (valeur.isEmpty()) {
                            fluxSortie.writeObject("501 Erreur de syntaxe");
                            fluxSortie.flush();
                            break;
                        }

                        File fichier = new File(dossierCourant, valeur);

                        if (!fichier.exists() || !fichier.isFile()) {
                            fluxSortie.writeObject("501 Fichier introuvable");
                            fluxSortie.flush();
                            break;
                        }

                        fluxSortie.writeObject("150 Ouverture de la connexion en cours");
                        fluxSortie.flush();

                        DataOutputStream sortieFichier = new DataOutputStream(socketDonnees.getOutputStream());
                        BufferedInputStream lectureFichier = new BufferedInputStream(new FileInputStream(fichier));

                        sortieFichier.writeLong(fichier.length());

                        byte[] buffer = new byte[1024];
                        int taille;

                        while ((taille = lectureFichier.read(buffer)) != -1) {
                            sortieFichier.write(buffer, 0, taille);
                        }

                        sortieFichier.flush();
                        lectureFichier.close();
                        socketDonnees.close();


                        fluxSortie.writeObject("200 Fichier envoyé");
                        fluxSortie.flush();

                        break;

                    case "QUIT":

                        if (!valeur.isEmpty()) {
                            fluxSortie.writeObject("501 Erreur de syntaxe");
                            fluxSortie.flush();
                        } else if (!authentifie) {
                            fluxSortie.writeObject("430 Aucun utilisateur connecté");
                            fluxSortie.flush();
                        } else {
                            fluxSortie.writeObject("200 Déconnexion");
                            fluxSortie.flush();
                            clientActif = false;
                        }

                        break;

                    default:

                        fluxSortie.writeObject("501 Erreur de syntaxe");
                        fluxSortie.flush();

                        break;
                }
            }

            System.out.println("Client déconnecté : " + utilisateurCourant);

            if (socketDonnees != null) {
                socketDonnees.close();
            }

            if (serveurDonnees != null) {
                serveurDonnees.close();
            }

            fluxEntree.close();
            fluxSortie.close();
            socketClient.close();

        } catch (EOFException e) {
            System.out.println("Un client a fermé la connexion.");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}