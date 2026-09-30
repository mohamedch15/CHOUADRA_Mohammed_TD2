# TD2 - Internationalisation et localisation Java

Projet Java 21 compose de deux applications Maven independantes.

## Structure

- `internationalizer` : analyse les sources Java et remplace prudemment les textes utilisateur par `ResourceBundle` + `MessageFormat`.
- `localizer` : traduit les fichiers `.properties` vers plusieurs locales avec Albert (API compatible OpenAI), puis valide les parametres avant ecriture.
- `demo-project` : petit projet Maven servant de support reproductible.

## Compiler et tester

Depuis la racine :

```powershell
.\mvnw.cmd clean test
```

Sans wrapper installe, `mvn clean test` fonctionne aussi si Maven est disponible.

## Application 1

```powershell
java -jar internationalizer\target\internationalizer-1.0-SNAPSHOT.jar demo-project
```

Le programme ne touche qu'aux `src/main/java`, ignore `target`, `.git` et les fichiers deja marques `ResourceBundle`. Il cible les chaines litterales directement utilisees par `System.out.print*`, `printf` et `JOptionPane.showMessageDialog`. Les chaines de commentaires, les constantes non utilisees pour l'affichage et les expressions complexes sont conservees et signalees. Une seconde execution est sans effet.

## Application 2

Lister les modeles :

```powershell
$env:ALBERT_API_KEY="votre-cle"
$env:ALBERT_MODEL="nom-du-modele"
java -jar localizer\target\localizer-1.0-SNAPSHOT.jar models
```

Traduire vers plusieurs locales :

```powershell
java -jar localizer\target\localizer-1.0-SNAPSHOT.jar translate demo-project\src\main\resources\messages.properties en,de,es
```

La cle n'est jamais ecrite dans le depot ni les journaux. Une traduction existante n'est pas ecrasee. Les reponses doivent etre un objet JSON contenant `translations`, avec les memes cles et les memes parametres `{0}`, `{1}`, etc. Une reponse invalide est refusee avant toute ecriture.

## Choix techniques

Les parcours de fichiers, filtrages de sources, regroupements de cles et validations des parametres utilisent des Streams et des lambdas : ce sont des transformations de donnees sans effet de bord. Les ecritures de fichiers et les appels HTTP restent imperatifs, car leur ordre et la gestion des erreurs doivent etre explicites. L'API HTTP repose uniquement sur `java.net.http.HttpClient` et Jackson.

## Limites

L'application 1 ne pretend pas comprendre toutes les constructions Java : elle prefere conserver une chaine complexe et produire un avertissement plutot que casser le code. L'application 2 depend de la disponibilite du modele Albert et ne lance aucune requete dans les tests unitaires.
