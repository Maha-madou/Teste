# Service Plus

Application Android native de mise en relation pour huit services : téléphones, télévisions, imprimerie, antennes paraboliques, climatiseurs, réfrigérateurs, dépannage automobile et demande de transport pour une femme en travail.

## Démarrer

Ouvrez ce projet dans Android Studio, laissez Gradle synchroniser les dépendances, puis lancez la configuration `app`.

## Contact WhatsApp et téléphone

Le numéro de contact est configuré au format international, chiffres uniquement, dans `app/src/main/java/com/example/maintenance/ContactConfig.kt`. Il est utilisé pour WhatsApp et pour ouvrir le composeur Android avec le numéro prérempli.

Pour l'assistance automobile, le client précise son véhicule, la localisation de la panne et ses symptômes. Il peut aussi indiquer une panne identifiée et les pièces nécessaires ; leur disponibilité et une éventuelle livraison sont à confirmer par l'équipe après évaluation.

Pour une demande de transport en cas de travail, le formulaire demande le lieu de prise en charge et un téléphone de rappel ; la destination et les détails utiles sont facultatifs. Le contact est temporaire, n'est pas un numéro d'urgence officiel et ne garantit ni ambulance ni transport. En cas de danger immédiat, il faut contacter les services d'urgence locaux.

Le partage de position approximative est déclenché volontairement par le client depuis le formulaire. L'application demande alors l'autorisation Android de localisation approximative ; si elle est refusée ou indisponible, le lieu peut être saisi manuellement. La position n'est ajoutée à la demande qu'après cette action et peut être vérifiée ou précisée avant l'envoi.

## Devis et paiement

Le prestataire communique le montant total et la devise via WhatsApp. Le client saisit ce devis reçu dans l'application, l'accepte ou le refuse, puis ne voit les moyens de paiement qu'après l'avoir accepté. Les libellés sont configurables dans `app/src/main/java/com/example/maintenance/PaymentConfig.kt` et restent distincts : `Amana`, `Amanata NITA`, `MyNITA` et `Wave`.

L'application ne calcule pas le devis, n'intègre aucun paiement et ne vérifie pas qu'un paiement a été effectué ; l'accord et le paiement se font directement avec le prestataire, hors application. Elle ne stocke aucune demande sur un serveur, ne répartit pas les interventions en direct, ne gère pas de stock de pièces et ne garantit pas la disponibilité d'un transport.
