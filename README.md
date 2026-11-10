# Service Plus

Application Android native de mise en relation avec un technicien pour sept services : téléphones, télévisions, imprimerie, antennes paraboliques, climatiseurs, réfrigérateurs et dépannage automobile.

## Démarrer

Ouvrez ce projet dans Android Studio, laissez Gradle synchroniser les dépendances, puis lancez la configuration `app`.

## Configurer WhatsApp

Le numéro professionnel est configuré au format international, chiffres uniquement, dans `app/src/main/java/com/example/maintenance/WhatsappConfig.kt`. L'application le valide avant d'ouvrir WhatsApp avec la demande pré-remplie.

Pour l'assistance automobile, le client précise son véhicule, la localisation de la panne et ses symptômes. Il peut aussi indiquer une panne identifiée et les pièces nécessaires ; leur disponibilité et une éventuelle livraison sont à confirmer par l'équipe après évaluation.

L'application ne stocke aucune demande sur un serveur : le client choisit un service, saisit ses coordonnées et transmet sa demande dans WhatsApp. Elle ne répartit pas les interventions en direct et ne gère ni stock de pièces ni paiement.
