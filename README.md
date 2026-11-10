# Service Plus

Application Android native de mise en relation pour huit services : téléphones, télévisions, imprimerie, antennes paraboliques, climatiseurs, réfrigérateurs, dépannage automobile et demande de transport pour une femme en travail.

## Ouvrir et lancer dans Android Studio

1. Installez une version récente d'Android Studio et le JDK 17 (ou choisissez le JDK intégré d'Android Studio).
2. Dans le SDK Manager, installez Android SDK Platform 35 et les outils de compilation correspondants.
3. Choisissez **Open** dans Android Studio et sélectionnez le dossier racine de ce dépôt, celui qui contient `settings.gradle.kts`.
4. Acceptez la synchronisation Gradle. Le wrapper fourni utilise Gradle 8.9 et télécharge la distribution et les dépendances lors de la première synchronisation.
5. Lancez la configuration `app` sur un appareil ou un émulateur Android 7.0 (API 24) ou ultérieur.

Le code Kotlin, le manifeste, les ressources et la configuration Gradle sont inclus dans le projet. Le fichier `gradle/wrapper/gradle-wrapper.jar` est inclus pour permettre à Android Studio et aux scripts Gradle de lancer le wrapper.

### Générer une version de test ou de publication

Depuis PowerShell à la racine du projet :

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
.\gradlew.bat :app:bundleRelease
```

Les APK sont placés sous `app\build\outputs\apk\` et le bundle Android sous `app\build\outputs\bundle\release\`. La configuration `release` active R8 et la réduction des ressources pour réduire la taille ; un AAB distribué par Google Play permet à la plateforme de fournir des APK adaptés à l'appareil. La version release générée localement n'est pas signée pour publication : configurez une clé de signature avant de la distribuer.

## Contact WhatsApp et téléphone

Le numéro de contact est configuré au format international, chiffres uniquement, dans `app/src/main/java/com/example/maintenance/ContactConfig.kt`. Il est utilisé pour WhatsApp et pour ouvrir le composeur Android avec le numéro prérempli.

Pour l'assistance automobile, le client précise son véhicule, la localisation de la panne et ses symptômes. Il peut aussi indiquer une panne identifiée et les pièces nécessaires ; leur disponibilité et une éventuelle livraison sont à confirmer par l'équipe après évaluation.

Pour une demande de transport en cas de travail, le formulaire demande le lieu de prise en charge et un téléphone de rappel ; la destination et les détails utiles sont facultatifs. Le contact est temporaire, n'est pas un numéro d'urgence officiel et ne garantit ni ambulance ni transport. En cas de danger immédiat, il faut contacter les services d'urgence locaux.

Le partage de position approximative est déclenché volontairement par le client depuis le formulaire. L'application demande alors l'autorisation Android de localisation approximative ; si elle est refusée ou indisponible, le lieu peut être saisi manuellement. La position n'est ajoutée à la demande qu'après cette action et peut être vérifiée ou précisée avant l'envoi.

## Devis et paiement

Le prestataire communique le montant total et la devise via WhatsApp. Le client saisit ce devis reçu dans l'application, l'accepte ou le refuse, puis ne voit les moyens de paiement qu'après l'avoir accepté. Les libellés sont configurables dans `app/src/main/java/com/example/maintenance/PaymentConfig.kt` et restent distincts : `Amana`, `Amanata NITA`, `MyNITA` et `Wave`.

L'application ne calcule pas le devis, n'intègre aucun paiement et ne vérifie pas qu'un paiement a été effectué ; l'accord et le paiement se font directement avec le prestataire, hors application. Elle ne stocke aucune demande sur un serveur, ne répartit pas les interventions en direct, ne gère pas de stock de pièces et ne garantit pas la disponibilité d'un transport.

## Langues et transcription vocale

L'interface et les messages WhatsApp sont disponibles en français, anglais, arabe (avec affichage droite à gauche) et haoussa. Le bouton de transcription utilise le service de reconnaissance vocale installé sur l'appareil et demande l'autorisation du microphone au moment de l'action. La disponibilité et la qualité dépendent de ce service et de la langue installée ; le haoussa n'est pas garanti. Le service vocal peut traiter l'audio selon ses propres règles. Le texte reconnu reste modifiable et la saisie manuelle reste disponible. Aucun serveur ni clé d'API de transcription n'est inclus.

## Liens de contenus externes

Les liens vers les actualités, les wazou au Niger et à l'étranger, et les directs de La Mecque et de la Kaaba sont configurables dans `app/src/main/java/com/example/maintenance/ContentLinks.kt`. Ils sont laissés vides jusqu'à la fourniture de sources vérifiées ; l'application n'héberge ni ne diffuse ces contenus.

Les modalités évoquées pour un paiement indirect, un montant réduit ou un accès gratuit sans session restent à préciser et ne sont pas implémentées.
