package fr.univ.td2.i18n.localizer.client;

/** Erreur reseau, HTTP ou de format rencontree en parlant a l'API Albert. Ne contient jamais la cle d'API. */
public class AlbertClientException extends RuntimeException {

    public AlbertClientException(String message) {
        super(message);
    }

    public AlbertClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
