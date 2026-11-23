# Daniel-Yago-Ativ3 — Gestão Veterinária

Sistema desktop (Java Swing + MySQL + JDBC) para cadastro de clientes, raças e animais de uma clínica veterinária.
Atividade: 3ª Avaliação – Linguagem de Programação 1.

## Estrutura
```
Script.sql                  banco gestao_veterinaria (tabelas + dados de exemplo)
config.properties.example   modelo de configuração (copie para config.properties)
src/factory  ConnectionFactory
src/modelo   Cliente, Raca, Animal
src/dao      ClienteDAO, RacaDAO, AnimalDAO, exceções
src/gui      telas Swing (sem SQL)
src/util     CPF, idade, validações, relatórios TXT
src/principal Main
src/teste    TesteBackend (testes) e CapturarTelas (prints do manual)
lib/         coloque aqui o MySQL Connector/J (.jar)
docs/        manual do usuário, diagramas UML/conceitual/lógico, telas, relatórios de exemplo, checklist
relatorios/  saída dos relatórios TXT
```

## Como rodar
1. Instale JDK 17+ e MySQL 8.0.16+.
2. Execute `Script.sql` no MySQL (recria o banco `gestao_veterinaria`).
3. Baixe o **MySQL Connector/J** (jar) e coloque em `lib/`.
4. Copie `config.properties.example` para `config.properties` e informe usuário e senha (esse arquivo não é versionado). Alternativa: variáveis `DB_URL`, `DB_USUARIO`, `DB_SENHA`.
5. Compile e execute: `compilar.bat` + `executar.bat` (Windows) ou `./compilar.sh` + `./executar.sh` (Linux/Mac).
   No VS Code: instale o "Extension Pack for Java" e execute `principal.Main`.
6. Testes do backend (com banco recém-criado pelo Script.sql): `testar.bat` / `./testar.sh`.
7. JAR opcional: `./gerar_jar.sh` e `java -jar GestaoVeterinaria.jar` (mantenha `lib/` ao lado).

## Regras implementadas
- Exclusão lógica (campo `status`); nada é apagado do banco. Há "Mostrar inativos" e "Reativar".
- Não desativa cliente/raça que tenha animais ativos; não troca o tipo da raça se já houver animais.
- CPF validado (dígitos verificadores) e único; alteração de cliente grava a data/hora da última alteração.
- Consulta de animais por nome ou CPF do cliente.
- Relatórios TXT em UTF-8: geral (clientes e animais), animais aniversariantes e clientes aniversariantes do mês.
- PreparedStatement em todo o acesso ao banco.

🚀 Como Configurar e Executar o Projeto

Siga o passo a passo abaixo para preparar o ambiente, configurar o banco de dados e iniciar o sistema **VetGestão** em uma nova máquina.

---

### 1. Instalar os Pré-requisitos

Certifique-se de que o computador possua os seguintes softwares instalados:

* **Java (JDK) 17 ou superior** — Necessário para compilar e executar a aplicação.
* **MySQL Server 8.0.16 ou superior** — Necessário para suportar as restrições `CHECK` utilizadas no script SQL.

---

### 2. Preparar a Pasta do Projeto e os Drivers (`lib/`)

1. Clone o repositório ou copie a pasta completa `GestaoVeterinaria` para o computador e abra o terminal dentro dela:
   ```bash
   cd caminho/para/GestaoVeterinaria
   ```
2. Garanta que os arquivos `.jar` abaixo estejam dentro da subpasta `lib/`:
   * `mysql-connector-j-8.4.0.jar` *(obrigatório para conectar ao banco de dados)*
   * `flatlaf-3.5.4.jar` *(opcional, habilita o tema visual moderno)*

---

### 3. Iniciar o MySQL, Criar o Banco e o Usuário de Acesso

1. **Inicie o serviço do MySQL** (caso ainda não esteja em execução):
   * **Linux (Ubuntu/Mint):**
     ```bash
     sudo service mysql start
     ```
   * **Windows:** Abra o aplicativo **Serviços** (`services.msc`), localize o serviço **MySQL80** e clique em **Iniciar**.

2. **Crie o banco de dados e as tabelas** executando o arquivo `Script.sql` localizado na raiz do projeto:
   * **Linux:**
     ```bash
     sudo mysql < Script.sql
     ```
   * **Windows:**
     ```cmd
     mysql -u root -p < Script.sql
     ```
   > **Verificação:** O comando deve concluir sem mensagens de erro, criando o banco `gestao_veterinaria` já populado com 4 clientes, 9 raças e 6 animais de teste.

3. **Crie o usuário de conexão** acessando o prompt do MySQL (`sudo mysql` no Linux ou `mysql -u root -p` no Windows) e execute os comandos SQL abaixo:
   ```sql
   CREATE USER IF NOT EXISTS 'vetuser'@'localhost' IDENTIFIED BY '123456';
   GRANT ALL PRIVILEGES ON gestao_veterinaria.* TO 'vetuser'@'localhost';
   FLUSH PRIVILEGES;
   EXIT;
   ```
   > **Verificação:** O MySQL exibirá `Query OK` após cada comando e encerrará o prompt ao digitar `EXIT;`.

---

### 4. Configurar o `config.properties`

Como o arquivo `config.properties` é ignorado pelo Git (`.gitignore`) por motivos de segurança, ele precisa ser criado localmente:

1. Crie o arquivo a partir do modelo de exemplo:
   * **Linux / macOS:**
     ```bash
     cp config.properties.example config.properties
     ```
   * **Windows:**
     ```cmd
     copy config.properties.example config.properties
     ```

2. Abra o arquivo `config.properties` e preencha exatamente com as chaves abaixo:
   ```properties
   db.url=jdbc:mysql://localhost:3306/gestao_veterinaria
   db.usuario=vetuser
   db.senha=123456
   ```
   > **Verificação:** Salve o arquivo e confirme que o nome final seja `config.properties` (sem a extensão `.example`).

---

### 5. Compilar e Abrir o Dashboard (Painel Inicial)

Dentro da pasta `GestaoVeterinaria` no terminal, execute os scripts correspondentes ao seu sistema operacional:

* **Linux / macOS:**
  ```bash
  chmod +x compilar.sh executar.sh
  ./compilar.sh
  ./executar.sh
  ```

* **Windows:**
  ```cmd
  compilar.bat
  executar.bat
  ```

* **Alternativa pelo VS Code (qualquer sistema):** Abra a pasta `GestaoVeterinaria`, navegue até `src/principal/Main.java` e clique em **Run** acima do método `main`.

> **Verificação:** A janela principal do **VetGestão** será aberta diretamente no **Painel inicial**, exibindo os cartões com **4 Clientes ativos**, **6 Animais ativos** e **9 Raças ativas**.

