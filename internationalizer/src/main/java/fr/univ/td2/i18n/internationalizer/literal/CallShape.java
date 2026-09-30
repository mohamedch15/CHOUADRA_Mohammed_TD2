package fr.univ.td2.i18n.internationalizer.literal;

public enum CallShape {
    /** System.out.println(LITERAL) ou System.out.print(LITERAL), sans parametre. */
    SIMPLE,
    /** System.out.printf(LITERAL, arg1, arg2, ...), converti en MessageFormat. */
    PRINTF,
    /** JOptionPane.showMessageDialog(parent, LITERAL). */
    DIALOG
}
