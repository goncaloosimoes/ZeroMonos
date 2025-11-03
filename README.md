# TQS HomeWork 1 : ZeroMonos, um Sistema para a Recolha de Resíduos 🗑️

Sistema web para agendamento e gestão de recolhas de resíduos municipais em Portugal, desenvolvido como parte da UC de Teste e Qualidade de Software.

## 📋 Stack Tecnológica

**Backend & Base de Dados**
- Java 21 + Spring Boot 3.5.7
- H2 (in-memory) + JPA/Hibernate

**Frontend**
- HTML5, CSS3, JavaScript (ES6+)

**Testes & Qualidade**
- JUnit 5, RestAssured 5.4.0, Cucumber 7.18.0, Selenium 4.19.1
- JaCoCo, SonarQube

**Documentação API**
- SpringDoc OpenAPI (Swagger UI)

**Build & CI/CD**
- Maven + GitHub Actions

***https://start.spring.io/*** was used to kickstart the project.
![Spring Initializr](docs/spring_initializr.png)

## 📁 Estrutura do Projeto

```
HW1/
├── docs/                          # Screenshots e documentação
│   ├── spring_initializr.png
│   ├── swagger_ui.png
│   ├── jacoco_report.png
│   └── sonarqube_report.png
├── zeromonos/
│   ├── src/main/java/tqs/zeromonos/
│   │   ├── boundary/              # Controllers REST
│   │   │   ├── BookingController.java
│   │   │   ├── StaffBookingController.java
│   │   │   └── RestExceptionHandler.java
│   │   ├── services/              # Lógica de negócio
│   │   │   ├── BookingService.java
│   │   │   ├── BookingServiceImplementation.java
│   │   │   └── MunicipalityImportService.java
│   │   ├── data/                  # Entities e Repositories
│   │   │   ├── Booking.java
│   │   │   ├── Municipality.java
│   │   │   ├── StateChange.java
│   │   │   ├── BookingRepository.java
│   │   │   ├── MunicipalityRepository.java
│   │   │   ├── BookingStatus.java
│   │   │   └── TimeSlot.java
│   │   ├── dto/                   # Data Transfer Objects
│   │   │   ├── BookingRequestDTO.java
│   │   │   └── BookingResponseDTO.java
│   │   ├── utils/                 # Utilitários
│   │   │   ├── DateValidator.java
│   │   │   └── HistoryMapper.java
│   │   └── ZeromonosApplication.java
│   ├── src/main/resources/
│   │   ├── application.properties # Configuração
│   │   ├── static/                # Frontend HTML/CSS/JS
│   │   │   ├── index.html
│   │   │   ├── create-booking.html
│   │   │   ├── lookup-booking.html
│   │   │   ├── staff-panel.html
│   │   │   ├── css/
│   │   │   └── js/
│   │   └── templates/
│   ├── src/test/java/tqs/zeromonos/
│   │   ├── isolation/             # Testes unitários
│   │   ├── functional/            # Testes de integração/E2E
│   │   └── cucumber/              # Testes BDD
│   ├── pom.xml
│   ├── sonar-project.properties
│   └── run-sonar.sh
├── .github/workflows/
│   └── build.yml                  # CI/CD pipeline
└── README.md
```

## 🏗️ Arquitetura

Arquitetura em **camadas** seguindo os princípios de separação de responsabilidades:

### Camadas

**1. Boundary (Controllers)**
- `BookingController` - API pública (`/api/bookings`)
- `StaffBookingController` - API administrativa (`/api/staff/bookings`)
- `RestExceptionHandler` - Tratamento global de exceções

**2. Service (Lógica de Negócio)**
- `BookingServiceImplementation` - Validações de data, limites, histórico de mudanças
- `MunicipalityImportService` - Importação de municípios portugueses

**3. Data (Entities & Repositories)**
- `Booking` - Agendamentos (token, data, período, estado)
- `Municipality` - Municípios portugueses
- `StateChange` - Histórico de mudanças de estado

**4. DTOs (Data Transfer Objects)**
- `BookingRequestDTO` / `BookingResponseDTO` - Transferência de dados

## 🗄️ Diagrama Relacional

```
┌─────────────────────────────┐
│     Municipality            │
├─────────────────────────────┤
│ PK  id: Long                │
│      name: String (unique)  │
└──────────────┬──────────────┘
               │ 1:N
┌──────────────▼──────────────┐
│     Booking                 │
├─────────────────────────────┤
│ PK  id: UUID                │
│      token: String (unique) │
│ FK  municipality_id         │
│      description: String    │
│      requestedDate: Date    │
│      timeSlot: Enum         │
│      status: Enum           │
│      createdAt: DateTime    │
│      updatedAt: DateTime    │
└──────────────┬──────────────┘
               │ 1:N
┌──────────────▼──────────────┐
│     StateChange             │
├─────────────────────────────┤
│ PK  id: UUID                │
│ FK  booking_id              │
│      status: Enum           │
│      timestamp: DateTime    │
└─────────────────────────────┘
```

**Relações:**
- `Municipality` → `Booking` (1:N) - Um município tem múltiplos agendamentos
- `Booking` → `StateChange` (1:N) - Cada agendamento mantém histórico de estados
- Cascade `ALL` + `orphanRemoval` para limpeza automática

## ⚙️ Regras de Negócio

- ❌ Datas no passado/hoje/Domingo
- 📊 Máximo 32 agendamentos por município
- 🔄 Estados por convenção seguem esta ordem **mas o staff pode mudá-la se assim entender**: RECEIVED → ASSIGNED → IN_PROGRESS → COMPLETED → CANCELLED
- ⏰ Períodos: Early Morning, Morning, Afternoon, Evening, Night, Late Night, Anytime

## 🧪 Modelo de Testes

Estratégia de testes **em pirâmide** combinando múltiplas abordagens para garantir qualidade:

### Testes Unitários
**Isolamento** com mocks e stubs:
- `BookingServiceImplementationTest` (25) - Mocks de Repository
- `BookingControllerTest` (17) - MockMvc + Mockito
- `StaffBookingControllerTest` (14) - MockMvc + Mockito
- `BookingRepositoryTest` (23) - @DataJpaTest + TestEntityManager
- `MunicipalityRepositoryTest` (23) - @DataJpaTest
- `TestDate` (4) - Utilitários

### Testes de Integração
**API REST** com RestAssured:
- `BookingApiTest` (21) - Endpoints públicos e staff
- `BookingApiEdgeCasesTest` - Validações e casos extremos

### Testes Funcionais (BDD)
**Cucumber** com Gherkin:
- `booking.feature` - Fluxos públicos (9 cenários)
- `staff.feature` - Gestão administrativa (6 cenários)

### Testes End-to-End
**Selenium** WebDriver:
- `ClientViewSeleniumTest` - Interface pública
- `StaffViewSeleniumTest` - Painel administrativo

```bash
# Executar todos os testes
mvn clean test

# Ver relatório de cobertura
open target/site/jacoco/index.html
```

## 📊 Qualidade de Código

**Ferramentas**: JaCoCo + SonarQube  
**Cobertura**: Relatórios gerados via JaCoCo, thresholds configurados no SonarQube

![JaCoCo Report](docs/jacoco_report.png)

![SonarQube Report](docs/sonarqube_report.png)

```bash
# Gerar relatório de cobertura
mvn clean test jacoco:report

# Visualizar relatório
open target/site/jacoco/index.html

# Iniciar server Via Docker (recomendado)
docker run -d --name sonarqube -p 9000:9000 sonarqube

# Análise SonarQube
./run-sonar.sh local  # Requer servidor em localhost:9000
```

**Métricas**: Cobertura de código, complexidade ciclomática, code smells, bugs, vulnerabilidades, dívida técnica

## 📚 Documentação API (Swagger UI)

![Swagger UI](docs/swagger_ui.png)

Interface interativa da API REST disponível em:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

**Endpoints documentados**:
- **Civilian** (`/api/bookings`) - Criar, consultar, cancelar agendamentos
- **Staff** (`/api/staff/bookings`) - Listar, atualizar estados

Documentação gerada automaticamente via SpringDoc OpenAPI 2.7.0

## 🔄 CI/CD (GitHub Actions)

Workflow automatizado configurado em `.github/workflows/build.yml`:

**Funcionalidades**:
- ✅ Build e testes automáticos em cada push/PR para `master`/`main`
- ✅ Análise SonarQube integrada
- ✅ Upload de relatórios JaCoCo como artefatos
- ✅ Cache de dependências Maven e SonarQube

**Configurar Secrets no GitHub**:
- `SONAR_TOKEN`: Token do SonarCloud/SonarQube
- `SONAR_HOST_URL`: URL do servidor SonarQube

**Ver resultados**: GitHub → Actions → Build and analyze

## 🚀 Executar Aplicação

```bash
cd zeromonos
mvn spring-boot:run
```

Aplicação disponível em http://localhost:8080