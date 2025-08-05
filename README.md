# 🎮 Fortnite Pronos App

> Application web de fantasy league permettant de créer des équipes avec de vrais joueurs pro Fortnite et de concourir selon leurs performances réelles.

[![Java](https://img.shields.io/badge/Java-21-red.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-green.svg)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-20.0.0-red.svg)](https://angular.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15.13-blue.svg)](https://www.postgresql.org/)

---

## 🚀 Démarrage Rapide

### Prérequis
- **Java 21** ☕
- **Node.js 18+** 🟢  
- **PostgreSQL 15.13** 🐘
- **Maven 3.9+** 📦

### Installation

```bash
# 1. Cloner le projet
git clone <url-repo>
cd fortnite-pronos-app

# 2. Configuration PostgreSQL
# Créer la base de données 'fortnite_pronos' 
# Utilisateur: postgres, Mot de passe: postgres

# 3. Démarrage complet automatique
./start-complete.ps1
```

### URLs d'accès
- 🌐 **Frontend** : http://localhost:4200
- 🔧 **Backend API** : http://localhost:8081  
- ❤️ **Health Check** : http://localhost:8081/actuator/health
- 📊 **Leaderboard** : http://localhost:4200/leaderboard

---

## 🏗️ Architecture

### Backend (Spring Boot)
```
📦 API REST Java 21
├── 👥 Gestion utilisateurs & équipes
├── 🏆 Système de classements ELO
├── 📊 Import données Fortnite Tracker  
├── 🔄 WebSocket temps réel
└── 🔐 Authentification JWT
```

### Frontend (Angular)
```
📦 Interface utilisateur moderne
├── 📈 Graphiques interactifs (Chart.js)
├── 🎬 Mode Replay cinematique premium
├── 📱 Design responsive Material
├── ⚡ Animations 60fps
└── 🌙 Support dark mode
```

### Base de Données (PostgreSQL)
```
🗄️ Données relationnelles
├── 👤 users, players, teams
├── 📊 scores, tournaments
├── 💬 notifications, trades
└── 🔄 UUID primary keys
```

---

## 🎮 Fonctionnalités

### ⭐ Core Features
- **🛡️ Création d'équipes** multi-régions (EU, NAC, NAW, BR, ASIA, OCE, ME)
- **📊 Classements temps réel** basés sur performances pro
- **📈 Graphiques ELO** avec survol interactif
- **🔄 Mode Replay** avec animations cinematiques  
- **👥 Gestion utilisateurs** avec rôles (Admin/Participant)

### 🚀 Advanced Features  
- **💰 Trading système** d'échange de joueurs
- **🏟️ Admin panel** complet
- **📱 Interface mobile** optimisée
- **🤖 Import automatique** données Fortnite Tracker
- **⚡ WebSocket** pour mises à jour temps réel

---

## 🛠️ Développement

### Scripts disponibles

```bash
# Démarrage complet avec UI
./start-complete.ps1

# Démarrage rapide développement  
./quick-start.ps1

# Monitoring des services
./monitor.ps1

# Nettoyage fichiers obsolètes
./clean-obsolete-files.ps1
```

### Tests

```bash
# Backend
mvn test

# Frontend  
cd frontend && ng test

# E2E
cd frontend && ng e2e
```

### Base de données

```bash
# Reset complet avec données de test
curl -X POST http://localhost:8081/quicktest/full-reset

# Vérifier statut
curl http://localhost:8081/quicktest/status
```

---

## 📚 Documentation

La documentation complète est disponible dans le dossier `DEVBOOK/` :

- 📖 **[DEVBOOK_COMPLET.md](DEVBOOK/DEVBOOK_COMPLET.md)** - Guide technique complet
- 🚀 **[GUIDE_DEMARRAGE.md](DEVBOOK/GUIDE_DEMARRAGE.md)** - Guide d'installation détaillé
- 🎨 **[INTERFACE_DESIGN.md](DEVBOOK/INTERFACE_DESIGN.md)** - Documentation design UI/UX  
- 📋 **[RAPPORT_NETTOYAGE.md](DEVBOOK/RAPPORT_NETTOYAGE.md)** - Rapport optimisations

---

## 🔧 Configuration

### Ports utilisés
- **Backend** : 8081
- **Frontend** : 4200  
- **PostgreSQL** : 5432

### Variables d'environnement
```properties
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/fortnite_pronos
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# Application
SERVER_PORT=8081
SPRING_PROFILES_ACTIVE=default
```

---

## 🎯 Statut du Projet

### ✅ Fonctionnel
- Backend Spring Boot avec API REST
- Frontend Angular avec design premium  
- Base de données PostgreSQL
- Scripts de démarrage automatisés
- Documentation technique complète

### 🔄 En Développement
- Système de trading complet
- Import automatique données Fortnite Tracker
- Optimisations performances mobile
- Tests automatisés E2E

### 📋 Prochaines étapes
1. **Corriger migration Flyway V1** (types UUID)
2. **Nettoyer références obsolètes** OUTSIDER
3. **Implémenter authentification JWT** complète
4. **Optimiser mode Replay** 60fps

---

## 🤝 Contributing

1. Fork le projet
2. Créer une branche feature (`git checkout -b feature/AmazingFeature`)
3. Commit les changements (`git commit -m 'Add AmazingFeature'`)
4. Push vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrir une Pull Request

---

## 📄 License

Ce projet est sous licence MIT. Voir le fichier `LICENSE` pour plus de détails.

---

## 📞 Support

- 📧 **Email** : support@fortnite-fantasy.com
- 💬 **Discord** : [Serveur communauté](https://discord.gg/fortnite-fantasy)
- 📚 **Wiki** : [Documentation complète](./DEVBOOK/)
- 🐛 **Issues** : [GitHub Issues](https://github.com/user/repo/issues)

---

<div align="center">

**🎮 Fait avec ❤️ pour la communauté Fortnite**

*Transformez votre passion Fortnite en compétition fantasy !*

</div> 