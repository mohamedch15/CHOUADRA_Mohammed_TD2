package fr.univ.td2.i18n.internationalizer.literal;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import fr.univ.td2.i18n.internationalizer.model.Finding;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Analyse une unite de compilation deja parsee pour y reperer les appels
 * d'affichage utilisant des litteraux (println, print, printf, showMessageDialog).
 * Ne modifie rien : produit uniquement des donnees (analyse pure), la reecriture
 * est realisee separement par {@link fr.univ.td2.i18n.internationalizer.rewrite.SourceRewriter}.
 */
public final class LiteralCollector {

    private static final Set<String> CONSOLE_SCOPES = Set.of("System.out", "System.err");

    private LiteralCollector() {
    }

    public static FileScanResult scan(Path file, CompilationUnit cu) {
        boolean alreadyProcessed = cu.findAll(FieldDeclaration.class).stream()
                .flatMap(f -> f.getVariables().stream())
                .anyMatch(v -> "BUNDLE".equals(v.getNameAsString()));

        if (alreadyProcessed) {
            return new FileScanResult(file, cu, true, List.of(), List.of(Finding.alreadyProcessed(file)));
        }

        List<Outcome> outcomes = cu.findAll(MethodCallExpr.class).stream()
                .map(call -> classify(file, call))
                .flatMap(Optional::stream)
                .toList();

        List<CallSite> callSites = outcomes.stream()
                .flatMap(outcome -> outcome.callSite().stream())
                .toList();
        List<Finding> findings = outcomes.stream()
                .filter(outcome -> outcome.callSite().isEmpty())
                .map(Outcome::finding)
                .toList();

        return new FileScanResult(file, cu, false, callSites, findings);
    }

    private record Outcome(Optional<CallSite> callSite, Finding finding) {
        static Outcome of(CallSite site) {
            return new Outcome(Optional.of(site), null);
        }

        static Outcome unsupported(Finding finding) {
            return new Outcome(Optional.empty(), finding);
        }
    }

    private static Optional<Outcome> classify(Path file, MethodCallExpr call) {
        String name = call.getNameAsString();
        String scope = call.getScope().map(Object::toString).orElse("");
        int line = call.getBegin().map(p -> p.line).orElse(0);

        return switch (name) {
            case "println", "print" -> CONSOLE_SCOPES.contains(scope)
                    ? classifySimple(file, call, line)
                    : Optional.empty();
            case "printf" -> CONSOLE_SCOPES.contains(scope)
                    ? classifyPrintf(file, call, line)
                    : Optional.empty();
            case "showMessageDialog" -> "JOptionPane".equals(scope)
                    ? classifyDialog(file, call, line)
                    : Optional.empty();
            default -> Optional.empty();
        };
    }

    private static Optional<Outcome> classifySimple(Path file, MethodCallExpr call, int line) {
        if (call.getArguments().size() != 1) {
            return Optional.empty();
        }
        Expression arg = call.getArgument(0);
        if (arg instanceof StringLiteralExpr literal) {
            CallSite site = new CallSite(call, CallShape.SIMPLE, literal.asString(), List.of(), null, false, line);
            return Optional.of(Outcome.of(site));
        }
        return containsNestedLiteral(arg)
                ? Optional.of(Outcome.unsupported(Finding.unsupported(file, line,
                        "argument non litteral (probable concatenation) : " + call)))
                : Optional.empty();
    }

    private static Optional<Outcome> classifyPrintf(Path file, MethodCallExpr call, int line) {
        if (call.getArguments().isEmpty()) {
            return Optional.empty();
        }
        Expression first = call.getArgument(0);
        if (!(first instanceof StringLiteralExpr literal)) {
            return containsNestedLiteral(first)
                    ? Optional.of(Outcome.unsupported(Finding.unsupported(file, line,
                            "format non litteral : " + call)))
                    : Optional.empty();
        }
        List<Expression> formatArgs = call.getArguments().subList(1, call.getArguments().size());
        Optional<FormatStringConverter.Result> converted = FormatStringConverter.convert(literal.asString(), formatArgs.size());
        if (converted.isEmpty()) {
            return Optional.of(Outcome.unsupported(Finding.unsupported(file, line,
                    "nombre de parametres incoherent avec le format : " + call)));
        }
        FormatStringConverter.Result result = converted.get();
        CallSite site = new CallSite(call, CallShape.PRINTF, result.messageFormatText(),
                List.copyOf(formatArgs), null, result.trailingNewline(), line);
        return Optional.of(Outcome.of(site));
    }

    private static Optional<Outcome> classifyDialog(Path file, MethodCallExpr call, int line) {
        if (call.getArguments().size() != 2) {
            return Optional.empty();
        }
        Expression messageArg = call.getArgument(1);
        if (messageArg instanceof StringLiteralExpr literal) {
            CallSite site = new CallSite(call, CallShape.DIALOG, literal.asString(), List.of(),
                    call.getArgument(0), false, line);
            return Optional.of(Outcome.of(site));
        }
        return containsNestedLiteral(messageArg)
                ? Optional.of(Outcome.unsupported(Finding.unsupported(file, line,
                        "message de dialogue non litteral (probable concatenation) : " + call)))
                : Optional.empty();
    }

    private static boolean containsNestedLiteral(Expression expression) {
        return !expression.findAll(StringLiteralExpr.class).isEmpty();
    }
}
