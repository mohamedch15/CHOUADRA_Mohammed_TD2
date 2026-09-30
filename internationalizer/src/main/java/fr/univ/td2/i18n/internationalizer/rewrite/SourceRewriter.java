package fr.univ.td2.i18n.internationalizer.rewrite;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.NodeList;
import fr.univ.td2.i18n.internationalizer.literal.CallShape;
import fr.univ.td2.i18n.internationalizer.literal.CallSite;
import fr.univ.td2.i18n.internationalizer.literal.FileScanResult;

import java.util.List;
import java.util.Map;

/**
 * Applique, sur une unite de compilation deja preparee pour l'impression lexicale
 * ({@link com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter}),
 * les remplacements calcules par l'analyse : ajout des imports et du champ BUNDLE,
 * puis substitution de chaque litteral par un appel a {@code BUNDLE.getString(...)}
 * (eventuellement enveloppe dans {@code MessageFormat.format(...)}).
 */
public final class SourceRewriter {

    private static final String BUNDLE_FIELD =
            "private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(\"messages\");";

    private SourceRewriter() {
    }

    public static boolean rewrite(FileScanResult result, Map<CallSite, String> keysByCallSite) {
        List<CallSite> sites = result.callSites();
        if (sites.isEmpty()) {
            return false;
        }

        CompilationUnit cu = result.compilationUnit();
        cu.addImport("java.util.ResourceBundle");
        boolean needsMessageFormat = sites.stream().anyMatch(s -> s.shape() == CallShape.PRINTF);
        if (needsMessageFormat) {
            cu.addImport("java.text.MessageFormat");
        }
        ensureBundleField(cu);

        for (CallSite site : sites) {
            String key = keysByCallSite.get(site);
            MethodCallExpr getString = new MethodCallExpr(new NameExpr("BUNDLE"), "getString",
                    NodeList.nodeList(new StringLiteralExpr(key)));
            switch (site.shape()) {
                case SIMPLE -> site.call().setArgument(0, getString);
                case DIALOG -> site.call().setArgument(1, getString);
                case PRINTF -> applyPrintf(site, getString);
            }
        }
        return true;
    }

    private static void applyPrintf(CallSite site, MethodCallExpr getString) {
        NodeList<Expression> formatArgs = NodeList.nodeList(getString);
        formatArgs.addAll(site.formatArgs());
        MethodCallExpr messageFormatCall = new MethodCallExpr(new NameExpr("MessageFormat"), "format", formatArgs);

        MethodCallExpr call = site.call();
        call.setName(site.trailingNewline() ? "println" : "print");
        call.setArguments(NodeList.nodeList(messageFormatCall));
    }

    private static void ensureBundleField(CompilationUnit cu) {
        TypeDeclaration<?> primaryType = cu.getTypes().stream().findFirst().orElseThrow(() ->
                new IllegalStateException("Aucun type de haut niveau dans le fichier"));
        boolean hasBundle = primaryType.getFields().stream()
                .flatMap(f -> f.getVariables().stream())
                .anyMatch(v -> "BUNDLE".equals(v.getNameAsString()));
        if (hasBundle) {
            return;
        }
        FieldDeclaration bundleField = StaticJavaParser.parseBodyDeclaration(BUNDLE_FIELD).asFieldDeclaration();
        primaryType.getMembers().add(0, bundleField);
    }
}
