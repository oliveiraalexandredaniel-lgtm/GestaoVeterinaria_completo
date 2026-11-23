# Checklist dos requisitos (3ª Avaliação – LP1)

| Requisito | Onde está | Situação |
|---|---|---|
| Java Swing desktop + MySQL/JDBC | gui/, factory/ | Implementado |
| Pacotes factory/modelo/dao/gui | src/ | Implementado |
| Cadastro de clientes (CPF válido e único) | PainelClientes, ClienteDAO, ValidadorCPF | Implementado e testado |
| Cadastro de raças (Cachorro/Gato) | PainelRacas, RacaDAO | Implementado e testado |
| Cadastro de animais vinculados a cliente e raça | PainelAnimais, AnimalDAO | Implementado e testado |
| Consulta de animais por cliente (nome/CPF) | PainelAnimais (aba Consultar) | Implementado e testado |
| Alteração com validação e data da última alteração | ClienteDAO.alterar | Implementado e testado |
| Exclusão lógica / reativar | status + desativar/reativar | Implementado e testado |
| 3 relatórios TXT UTF-8 | GeradorRelatorioTXT, PainelRelatorios | Implementado; exemplos em docs/exemplos_relatorios |
| Script.sql | Script.sql | Implementado e executado no MySQL 8.0.46 |
| Diagramas UML, conceitual e lógico | docs/diagramas | Entregue (PNG + fontes .dot) |
| Manual do usuário com telas | docs/manual_do_usuario.pdf/.docx | Entregue (17 páginas) |
| JAR executável (opcional) | gerar_jar.sh | Script pronto |
| Pendências do aluno | — | Colocar o Connector/J em lib/, criar config.properties, conferir com o enunciado e apresentar |
