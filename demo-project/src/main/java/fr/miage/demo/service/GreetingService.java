package fr.miage.demo.service;

public final class GreetingService {

    /** Salue l'utilisateur par son nom. */
    public void greet(String name) {
        System.out.printf("Bonjour, %s !%n", name);
    }

    // Affiche un message de bienvenue identique a celui d'OrderService (teste la deduplication des cles).
    public void welcome() {
        System.out.println("Bienvenue dans l'application de demonstration !");
    }
}
