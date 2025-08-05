# TODO - CORRECTION PROBLÈMES DE LANCEMENT

## 🚨 **DIAGNOSTIC COMPLET EFFECTUÉ**

### **Problèmes identifiés qui empêchent le lancement correct :**

1. **🔴 CRITIQUE - Erreurs de validation Player** (logs error)
2. **🔴 CRITIQUE - Crashes JVM mémoire** (hs_err_pid*.log)  
3. **🟡 IMPORTANT - Configuration port incohérente** (8080 vs 8081)
4. **🟡 IMPORTANT - Données d'initialisation problématiques**

---

## 🛠️ **SOLUTIONS PRIORITAIRES À IMPLÉMENTER**

### **🔴 PROBLÈME 1 : Validation @NotBlank username Players**

**Erreur trouvée** : `ConstraintViolationException: username ne doit pas être vide`

#### **Solution immédiate :**
- [ ] **Corriger DataInitializationService.java ligne 183-185**
  ```java
  // PROBLÈME : Validation peut échouer si cleanUsername est vide
  if (cleanUsername == null || cleanUsername.trim().isEmpty()) {
      cleanUsername = "player" + Math.abs(nickname.hashCode());
  }
  ```
- [ ] **Renforcer generateValidUsername() ligne 196-215**
  ```java
  // SOLUTION : Garantir TOUJOURS un username valide
  private String generateValidUsername(String nickname) {
      if (nickname == null || nickname.trim().isEmpty()) {
          return "player" + System.currentTimeMillis();
      }
      // Nettoyer et s'assurer minimum 3 caractères
      String cleaned = nickname.toLowerCase()
          .replaceAll("[^a-z0-9]", "")
          .trim();
      if (cleaned.length() < 3) {
          cleaned = cleaned + "usr" + System.currentTimeMillis() % 1000;
      }
      return cleaned;
  }
  ```

### **🔴 PROBLÈME 2 : Crashes JVM - Mémoire insuffisante**

**Erreur trouvée** : `insufficient memory for Java Runtime Environment`

#### **Solutions critiques :**
- [ ] **Corriger start-app.ps1 MAVEN_OPTS ligne 64**
  ```powershell
  # PROBLÈME ACTUEL : -Xmx4g peut être insuffisant
  # SOLUTION : Augmenter heap et optimiser GC
  $env:MAVEN_OPTS = "-Xms4g -Xmx8g -XX:HeapBaseMinAddress=8g -XX:MaxDirectMemorySize=2g -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication"
  ```

- [ ] **Ajouter options JVM dans application.yml**
  ```yaml
  # Ajouter section JVM dans application.yml
  jvm:
    options:
      - "-Xms4g"
      - "-Xmx8g" 
      - "-XX:+UseG1GC"
      - "-XX:MaxGCPauseMillis=100"
  ```

### **🟡 PROBLÈME 3 : Configuration ports incohérente**

**Problème trouvé** : Backend config port 8081, script attend 8080

#### **Solutions :**
- [ ] **Corriger application.yml ligne 91**
  ```yaml
  server:
    port: 8080  # CHANGER de 8081 vers 8080
  ```

- [ ] **Vérifier start-app.ps1 ligne 72**
  ```powershell
  # S'assurer cohérence
  mvn spring-boot:run -Dserver.port=8080 -q
  ```

### **🟡 PROBLÈME 4 : Données d'initialisation robustesse**

#### **Solutions préventives :**
- [ ] **Ajouter validation dans DataInitializationService ligne 66-113**
  ```java
  try {
      // Validation préalable des données avant sauvegarde
      List<User> usersToCreate = createUsers();
      for (User user : usersToCreate) {
          validateUser(user); // Nouvelle méthode de validation
      }
      // ... reste du code
  } catch (Exception e) {
      log.error("❌ Erreur validation données", e);
      // Utiliser données de fallback simples
      createMinimalTestData();
  }
  ```

- [ ] **Créer méthode validateUser() robuste**
  ```java
  private void validateUser(User user) {
      if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
          throw new IllegalArgumentException("Username cannot be empty");
      }
      if (user.getEmail() == null || !user.getEmail().contains("@")) {
          throw new IllegalArgumentException("Invalid email");
      }
  }
  ```

---

## 🚀 **ORDRE D'IMPLÉMENTATION RECOMMANDÉ**

### **Étape 1 : Corrections critiques (30 min)**
1. ✅ Corriger generateValidUsername() pour garantir validation
2. ✅ Fixer configuration port 8080 dans application.yml  
3. ✅ Augmenter heap JVM à 8GB dans start-app.ps1

### **Étape 2 : Robustesse données (20 min)**
4. ✅ Ajouter validation utilisateurs avant sauvegarde
5. ✅ Créer fallback de données minimales en cas d'erreur

### **Étape 3 : Tests et validation (15 min)**
6. ✅ Tester démarrage avec nouvelles configurations
7. ✅ Vérifier logs sans erreurs de validation
8. ✅ Confirmer accès frontend/backend sur ports corrects

---

## 📊 **RÉSULTATS ATTENDUS APRÈS CORRECTIONS**

### **✅ Démarrage sans erreurs**
- Pas d'erreurs `ConstraintViolationException`
- Pas de crashes JVM `OutOfMemoryError`
- Démarrage backend sur port 8080 stable
- Frontend accessible sur http://localhost:4200

### **✅ Performance optimisée**
- JVM stable avec 8GB heap
- Données d'initialisation robustes
- Temps de démarrage < 30 secondes
- 0 erreurs dans les logs

### **✅ Fonctionnalités opérationnelles**
- Authentification fonctionnelle  
- Chargement des 147 joueurs CSV
- Création équipes sans erreurs
- APIs backend répondent correctement

---

## 🔧 **COMMANDES DE TEST POST-CORRECTION**

```powershell
# 1. Nettoyer avant test
./stop-app.ps1

# 2. Lancer avec corrections
./start-app.ps1

# 3. Vérifier santé
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/games

# 4. Tester frontend
# Ouvrir http://localhost:4200 et vérifier login
```

---

## 📝 **FICHIERS À MODIFIER**

### **Fichiers critiques :**
- `src/main/java/com/fortnite/pronos/service/DataInitializationService.java` - Validation robuste
- `src/main/resources/application.yml` - Port 8080 + JVM config
- `start-app.ps1` - Heap 8GB + optimisations GC

### **Fichiers optionnels :**
- `lancer-app.ps1` - Appliquer mêmes corrections
- Tests de validation pour DataInitializationService

---

**🎯 OBJECTIF : Application qui démarre sans erreurs en < 30 secondes avec toutes fonctionnalités opérationnelles**

**STATUS** : 🔧 Solutions identifiées - Prêt pour implémentation immédiate