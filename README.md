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

## O que foi e o que não foi testado
- Testado: banco em MySQL 8.0.46, 175 verificações do backend (`docs/resultado_testes_backend.txt`) e a interface real (prints em `docs/telas`).
- Os testes foram executados com o driver MariaDB; o **MySQL Connector/J não pôde ser baixado no ambiente de desenvolvimento** — teste na sua máquina.
- Scripts `.bat` e o visual com FlatLaf (opcional, jar em `lib/`) não foram testados no Windows.
