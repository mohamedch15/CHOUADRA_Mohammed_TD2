package fr.univ.td2.i18n.internationalizer;

import fr.univ.td2.i18n.internationalizer.report.ScanReport;

import java.nio.file.Path;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java -jar internationalizer.jar <repertoire-racine-du-projet>");
            System.exit(2);
        }
        Path projectRoot = Path.of(args[0]);
        ScanReport report = Internationalizer.run(projectRoot);
        report.printTo(System.out);
    }
}
