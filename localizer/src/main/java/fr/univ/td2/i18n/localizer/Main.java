package fr.univ.td2.i18n.localizer;

import fr.univ.td2.i18n.localizer.client.AlbertClient;
import fr.univ.td2.i18n.localizer.client.HttpAlbertClient;
import fr.univ.td2.i18n.localizer.client.ModelInfo;
import fr.univ.td2.i18n.localizer.orchestrate.LocaleTranslationResult;
import fr.univ.td2.i18n.localizer.orchestrate.LocalizationOrchestrator;
import fr.univ.td2.i18n.localizer.report.LocalizationReport;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public final class Main {

    private static final String DEFAULT_BASE_URL = "https://albert.api.etalab.gouv.fr/v1";

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(2);
        }

        String apiKey = System.getenv("ALBERT_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("Variable d'environnement ALBERT_API_KEY absente.");
            System.exit(2);
        }
        String baseUrl = System.getenv().getOrDefault("ALBERT_BASE_URL", DEFAULT_BASE_URL);
        AlbertClient client = new HttpAlbertClient(baseUrl, apiKey);

        switch (args[0]) {
            case "models" -> runModels(client);
            case "translate" -> runTranslate(client, args);
            default -> {
                printUsage();
                System.exit(2);
            }
        }
    }

    private static void runModels(AlbertClient client) {
        List<ModelInfo> models = client.listModels();
        models.forEach(m -> System.out.println(m.id()));
    }

    private static void runTranslate(AlbertClient client, String[] args) {
        if (args.length < 3) {
            printUsage();
            System.exit(2);
            return;
        }
        Path baseFile = Path.of(args[1]);
        List<String> locales = Arrays.stream(args[2].split(","))
                .map(String::strip)
                .filter(s -> !s.isBlank())
                .toList();
        boolean force = args.length > 3 && "--force".equals(args[3]);

        String model = System.getenv("ALBERT_MODEL");
        if (model == null || model.isBlank()) {
            model = client.listModels().stream()
                    .map(ModelInfo::id)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Aucun modele disponible et ALBERT_MODEL non defini"));
            System.err.println("ALBERT_MODEL non defini, utilisation du modele : " + model);
        }

        LocalizationOrchestrator orchestrator = new LocalizationOrchestrator(client, model);
        List<LocaleTranslationResult> results = orchestrator.translateAll(baseFile, locales, force);
        new LocalizationReport(results).printTo(System.out);
    }

    private static void printUsage() {
        System.err.println("""
                Usage :
                  java -jar localizer.jar models
                  java -jar localizer.jar translate <messages.properties> <locale1,locale2,...> [--force]
                Variables d'environnement requises : ALBERT_API_KEY (et ALBERT_MODEL, sinon le premier modele disponible est utilise).""");
    }
}
