package fr.miage.demo;

import fr.miage.demo.service.GreetingService;
import fr.miage.demo.service.OrderService;

public final class Main {

    // Cle de configuration interne : jamais affichee a l'utilisateur, ne doit pas etre internationalisee.
    private static final String CONFIG_MODE = "app.mode.standard";

    public static void main(String[] args) {
        // Affiche le message d'accueil, puis simule la confirmation d'une commande.
        GreetingService greetingService = new GreetingService();
        greetingService.greet("Camille");
        greetingService.welcome();

        OrderService orderService = new OrderService();
        orderService.confirmOrder("CMD-42", 3);
        orderService.welcomeAgain();
        orderService.printResultSummary(orderService.computeTotal(3));

        if (!CONFIG_MODE.isBlank()) {
            orderService.printSpecialThanks();
        }
    }
}
