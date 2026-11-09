# Service Plus

Application Android native de mise en relation avec un technicien pour six services : téléphones, télévisions, imprimerie, antennes paraboliques, climatiseurs et réfrigérateurs.

## Démarrer

Ouvrez ce projet dans Android Studio, laissez Gradle synchroniser les dépendances, puis lancez la configuration `app`.

## Configurer WhatsApp

Avant de distribuer l'application, remplacez `REMPLACER_PAR_VOTRE_NUMERO_WHATSAPP` dans `app/src/main/java/com/example/maintenance/WhatsappConfig.kt` par le numéro professionnel au format international, chiffres uniquement. L'application valide le numéro avant d'ouvrir WhatsApp avec la demande pré-remplie.

Le MVP ne stocke aucune demande sur un serveur : le client choisit un service, saisit ses coordonnées et décrit son problème, puis transmet le tout dans WhatsApp.
