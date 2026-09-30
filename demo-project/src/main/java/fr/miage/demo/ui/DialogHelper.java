package fr.miage.demo.ui;

import javax.swing.JOptionPane;
import java.util.ResourceBundle;

/** Boite de dialogue optionnelle (non invoquee par Main, pour ne pas rendre la demo interactive). */
public final class DialogHelper {

private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages");

    public void showError() {
        JOptionPane.showMessageDialog(null, BUNDLE.getString("msg.une_erreur_est_survenue"));
    }
}
