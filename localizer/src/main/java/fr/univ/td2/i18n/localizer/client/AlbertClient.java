package fr.univ.td2.i18n.localizer.client;

import java.util.List;

/**
 * Acces au service Albert, reduit aux deux operations necessaires. L'implementation
 * reelle parle HTTP ; les tests utilisent une implementation en memoire pour ne
 * jamais dependre du reseau ni d'une cle valide.
 */
public interface AlbertClient {

    List<ModelInfo> listModels();

    String chatComplete(String model, String systemPrompt, String userPrompt);
}
