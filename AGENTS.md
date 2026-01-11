\# AGENTS.md — Directives de refactorisation du projet (Codex)



\## Mandat

\- Refactoriser le projet en appliquant \*\*Clean Code\*\* et \*\*SOLID\*\* parfaitement.

\- \*\*Analyser le projet\*\* et \*\*supprimer les fichiers inutiles\*\*.

\- Travailler en \*\*TDD\*\* : Red → Green → Refactor, en petits lots sûrs.



\## Processus \& Sécurité

1\) \*\*Cartographie\*\* (sans code) : liste des modules, dépendances, zones mortes/dupliquées.

2\) \*\*Plan par lots\*\* (≤ 200 lignes modifiées/lot). Pas de big bang.

3\) \*\*Suppression contrôlée\*\* :

&nbsp;  - Étape 1 (rapport) : fichier | raison | références | décision.

&nbsp;  - Étape 2 (diff) : supprimer uniquement ce qui a été validé.

4\) Pas d’ajout de dépendances externes sans justification explicite.



\## Contraintes de taille

\- \*\*Maximum 500 lignes par classe\*\*.

\- \*\*Maximum 50 lignes par méthode\*\*.

\- Si dépassement : extraire classes/fonctions/stratégies jusqu’à respecter les seuils.



\## Organisation du code

\- \*\*Regrouper les classes qui évoluent ensemble\*\* (cohésion forte, couplage faible).

\- Respecter l’architecture cible choisie (cf. section “Architectures”).



\## Règles générales de Clean Code

\- Nommer classes/méthodes/variables \*\*explicitement\*\*.

\- \*\*Aucun code mort\*\*. \*\*Aucune duplication\*\* (DRY).

\- Méthodes \*\*courtes et cohérentes\*\* (une seule intention).

\- Respect des conventions du repo (indentation, style, lint/format).

\- \*\*Tests unitaires systématiques\*\* sur tout code modifié/ajouté.



\## SOLID (obligatoire)

\- \*\*SRP\*\* — Single Responsibility Principle

\- \*\*OCP\*\* — Open/Closed Principle

\- \*\*LSP\*\* — Liskov Substitution Principle

\- \*\*ISP\*\* — Interface Segregation Principle

\- \*\*DIP\*\* — Dependency Inversion Principle



\## Design Patterns (utiliser quand le besoin est avéré, pas par dogme)

\- \*\*Création\*\* : Singleton, Factory Method, Abstract Factory, Builder, Prototype

\- \*\*Structuraux\*\* : Adapter, Facade, Decorator, Composite, Proxy, Flyweight, Bridge

\- \*\*Comportement\*\* : Strategy, Observer, Command, Chain of Responsibility, Template Method, Mediator, Iterator, Memento, Interpreter, State



\## Architectures (choix guidé par le contexte)

\- \*\*Layered Architecture\*\*

\- \*\*Hexagonal Architecture\*\*

\- \*\*Microservices Architecture\*\*

\- \*\*Event-Driven Architecture (EDA)\*\*

\- \*\*CQRS\*\* (Command Query Responsibility Segregation)

\- \*\*Event Sourcing\*\*

\- \*\*Saga Pattern\*\*

\- \*\*Serverless Patterns\*\*

\- \*\*API Gateway\*\*

\- \*\*Résilience\*\* : Circuit Breaker, Retry Pattern (uniquement aux frontières I/O)



> Règle : privilégier \*\*Layered/Hexagonal\*\* par défaut. \*\*Microservices/Serverless/CQRS/Event Sourcing/Saga\*\* uniquement si la complexité métier/équipe/scale le justifie.



\## TDD — Exigences de tests

\- Toujours écrire un \*\*test rouge minimal\*\* avant d’implémenter.

\- Couverture visée ≥ \*\*85% lignes\*\* sur le code modifié (qualité > pourcentage).

\- Cas : \*\*nominal + ≥3 edge cases\*\* + erreurs attendues.

\- Tests \*\*déterministes\*\* (isoler temps/réseau/IO via mocks/adapters).



\## Sortie attendue (obligatoire pour chaque demande à Claude)

1\) \*\*Plan bref\*\* (3–6 puces) décrivant l’approche.

2\) \*\*Tests\*\* (diff des fichiers dans `tests/` ou équivalent).

3\) \*\*Checklist qualité\*\* :

&nbsp;  - \[ ] SOLID respecté (SRP/OCP/LSP/ISP/DIP)

&nbsp;  - \[ ] ≤ 500 lignes par classe / ≤ 50 lignes par méthode

&nbsp;  - \[ ] 0 duplication, 0 code mort

&nbsp;  - \[ ] Nommage explicite, conventions respectées

&nbsp;  - \[ ] Tests unitaires ajoutés/MAJ, cas limites couverts

&nbsp;  - \[ ] Pas de nouvelle dépendance non justifiée

&nbsp;  - \[ ] Si suppression : référencée dans le rapport approuvé



\## Zones sensibles (ne pas toucher sans plan dédié)

\- Pipelines CI/CD, schémas DB publics, contrats d’API publiés, config build/prod.



