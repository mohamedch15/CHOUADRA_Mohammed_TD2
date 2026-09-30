package fr.miage.demo.service;

public final class OrderService {

    private static final int UNIT_PRICE = 10;

    /** Confirme une commande avec son identifiant et son nombre d'articles. */
    public void confirmOrder(String orderId, int itemCount) {
        System.out.printf("Commande %s confirmee pour %d article(s).%n", orderId, itemCount);
    }

    // Message identique a GreetingService.welcome() : doit reutiliser la meme cle de ressource.
    public void welcomeAgain() {
        System.out.println("Bienvenue dans l'application de demonstration !");
    }

    public int computeTotal(int itemCount) {
        return itemCount * UNIT_PRICE;
    }

    public void printResultSummary(int total) {
        System.out.println("Merci pour votre confiance ! A bientot : \"au revoir\" et 'adieu'.");
        System.out.printf("Montant total : %d euros.%n", total);
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
