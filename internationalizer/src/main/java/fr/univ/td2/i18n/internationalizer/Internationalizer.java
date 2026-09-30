package fr.univ.td2.i18n.internationalizer;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import fr.univ.td2.i18n.common.PropertiesFileStore;
import fr.univ.td2.i18n.internationalizer.key.BundleKeyAssigner;
import fr.univ.td2.i18n.internationalizer.literal.CallSite;
import fr.univ.td2.i18n.internationalizer.literal.FileScanResult;
import fr.univ.td2.i18n.internationalizer.literal.LiteralCollector;
import fr.univ.td2.i18n.internationalizer.model.Finding;
import fr.univ.td2.i18n.internationalizer.report.ScanReport;
import fr.univ.td2.i18n.internationalizer.rewrite.SourceRewriter;
import fr.univ.td2.i18n.internationalizer.scan.JavaSourceScanner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Point d'entree fonctionnel de l'application 1 : analyse un projet Java et
 * prepare son internationalisation en conservant la langue et le comportement
 * d'origine.
 */
public final class Internationalizer {

    private Internationalizer() {
    }

    public static ScanReport run(Path projectRoot) {
        List<Path> sourceFiles = JavaSourceScanner.findJavaSources(projectRoot);

        List<Finding> parseFindings = new ArrayList<>();
        List<FileScanResult> scanResults = sourceFiles.stream()
                .map(file -> parse(file, parseFindings))
                .flatMap(Optional::stream)
                .map(parsed -> LiteralCollector.scan(parsed.file(), parsed.compilationUnit()))
                .toList();

        Path bundlePath = projectRoot.resolve("src").resolve("main").resolve("resources").resolve("messages.properties");
        Map<String, String> existingBundle = PropertiesFileStore.load(bundlePath);

        BundleKeyAssigner.Assignment assignment = BundleKeyAssigner.assign(scanResults, existingBundle);

        List<Finding> translatedFindings = new ArrayList<>();
        for (FileScanResult result : scanResults) {
            boolean changed = SourceRewriter.rewrite(result, assignment.keysByCallSite());
            if (changed) {
                writeFile(result);
                for (CallSite site : result.callSites()) {
                    translatedFindings.add(Finding.translated(result.file(), site.line(),
                            assignment.keysByCallSite().get(site)));
                }
            }
        }

        if (!assignment.mergedBundle().equals(existingBundle)) {
            PropertiesFileStore.save(bundlePath, assignment.mergedBundle(),
                    "Genere par l'application 1 (internationalizer) - langue d'origine");
        }

        List<Finding> allFindings = new ArrayList<>();
        allFindings.addAll(parseFindings);
        scanResults.forEach(r -> allFindings.addAll(r.findings()));
        allFindings.addAll(translatedFindings);
        return new ScanReport(allFindings);
    }

    private record ParsedFile(Path file, CompilationUnit compilationUnit) {
    }

    private static Optional<ParsedFile> parse(Path file, List<Finding> parseFindings) {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            CompilationUnit cu = StaticJavaParser.parse(content);
            LexicalPreservingPrinter.setup(cu);
            return Optional.of(new ParsedFile(file, cu));
        } catch (ParseProblemException e) {
            parseFindings.add(Finding.parseError(file, "fichier non analysable, laisse tel quel"));
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture impossible : " + file, e);
        }
    }

    private static void writeFile(FileScanResult result) {
        String updated = LexicalPreservingPrinter.print(result.compilationUnit());
        try {
            Files.writeString(result.file(), updated, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Ecriture impossible : " + result.file(), e);
        }
    }
}
