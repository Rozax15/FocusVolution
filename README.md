# FocusVolution

Aplicacao Android para gestao de sessoes de foco com gamificacao, autenticacao segura e administracao de utilizadores.

---

## Indice

- [Visao Geral](#visao-geral)
- [Tecnologias Utilizadas](#tecnologias-utilizadas)
- [Requisitos de Sistema](#requisitos-de-sistema)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Base de Dados](#base-de-dados)
- [Configuracao](#configuracao)
- [Instalacao e Execucao](#instalacao-e-execucao)
- [Gerar APK](#gerar-apk)
- [Funcionalidades](#funcionalidades)

---

## Visao Geral

FocusVolution e um temporizador de foco que incentiva o utilizador a completar sessoes de concentracao atraves de um sistema de niveis (gamificacao). Cada 5 sessoes concluidas sobem um nivel (maximo 10). Inclui registo com verificacao por email, historico de sessoes e painel de administracao.

---

## Tecnologias Utilizadas

| Tecnologia | Versao | Finalidade |
|---|---|---|
| Kotlin | 1.9.x | Linguagem principal |
| Jetpack Compose | BOM 2024.06 | UI declarativa |
| Room | 2.6.1 | Base de dados local (SQLite) |
| Navigation Compose | 2.7.7 | Navegacao entre ecras |
| Hilt (ViewModel) | 2.8.4 | Injecao de dependencias simplificada |
| JavaMail (Android) | 1.6.6 | Envio de emails de verificacao |
| Gradle | 8.x | Sistema de build |
| Android SDK | 34 (compile), 24 (min) | SDK alvo |

---

## Requisitos de Sistema

### Para Desenvolvimento

- **Android Studio** Hedgehog (2023.1.1) ou superior
- **JDK** 17 (incluido no Android Studio)
- **Git** para clonar o repositorio
- **Android SDK** 34 (instalado via Android Studio SDK Manager)

### Para Execucao (dispositivo)

- **Android** 7.0 (API 24) ou superior
- **Conexao a Internet** (apenas para registo/verificacao de email)
- **Permissoes:** Notificacoes (Android 13+)

---

## Estrutura do Projeto

```
app/
├── src/main/
│   ├── java/com/focusvolution/app/
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── AppDao.kt              # DAO (queries Room)
│   │   │   │   ├── AppDatabase.kt          # Base de dados
│   │   │   │   ├── AppStateEntity.kt       # Entidade estado global
│   │   │   │   ├── SessionEntity.kt        # Entidade sessoes
│   │   │   │   └── UserEntity.kt           # Entidade utilizadores
│   │   │   └── repository/
│   │   │       └── FocusVolutionRepository.kt  # Logica de negocios
│   │   ├── service/
│   │   │   ├── EmailService.kt             # Envio de emails
│   │   │   ├── TimerForegroundService.kt   # Servico foreground
│   │   │   └── TimerServiceStateStore.kt   # Estado do temporizador
│   │   ├── ui/
│   │   │   ├── components/
│   │   │   │   ├── AppCharacter.kt         # Imagem do cerebro por nivel
│   │   │   │   ├── LevelUpPopup.kt         # Pop-up subida de nivel
│   │   │   │   └── LevelDownPopup.kt       # Pop-up descida de nivel
│   │   │   ├── main/
│   │   │   │   ├── MainUiState.kt          # Estado da UI principal
│   │   │   │   └── MainViewModel.kt        # ViewModel principal
│   │   │   ├── navigation/
│   │   │   │   └── AppNavigation.kt        # Rotas e navegacao
│   │   │   ├── screens/
│   │   │   │   ├── AdminScreen.kt          # Painel admin
│   │   │   │   ├── AdminUserHistoryScreen.kt # Historico (admin)
│   │   │   │   ├── LoginScreen.kt          # Login
│   │   │   │   ├── MainScreen.kt           # Temporizador + nivel
│   │   │   │   ├── RegisterScreen.kt       # Registo
│   │   │   │   ├── SessionHistoryScreen.kt # Historico pessoal
│   │   │   │   └── VerifyCodeScreen.kt     # Verificacao de email
│   │   │   └── theme/
│   │   │       └── Theme.kt
│   │   ├── email/EmailConfig.kt            # Configuracao SMTP
│   │   ├── FocusVolutionApp.kt             # Application class
│   │   └── MainActivity.kt                 # Activity principal
│   ├── res/
│   │   ├── drawable/                       # Imagens cerebro_1 a cerebro_10
│   │   └── ...                             # Layouts, valores, etc.
│   └── AndroidManifest.xml
├── build.gradle.kts
└── settings.gradle.kts
```

---

## Base de Dados

A aplicacao utiliza **Room** (SQLite local) com tres tabelas:

### Tabela: `users`

| Coluna | Tipo | Descricao |
|---|---|---|
| `id` | INTEGER (PK) | Identificador unico |
| `username` | TEXT (UNIQUE) | Nome de utilizador |
| `email` | TEXT (UNIQUE) | Email |
| `passwordHash` | TEXT | Hash SHA-256 da password |
| `totalSessions` | INTEGER | Total de sessoes concluidas |
| `currentLevel` | INTEGER | Nivel atual (1-10) |
| `isEmailVerified` | INTEGER (0/1) | Email verificado? |
| `verificationToken` | TEXT (NULLABLE) | Token de verificacao |

### Tabela: `sessions`

| Coluna | Tipo | Descricao |
|---|---|---|
| `id` | INTEGER (PK) | Identificador unico |
| `userId` | INTEGER | ID do utilizador (-1 para guest) |
| `timestamp` | INTEGER | Data/hora em epoch millis |
| `duration` | INTEGER | Duracao em segundos |

### Tabela: `app_state`

| Coluna | Tipo | Descricao |
|---|---|---|
| `id` | INTEGER (PK) | Sempre 0 (unica linha) |
| `totalSessions` | INTEGER | Total de sessoes (modo guest) |
| `currentLevel` | INTEGER | Nivel atual (modo guest) |

### Esquema SQL equivalente

```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    email TEXT NOT NULL UNIQUE,
    passwordHash TEXT NOT NULL,
    totalSessions INTEGER NOT NULL DEFAULT 0,
    currentLevel INTEGER NOT NULL DEFAULT 1,
    isEmailVerified INTEGER NOT NULL DEFAULT 0,
    verificationToken TEXT UNIQUE
);

CREATE TABLE sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    userId INTEGER NOT NULL DEFAULT -1,
    timestamp INTEGER NOT NULL,
    duration INTEGER NOT NULL
);

CREATE TABLE app_state (
    id INTEGER PRIMARY KEY DEFAULT 0,
    totalSessions INTEGER NOT NULL DEFAULT 0,
    currentLevel INTEGER NOT NULL DEFAULT 1
);
```

### Formula de Nivel

```
nivel = min((totalSessoes / 5) + 1, 10)
```

- 0-4 sessoes  → Nivel 1
- 5-9 sessoes  → Nivel 2
- 10-14 sessoes → Nivel 3
- ...
- 45+ sessoes  → Nivel 10 (maximo)

---

## Configuracao

### Email de Verificacao (SMTP Gmail)

No ficheiro `FocusVolutionApp.kt` (linhas 39-41), configurar as credenciais Gmail:

```kotlin
EmailConfig.smtpUsername = "o.teu.email@gmail.com"
EmailConfig.smtpPassword = "xxxx xxxx xxxx xxxx"  // App Password (16 caracteres)
EmailConfig.fromEmail    = "o.teu.email@gmail.com"
```

**Para gerar a App Password:**
1. Ativar 2FA na conta Gmail: https://myaccount.google.com/security
2. Gerar App Password: https://myaccount.google.com/apppasswords
3. Copiar a password de 16 caracteres para o campo `smtpPassword`

---

## Instalacao e Execucao

### 1. Clonar o repositorio

```bash
git clone https://github.com/[utilizador]/focusvolution.git
cd focusvolution
```

### 2. Abrir no Android Studio

- **File > Open...** > selecionar a pasta do projeto
- Aguardar a sincronizacao do Gradle (pode demorar 1-2 minutos)

### 3. Configurar o email (opcional)

Editar `app/src/main/java/com/focusvolution/app/FocusVolutionApp.kt` e colocar as credenciais SMTP (ver secao [Configuracao](#configuracao)).

Se nao configurar o email, o registo nao funcionara.

### 4. Executar no emulador ou dispositivo

- Ligar o dispositivo Android via USB (com Debug USB ativado) ou iniciar um emulador
- Clicar em **Run > Run 'app'** (ou Shift+F10)

A aplicacao compila e instala automaticamente.

---

## Gerar APK

### Via Android Studio

1. **Build > Build Bundle(s) / APK(s) > Build APK(s)**
2. O APK e gerado em: `app/build/outputs/apk/debug/app-debug.apk`

### Via linha de comandos (Gradle)

```bash
# Windows (PowerShell)
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew assembleDebug

# Linux / macOS
./gradlew assembleDebug
```

O APK estara em: `app/build/outputs/apk/debug/app-debug.apk`

> **Nota:** O APK de debug pode ser instalado diretamente no dispositivo. Para distribuicao, deve gerar uma versao release com keystore assinada.

---

## Funcionalidades

| Funcionalidade | Descricao |
|---|---|
| **Temporizador de foco** | Define minutos e segundos, inicia/pausa/reinicia |
| **Sistema de niveis** | 10 niveis, sobe a cada 5 sessoes, desce a cada 3 falhas |
| **Imagens por nivel** | `cerebro_1.png` a `cerebro_10.png` consoante o nivel |
| **Pop-ups animados** | Parabens ao subir de nivel ("Parabens!"), apoio ao descer ("Que pena!") |
| **Transicoes fade** | Navegacao suave entre ecras |
| **Registo seguro** | Password com hash SHA-256, verificacao por email (codigo 6 digitos) |
| **Historico de sessoes** | Lista cronologica de sessoes concluidas |
| **Painel admin** | Administrador pode ver todos os utilizadores e historico |
| **Persistencia** | Dados guardados em SQLite local (Room) |

---

## Estrutura de Navegacao

```
Login → Register → VerifyCode → Login
     ↓
    Main (temporizador + nivel)
     ↓
    SessionHistory

Login → Admin (painel de administracao)
     ↓
    AdminUserHistory
```

---

## Credenciais de Teste

### Conta normal
- Registar atraves do ecra de registo (necessita email valido para verificacao)

### Conta admin
- O administrador e o primeiro utilizador registado (assumido como admin)
- Login admin disponivel no ecra de login

---

## Formato de Entrega (PAP)

O projeto deve ser entregue no formato:

```
PAP_NomeAluno_NumeroAluno_GestaoSistemas.zip
```

Incluindo:
- Codigo-fonte completo
- APK compilado (`app-debug.apk`)
- Documento PDF com descricao e manual de utilizador
- Documentacao tecnica
- Este ficheiro README

---

## Licenca

Projeto academico — PAP do curso Tecnico de Gestao e Programacao de Sistemas de Informacao (GPSI).
