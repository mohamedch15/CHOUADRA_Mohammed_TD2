package fr.miage.demo.service;
import java.util.ResourceBundle;
import java.text.MessageFormat;


public final class GreetingService {

private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages");

    /** Salue l'utilisateur par son nom. */
    public void greet(String name) {
        System.out.println(MessageFormat.format(BUNDLE.getString("msg.bonjour_0"), name));
    }

    // Affiche un message de bienvenue identique a celui d'OrderService (teste la deduplication des cles).
    public void welcome() {
        System.out.println(BUNDLE.getString("msg.bienvenue_dans_l_application_de_demonstration"));
    }
}
