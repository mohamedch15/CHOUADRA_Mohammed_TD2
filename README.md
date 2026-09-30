# TD2 — Internationalisation et localisation d'un projet Java

Deux applications Java 21 independantes, livrees dans un meme depot Maven multi-modules :

- **`internationalizer`** (application 1) : analyse un projet Java source et prepare son
  internationalisation (extraction des textes utilisateur vers un `ResourceBundle`, adaptation
  du code) sans changer son comportement ni sa langue d'origine.
- **`localizer`** (application 2) : ajoute une ou plusieurs localisations a un projet deja
  internationalise, en interrogeant l'API Albert (compatible OpenAI) et en validant chaque
  traduction avant de l'ecrire.

Un troisieme module, **`common`**, factorise la lecture/ecriture des fichiers `.properties`
(UTF-8, ordre preserve) et la gestion des marqueurs `{0}`, `{1}`, ... utilises par les deux
applications. **`demo-project`** est un petit projet Maven autonome servant de support de
demonstration (il n'est pas un module du reacteur : les deux applications operent dessus comme
sur n'importe quel projet externe, sans jamais le compiler ni l'executer elles-memes).

## Prerequis

- Java 21 (`java -version`)
- Aucune installation de Maven n'est necessaire : le Maven Wrapper est fourni et versionne
  (`mvnw` / `mvnw.cmd`), il telecharge Maven au premier lancement.

## Compiler et tester

Depuis la racine du depot (modules `common`, `internationalizer`, `localizer`) :

```powershell
.\mvnw.cmd clean install
```

```bash
./mvnw clean install
```

Tous les tests s'executent **sans reseau et sans cle API** : l'application 2 est testee avec un
double de test en memoire (`FakeAlbertClient`), jamais contre le vrai service.

`demo-project` est un projet Maven autonome (pas un module du reacteur, puisque l'application 1
ne doit ni le compiler ni l'executer pour l'analyser) :

```powershell
.\mvnw.cmd -f demo-project\pom.xml clean compile
```

## Application 1 — `internationalizer`

```powershell
.\mvnw.cmd -q -pl common,internationalizer -am package -DskipTests
java -jar internationalizer\target\internationalizer-1.0-SNAPSHOT.jar demo-project
```

### Ce qu'elle fait

Le projet est parcouru recursivement sous `src/main/java` (les repertoires `target`, `.git`,
`build` sont ignores). Chaque fichier `.java` est analyse avec **JavaParser** (et non par
regex) : cela permet de distinguer nativement un litteral de chaine reellement present dans le
code d'un texte de commentaire, qui n'existe simplement pas dans l'arbre syntaxique construit
par JavaParser.

L'application ne cible que les chaines effectivement **affichees a l'utilisateur**, via :

- `System.out.println(LITTERAL)` / `System.out.print(LITTERAL)` (et `System.err`) ;
- `System.out.printf(LITTERAL, arg1, arg2, ...)`, dont le format est converti en message
  `MessageFormat` (`%s`, `%d`, ... deviennent `{0}`, `{1}`, ...) — l'appel `printf` est alors
  remplace par `print`/`println` sur `MessageFormat.format(...)`, jamais conserve tel quel,
  pour eviter qu'un `%` present dans une traduction future ne soit reinterprete comme un
  specificateur de format ;
- `JOptionPane.showMessageDialog(parent, LITTERAL)`.

Une constante non passee a l'un de ces appels (ex. une cle de configuration interne) n'est
jamais touchee : elle n'est simplement jamais visitee par ce filtre.

Les chaines identiques (meme apres conversion des placeholders) sont **dedupliquees** : deux
appels a `println("Bienvenue !")` dans deux classes differentes partagent la meme cle de
ressource dans `messages.properties`.

### Ce qu'elle refuse de transformer

Un appel dont l'argument concatene une valeur non litterale (ex.
`println("Resultat : " + valeur)`) n'est **pas** transforme : le construire correctement
supposerait de deviner la position et le type de `valeur`, ce que l'outil ne peut pas garantir
sans risquer de casser le code. Le cas est **signale** dans le rapport final (fichier, ligne,
raison) plutot que d'etre traite a l'aveugle.

### Idempotence

Un fichier deja internationalise (repere par la presence d'un champ `BUNDLE` de type
`ResourceBundle`) est entierement ignore lors d'une nouvelle execution : ni le fichier, ni
`messages.properties` (les cles deja presentes sont conservees telles quelles) ne sont modifies.
Une seconde execution sur un projet deja traite est donc un no-op verifie par test
(`InternationalizerIntegrationTest`).

**Limite connue** : cette detection se fait au niveau du fichier entier. Si un fichier contient
a la fois des appels deja migres et un appel non gere (concatenation) qui n'a pas ete corrige a
la main, une seconde execution ne re-signalera plus ce cas non gere puisque le fichier entier est
considere comme deja traite. Le rapport de la premiere execution doit donc etre traite avant de
relancer l'outil.

## Application 2 — `localizer`

Variables d'environnement :

| Variable | Obligatoire | Role |
|---|---|---|
| `ALBERT_API_KEY` | oui | Cle d'acces a l'API Albert. Jamais lue depuis un fichier versionne, jamais affichee ni journalisee. |
| `ALBERT_MODEL` | non | Identifiant du modele de generation. Si absent, le premier modele renvoye par `GET /v1/models` est utilise (avec un avertissement). |
| `ALBERT_BASE_URL` | non | Par defaut `https://albert.api.etalab.gouv.fr/v1`. |

```powershell
.\mvnw.cmd -q -pl common,localizer -am package -DskipTests
$env:ALBERT_API_KEY = "votre-cle"          # jamais en dur dans un fichier
java -jar localizer\target\localizer-1.0-SNAPSHOT.jar models
java -jar localizer\target\localizer-1.0-SNAPSHOT.jar translate demo-project\src\main\resources\messages.properties en,de,es
```

Un seul lancement peut demander plusieurs locales (liste separee par des virgules). Pour chaque
locale, `messages_<locale>.properties` est cree/complete a cote du fichier de base.

### Garanties appliquees avant toute ecriture

- **Aucun ecrasement silencieux** : seules les cles absentes du fichier de la locale cible sont
  demandees au modele (sauf `--force`, qui doit etre explicitement passe) ; les traductions deja
  presentes ne sont jamais renvoyees au modele ni modifiees.
- **Validation systematique** de chaque traduction recue avant integration : cle attendue
  presente, valeur non vide, et **conservation exacte des marqueurs** `{0}`, `{1}`, ... du
  message d'origine (`TranslationValidator`, fonction pure testee independamment). Une
  traduction rejetee est signalee dans le rapport et n'est jamais ecrite.
- **Aucune ecriture partielle** : le fichier de la locale n'est (ré)ecrit qu'apres reception et
  validation complete de la reponse ; une reponse reseau en erreur ou un JSON invalide laisse le
  fichier existant totalement intact.
- **Isolation par locale** : l'echec d'une locale (reseau, reponse invalide) n'empeche pas le
  traitement des autres locales demandees dans le meme lancement.

### Exemples de sortie fournis (`demo-project`)

`demo-project/src/main/resources/messages_en.properties` et `messages_de.properties` sont
fournis pour permettre une demonstration multi-langues **sans cle API**. Ils ont ete produits en
faisant passer des traductions relues par un humain a travers le **vrai** pipeline de
l'application 2 (`LocalizationOrchestrator`, validation des placeholders, ecriture via
`PropertiesFileStore`), a la place d'un appel reseau reel — voir l'en-tete de chacun de ces
fichiers. La commande ci-dessus (`localizer translate ... en,de,es`) avec une cle
`ALBERT_API_KEY` reelle produit des fichiers au meme format.

### Demonstration : executer `demo-project` dans plusieurs langues

`ResourceBundle.getBundle("messages")` choisit le fichier selon la locale par defaut de la JVM.
Sans rien changer au code, compiler une fois puis faire varier la locale au lancement suffit :

```powershell
.\mvnw.cmd -q -f demo-project\pom.xml clean compile
java -cp demo-project\target\classes -Duser.language=fr fr.miage.demo.Main   # langue d'origine
java -cp demo-project\target\classes -Duser.language=en fr.miage.demo.Main
java -cp demo-project\target\classes -Duser.language=de fr.miage.demo.Main
```

(Sous Maven, les ressources ne sont copiees dans `target/classes` qu'a l'etape `process-resources` ;
`clean compile` suffit car elle l'inclut.)

Le seul message non internationalise par l'application 1 (la concatenation de
`OrderService.printSpecialThanks`) reste volontairement en francais dans les trois cas : c'est
le comportement attendu, documente dans le rapport de l'application 1.

## Choix fonctionnel vs imperatif

**Approche fonctionnelle (Streams / lambdas / references de methode)**, utilisee pour les
transformations pures de donnees, sans effet de bord :

- `JavaSourceScanner` : parcours (`Files.walk`), filtrage (extension, repertoires exclus) et tri
  des fichiers source, en pipeline de `Stream`.
- `LiteralCollector` : classification de chaque `MethodCallExpr` (`cu.findAll(...).stream()
  .map(...).flatMap(Optional::stream)...`) puis separation transformable/non-gere par filtrage
  de Stream — aucune mutation de l'AST a ce stade, uniquement de la collecte.
- `BundleKeyAssigner` : aplatissement (`flatMap`) de tous les appels detectes de tous les
  fichiers en une seule liste avant attribution des cles.
- `TranslationValidator` (application 2) : fonction pure `Map -> ValidationResult`, entierement
  construite par Streams (`map`, filtrage implicite via des enregistrements `Evaluation`) — aucun
  effet de bord, ce qui la rend trivialement testable sans mock.
- `AlbertResponseParser`, `TranslationRequestBuilder` : transformation de donnees (texte -> Map,
  Map -> JSON) sans effet de bord.
- `LocalizationOrchestrator.translateAll` : `localeCodes.stream().map(this::translateLocale)` —
  chaque locale est traitee independamment, ce qui rend l'isolation des echecs naturelle.
- `ScanReport` / `LocalizationReport` : regroupement des constats par categorie
  (`Collectors.groupingBy`) pour l'affichage.

**Approche imperative**, deliberement conservee pour les traitements ayant un ordre precis et/ou
des effets de bord :

- Les **ecritures de fichiers** (`SourceRewriter` -> `Files.writeString`, sauvegarde du bundle)
  et les **appels HTTP** (`HttpAlbertClient`) : leur enchainement, la gestion d'erreur associee
  (reseau, statut HTTP, JSON invalide) et l'ordre "ne rien ecrire avant validation complete"
  doivent rester explicites — un pipeline de Streams masquerait cet ordre et rendrait le
  traitement des exceptions moins lisible. `peek()` n'est utilise nulle part pour produire un
  effet de bord.
- `BundleKeyAssigner` : l'attribution des cles est **intrinsequement sequentielle et etat-
  dependante** (il faut savoir, pour chaque nouveau contenu, si une cle a deja ete attribuee
  precedemment dans la meme execution, et eviter toute collision) ; une boucle imperative sur une
  `Map` mutable est plus claire ici qu'un `reduce` artificiel.
- `SourceRewriter` : la modification de l'AST JavaParser (ajout d'imports, du champ `BUNDLE`,
  remplacement d'arguments) manipule un objet mutable partage (`CompilationUnit`) ; l'exprimer en
  Streams n'apporterait rien et nuirait a la lisibilite.
- `LocalizationOrchestrator.translateLocale` : sequence lineaire avec plusieurs points de sortie
  anticipee (rien a faire, echec reseau, JSON invalide) — un `try`/`if` imperatif reste plus
  lisible qu'un enchainement de `Optional`/`Either` pour ce cas.

## Limites connues

- L'application 1 ne traite que les formes d'appel listees ci-dessus (println/print/printf sur
  `System.out`/`System.err`, `JOptionPane.showMessageDialog` a deux arguments). Toute autre
  construction (concatenation, `String.format` non enveloppe dans un appel d'affichage,
  bibliotheques de journalisation, etc.) est laissee intacte et signalee si un litteral y est
  detecte a l'interieur d'une expression non geree.
- Le lecteur/ecrivain `.properties` de `common` est une implementation volontairement minimale
  (assez pour des cles/valeurs simples avec `=`, `:`, accents, guillemets, retours a la ligne
  echappes) ; il ne vise pas l'exhaustivite de `java.util.Properties`.
- L'application 2 depend de la disponibilite du modele Albert choisi ; en cas d'indisponibilite,
  chaque locale echoue independamment et le rapport l'indique, sans toucher aux fichiers deja
  presents.

## Securite de la cle API

`ALBERT_API_KEY` n'est lue que depuis l'environnement, n'est jamais ecrite dans un fichier
versionne, un message de commit, ou une sortie console/journal. Tous les tests s'executent avec
un client Albert simule (`FakeAlbertClient`) : aucune cle ni aucun acces reseau n'est necessaire
pour `mvnw test`.

## Depot GitHub et mini-GitFlow

Ce depot suit le mini-GitFlow demande : chaque fonctionnalite a ete developpee sur une branche
`feature/...`, testee, puis fusionnee dans `main` avec `git merge --no-ff` apres re-verification
des tests (`git log --graph` en atteste).

Remotes configures :

```
origin    https://github.com/mohamedch15/CHOUADRA_Mohammed_TD2.git
upstream  https://github.com/Miage-Mulhouse/complements-java-2026-2027.git
```

Pour publier ce depot : creer sur GitHub un depot **prive** vide nomme exactement
`CHOUADRA_Mohammed_TD2` (sans README/.gitignore/licence auto-genere), inviter le compte
`Miage-Mulhouse` en tant que collaborateur, puis :

```powershell
git push -u origin main
```
