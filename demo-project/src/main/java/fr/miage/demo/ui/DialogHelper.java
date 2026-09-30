package fr.miage.demo.ui;

import javax.swing.JOptionPane;

/** Boite de dialogue optionnelle (non invoquee par Main, pour ne pas rendre la demo interactive). */
public final class DialogHelper {

    public void showError() {
        JOptionPane.showMessageDialog(null, "Une erreur est survenue.");
    }
}
