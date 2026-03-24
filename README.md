# Sistema de Votação

## Visão geral da solução

Solução fullstack para assembleias de cooperativismo com:

- cadastro de pautas
- abertura de sessões de votação com duração configurável
- registro de votos `SIM` e `NÃO`
- restrição de um voto por associado em cada pauta
- apuração do resultado por pauta
- integração fake com validação externa de CPF

O projeto foi dividido em:

- `backend`: API REST em Spring Boot
- `frontend`: interface React para consumir os endpoints
- `db/sqlserver`: scripts SQL para subir a estrutura no SQL Server

Observação: a persistência principal da aplicação é feita em SQL Server. O H2 permanece apenas nos testes automatizados, para manter a suíte rápida e isolada.

## Tecnologias utilizadas

### Backend

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Bean Validation
- Microsoft SQL Server JDBC Driver

### Frontend

- React 18
- Vite
- CSS puro

### Qualidade

- JUnit 5
- Spring Boot Test
- MockMvc
- Maven Checkstyle Plugin
- ESLint

## Decisões técnicas adotadas

- Arquitetura simples em camadas: `controller`, `service`, `repository`, `domain`, `dto`
- Versionamento de API por URL: `/api/v1/...`
- Tratamento centralizado de exceções com respostas previsíveis
- Restrição de voto único por pauta no serviço e no banco
- Apuração performática usando agregação no banco em vez de carregar todos os votos em memória
- Logs de negócio nos fluxos principais
- Frontend simples, focado apenas na operação do sistema
- SQL Server como persistência padrão da aplicação
- H2 restrito aos testes automatizados

## Pré-requisitos

- Docker Desktop
- Java 21
- Maven 3.9+
- Node.js 18+ com `npm`

Valide o ambiente antes de começar:

```bash
docker --version
java -version
mvn --version
node -v
npm -v
```

Observação para Windows:

- em `PowerShell` ou `cmd`, use `mvn.cmd`
- no `Git Bash`, `mvn` pode falhar dependendo da instalação local; se isso acontecer, use `mvn.cmd`

## Como subir o SQL Server com Docker

Na raiz do projeto:

```bash
docker compose up -d
```

Isso sobe um SQL Server 2022 com:

- host: `localhost`
- porta: `1433`
- usuário: `sa`
- senha: `SqlServer@123`

Para verificar se o container subiu:

```bash
docker ps
```

Container esperado:

- `votacao-sqlserver`

Se quiser acompanhar a inicialização:

```bash
docker logs -f votacao-sqlserver
```

Espere o banco terminar a inicialização antes de seguir para o schema.

## Como executar os scripts SQL

Os scripts estão em:

- `db/sqlserver/01_schema.sql`
- `db/sqlserver/02_seed.sql`

Observações importantes:

- o backend usa `ddl-auto: validate`
- isso significa que o schema precisa existir antes de subir a aplicação
- o script `01_schema.sql` recria as tabelas; se você rodar novamente, os dados atuais serão perdidos

### PowerShell

Execução do schema:

```powershell
Get-Content .\db\sqlserver\01_schema.sql | docker exec -i votacao-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "SqlServer@123" -C -i /dev/stdin
```

Execução do seed opcional:

```powershell
Get-Content .\db\sqlserver\02_seed.sql | docker exec -i votacao-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "SqlServer@123" -C -i /dev/stdin
```

### Git Bash

No `Git Bash`, use `bash -lc` para evitar conversão incorreta de caminhos:

```bash
export MSYS2_ARG_CONV_EXCL="*"
cat ./db/sqlserver/01_schema.sql | docker exec -i votacao-sqlserver bash -lc '/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "SqlServer@123" -C -i /dev/stdin'
```

Seed opcional:

```bash
cat ./db/sqlserver/02_seed.sql | docker exec -i votacao-sqlserver bash -lc '/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "SqlServer@123" -C -i /dev/stdin'
```

Se quiser validar se o banco respondeu:

```bash
docker exec -i votacao-sqlserver bash -lc '/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "SqlServer@123" -C -Q "SELECT name FROM sys.databases"'
```

## Como rodar o backend

Depois de subir o Docker e executar o schema SQL:

### PowerShell ou CMD

```powershell
cd backend
mvn.cmd spring-boot:run
```

### Git Bash

```bash
cd backend
mvn.cmd spring-boot:run
```

Se o `mvn` do seu ambiente estiver corretamente configurado, você também pode usar:

```bash
mvn spring-boot:run
```

Aplicação disponível em:

- `http://localhost:8080`

Configuração usada:

- arquivo: `backend/src/main/resources/application.yml`
- banco: `votacao`
- host: `localhost:1433`
- usuário: `sa`
- senha: `SqlServer@123`

## Como rodar o frontend

```bash
cd frontend
npm install
npm run dev
```

Aplicação disponível em:

- `http://localhost:5173`

O frontend consome a API versionada em:

- `http://localhost:8080/api/v1/pautas`

## Endpoints principais

### Criar pauta

`POST /api/v1/pautas`

```json
{
  "titulo": "Aprovação do balanço anual",
  "descricao": "Deliberação da assembleia sobre o balanço."
}
```

### Listar pautas

`GET /api/v1/pautas`

### Abrir sessão

`POST /api/v1/pautas/{pautaId}/sessao`

```json
{
  "duracaoSegundos": 120
}
```

Se a duração não for enviada, a API usa `60` segundos.

### Registrar voto

`POST /api/v1/pautas/{pautaId}/votos`

```json
{
  "associadoId": "assoc-001",
  "cpf": "12345678901",
  "opcao": "SIM"
}
```

### Obter resultado

`GET /api/v1/pautas/{pautaId}/resultado`

## Fluxo rápido de validação

Depois de subir backend e frontend, valide este fluxo:

1. Cadastrar uma pauta.
2. Abrir uma sessão para a pauta.
3. Registrar um voto `SIM` ou `NÃO`.
4. Consultar o resultado da pauta.
5. Tentar votar novamente com o mesmo associado para validar a restrição.
6. Aguardar o encerramento da sessão e tentar votar para validar o bloqueio.

Observação sobre o bônus de CPF:

- o client fake é aleatório
- o mesmo CPF pode ser aceito, rejeitado como inválido ou marcado como inapto em chamadas diferentes

## Instruções de teste

### Testes unitários e integrados do backend

#### PowerShell ou CMD

```powershell
cd backend
mvn.cmd test
```

#### Git Bash

```bash
cd backend
mvn.cmd test
```

### Validação de qualidade do backend

#### PowerShell ou CMD

```powershell
cd backend
mvn.cmd checkstyle:check
```

#### Git Bash

```bash
cd backend
mvn.cmd checkstyle:check
```

### Validação de qualidade do frontend

```bash
cd frontend
npm install
npm run lint
```

### Build do frontend

```bash
cd frontend
npm run build
```

## Observações sobre os bônus implementados

### Bônus 1 - integração com sistema externo

- Foi criado um facade fake em `backend/src/main/java/com/cooperativa/votacao/client`
- O client recebe o CPF e responde aleatoriamente
- Regras implementadas:
  - CPF inválido: `404 Not Found`
  - CPF válido + apto: `ABLE_TO_VOTE`
  - CPF válido + inapto: `UNABLE_TO_VOTE`, refletido na API como `422 Unprocessable Entity`
- O mesmo CPF pode ter respostas diferentes em chamadas diferentes, conforme o enunciado

### Bônus 2 - performance

- Restrição única por pauta e associado no banco
- Índices para busca por pauta e sessão
- Apuração usando `group by` no banco

### Bônus 3 - versionamento da API

- Estratégia adotada: versionamento por URL
- Versão principal: `/api/v1`
- Mantida compatibilidade com `/api` para transição
