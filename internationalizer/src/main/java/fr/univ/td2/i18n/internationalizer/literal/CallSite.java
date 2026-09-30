package fr.univ.td2.i18n.internationalizer.literal;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.List;

/**
 * Un appel detecte comme candidat a l'internationalisation, avec assez d'information
 * pour reconstruire l'appel une fois la cle de ressource connue. Identite par reference
 * (pas d'egalite structurelle) : utilise dans des {@link java.util.IdentityHashMap}.
 */
public record CallSite(
        MethodCallExpr call,
        CallShape shape,
        String convertedMessage,
        List<Expression> formatArgs,
        Expression dialogParent,
        boolean trailingNewline,
        int line
) {
}
