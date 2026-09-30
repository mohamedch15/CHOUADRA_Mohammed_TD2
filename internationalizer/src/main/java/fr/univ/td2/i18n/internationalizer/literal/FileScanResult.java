package fr.univ.td2.i18n.internationalizer.literal;

import com.github.javaparser.ast.CompilationUnit;
import fr.univ.td2.i18n.internationalizer.model.Finding;

import java.nio.file.Path;
import java.util.List;

public record FileScanResult(
        Path file,
        CompilationUnit compilationUnit,
        boolean alreadyProcessed,
        List<CallSite> callSites,
        List<Finding> findings
) {
}
