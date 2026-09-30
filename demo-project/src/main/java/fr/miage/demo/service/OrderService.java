package fr.miage.demo.service;
import java.util.ResourceBundle;
import java.text.MessageFormat;


public final class OrderService {

private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages");

    private static final int UNIT_PRICE = 10;

    /** Confirme une commande avec son identifiant et son nombre d'articles. */
    public void confirmOrder(String orderId, int itemCount) {
        System.out.println(MessageFormat.format(BUNDLE.getString("msg.commande_0_confirmee_pour_1_article"), orderId, itemCount));
    }

    // Message identique a GreetingService.welcome() : doit reutiliser la meme cle de ressource.
    public void welcomeAgain() {
        System.out.println(BUNDLE.getString("msg.bienvenue_dans_l_application_de_demonstration"));
    }

    public int computeTotal(int itemCount) {
        return itemCount * UNIT_PRICE;
    }

    public void printResultSummary(int total) {
        System.out.println(BUNDLE.getString("msg.merci_pour_votre_confiance_a_bientot"));
        System.out.println(MessageFormat.format(BUNDLE.getString("msg.montant_total_0_euros"), total));
    }

    // Cas volontairement non gere : le message concatene une valeur non litterale.
    // L'application 1 doit le signaler plutot que de produire un code incorrect.
    public void printSpecialThanks() {
        System.out.println("Merci encore, " + vipLabel() + " !");
    }

    private String vipLabel() {
        return "cher client";
    }
}
