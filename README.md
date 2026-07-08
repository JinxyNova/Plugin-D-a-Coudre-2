# DeACoudre — plugin Minecraft

Minigame façon "dé à coudre" : les joueurs sont enfermés dans une zone qui se
remplit d'eau petit à petit. Le dernier joueur encore au sec gagne.

## Compilation

Ce projet est un projet Maven standard. Sur ta machine (avec accès internet
à Maven Central / repo PaperMC) :

```bash
mvn clean package
```

Le jar final sera dans `target/DeACoudre-1.0.0.jar`, à mettre dans le
dossier `plugins/` de ton serveur Paper/Spigot (1.20.x, Java 17+).

> Note : dans cet environnement sandbox je n'ai pas accès à Maven Central,
> donc je n'ai pas pu produire le `.jar` directement. J'ai en revanche
> testé la compilation du code source en recréant à l'identique les
> classes de l'API Bukkit utilisées (stubs), ce qui m'a permis de vérifier
> qu'il n'y a aucune erreur de syntaxe ni de typage (0 erreur, 0 warning).
> Un `mvn package` chez toi devrait fonctionner directement.

## Compiler automatiquement avec GitHub Actions (sans rien installer)

Ce projet contient un workflow GitHub Actions (`.github/workflows/build.yml`)
qui compile le plugin pour toi, dans le cloud, à chaque envoi de code. Tu
n'as pas besoin d'installer Java ni Maven sur ton PC.

1. Crée un nouveau dépôt sur https://github.com/new (par exemple `deacoudre`)
2. Sur ton PC, dans le dossier `deacoudre` (celui du zip), ouvre un terminal
   et tape :
   ```bash
   git init
   git add .
   git commit -m "Premier envoi du plugin DeACoudre"
   git branch -M main
   git remote add origin https://github.com/TON-PSEUDO/deacoudre.git
   git push -u origin main
   ```
   (remplace `TON-PSEUDO` par ton nom d'utilisateur GitHub, et adapte l'URL
   si le nom du dépôt est différent)
3. Va sur ton dépôt GitHub, onglet **Actions** : une compilation démarre
   automatiquement (ça prend 1-2 minutes)
4. Une fois qu'elle est verte (✅), clique dessus, puis dans **Artifacts**
   en bas de page, télécharge `DeACoudre-plugin` : c'est un zip qui
   contient le `.jar` prêt à mettre dans `plugins/`

À chaque fois que tu modifies le code et que tu refais `git push`, une
nouvelle compilation se lance automatiquement et tu récupères le nouveau
`.jar` de la même façon.

## Configuration initiale (en jeu)

1. Va à l'endroit où tu veux que les joueurs attendent, puis :
   `/dac setlobby`
2. Va au centre de la zone qui doit se remplir d'eau, puis :
   `/dac setcenter`
3. Définis la taille (rayon en blocs) de la zone :
   `/dac settaille 25`
4. Définis la hauteur à partir de laquelle l'eau commence à monter
   (en général le sol de ta map) :
   `/dac sethauteurdepart 64`
5. Définis la hauteur max que l'eau peut atteindre :
   `/dac sethauteurmax 100`
6. (Optionnel) Ajoute des points de spawn pour les joueurs, un par un, en te
   plaçant à l'endroit voulu puis :
   `/dac addspawn`
   Si tu n'en définis aucun, les joueurs sont répartis automatiquement en
   cercle en haut de la zone.

## Toutes les commandes

| Commande | Permission | Effet |
|---|---|---|
| `/dac join` | `dac.join` | Rejoindre la partie |
| `/dac leave` | `dac.join` | Quitter la partie |
| `/dac info` | tout le monde | Affiche l'état et la config actuelle |
| `/dac start` | `dac.admin` | Force le lancement immédiat |
| `/dac stop` | `dac.admin` | Arrête et réinitialise la partie en cours |
| `/dac reload` | `dac.admin` | Recharge config.yml |
| `/dac setlobby` | `dac.admin` | Définit le lobby à ta position |
| `/dac setcenter` | `dac.admin` | Définit le centre de la zone à ta position |
| `/dac settaille <rayon>` | `dac.admin` | Taille (rayon en blocs) de la zone |
| `/dac sethauteurdepart <y>` | `dac.admin` | Hauteur Y où l'eau commence |
| `/dac sethauteurmax <y>` | `dac.admin` | Hauteur Y max de l'eau |
| `/dac setintervalle <secondes>` | `dac.admin` | Temps entre 2 montées d'eau |
| `/dac setminjoueurs <n>` | `dac.admin` | Joueurs minimum pour lancer le countdown |
| `/dac setmaxjoueurs <n>` | `dac.admin` | Joueurs maximum dans une partie |
| `/dac setcountdown <secondes>` | `dac.admin` | Durée du compte à rebours du lobby |
| `/dac addspawn` | `dac.admin` | Ajoute un point de spawn à ta position |
| `/dac clearspawns` | `dac.admin` | Supprime tous les points de spawn |

Toutes ces options sont aussi modifiables directement dans `config.yml`
puis `/dac reload`.

## Déroulement d'une partie

1. Les joueurs font `/dac join` dans le lobby.
2. Dès que le nombre minimum de joueurs est atteint, un compte à rebours
   démarre automatiquement.
3. À la fin du compte à rebours, tous les joueurs sont téléportés dans la
   zone (sur les spawns définis, ou en cercle sinon).
4. Toutes les `intervalle-secondes` secondes, l'eau monte d'un niveau dans
   toute la zone (remplace uniquement les blocs d'air, donc respecte tes
   constructions/plateformes).
5. Un joueur touché par l'eau (ou tombé sous la hauteur de départ) est
   éliminé et passe en mode spectateur.
6. Le dernier survivant gagne, la partie se réinitialise après 5 secondes.

## Notes techniques

- Aucune perte de faim pendant la partie, et la noyade ne peut pas tuer un
  joueur directement (c'est le plugin qui gère l'élimination proprement).
- La déconnexion d'un joueur pendant une partie est gérée (il est retiré,
  la victoire est réévaluée automatiquement).
- À la fin d'une partie, l'eau ajoutée dans la zone est automatiquement
  retirée (remise à l'air) pour que la map soit prête pour la partie
  suivante.
