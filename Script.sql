-- =====================================================================
-- Script.sql - Sistema Desktop de Gestão Veterinária
-- Banco: MySQL 8.0.16 ou superior (necessário para CHECK constraints)
-- Execução: mysql -u <usuario> -p < Script.sql
--           ou abrir no MySQL Workbench e executar tudo.
-- ATENÇÃO: o script recria o banco do zero (DROP DATABASE IF EXISTS).
-- =====================================================================

SET NAMES utf8mb4;

DROP DATABASE IF EXISTS gestao_veterinaria;

CREATE DATABASE gestao_veterinaria
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE gestao_veterinaria;

-- ---------------------------------------------------------------------
-- Tabela cliente
-- ---------------------------------------------------------------------
CREATE TABLE cliente (
    ID_Cliente            INT          NOT NULL AUTO_INCREMENT,
    Nome                  VARCHAR(100) NOT NULL,
    CPF                   VARCHAR(14)  NOT NULL,
    Data_Nascimento       DATE         NULL,
    Telefone              VARCHAR(20)  NULL,
    Endereco              VARCHAR(150) NULL,
    Bairro                VARCHAR(80)  NULL,
    Cidade                VARCHAR(80)  NULL,
    Estado                VARCHAR(2)   NULL,
    CEP                   VARCHAR(10)  NULL,
    status                BOOLEAN      NOT NULL DEFAULT TRUE,
    data_ultima_alteracao DATETIME     NULL,
    CONSTRAINT pk_cliente PRIMARY KEY (ID_Cliente),
    CONSTRAINT uq_cliente_cpf UNIQUE (CPF)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabela raca
-- Duplicidade: o mesmo nome pode existir para tipos diferentes
-- (Poodle/Cachorro e Poodle/Gato), mas não duas vezes no mesmo tipo.
-- ---------------------------------------------------------------------
CREATE TABLE raca (
    ID_Raca      INT         NOT NULL AUTO_INCREMENT,
    Nome_Raca    VARCHAR(80) NOT NULL,
    Tipo_Animal  VARCHAR(20) NOT NULL,
    Status       BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_raca PRIMARY KEY (ID_Raca),
    CONSTRAINT uq_raca_nome_tipo UNIQUE (Nome_Raca, Tipo_Animal),
    CONSTRAINT ck_raca_tipo CHECK (Tipo_Animal IN ('Cachorro', 'Gato'))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabela animal
-- ---------------------------------------------------------------------
CREATE TABLE animal (
    ID_Animal        INT          NOT NULL AUTO_INCREMENT,
    Nome             VARCHAR(100) NOT NULL,
    Data_Nascimento  DATE         NULL,
    Sexo             CHAR(1)      NULL,
    Cor              VARCHAR(50)  NULL,
    Observacoes      TEXT         NULL,
    ID_Cliente       INT          NOT NULL,
    ID_Raca          INT          NOT NULL,
    Status           BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_animal PRIMARY KEY (ID_Animal),
    CONSTRAINT fk_animal_cliente FOREIGN KEY (ID_Cliente) REFERENCES cliente (ID_Cliente),
    CONSTRAINT fk_animal_raca    FOREIGN KEY (ID_Raca)    REFERENCES raca (ID_Raca),
    CONSTRAINT ck_animal_sexo CHECK (Sexo IS NULL OR Sexo IN ('M', 'F'))
) ENGINE=InnoDB;

-- Índices para acelerar as consultas mais comuns
CREATE INDEX idx_cliente_nome   ON cliente (Nome);
CREATE INDEX idx_animal_cliente ON animal (ID_Cliente);
CREATE INDEX idx_animal_raca    ON animal (ID_Raca);

-- ---------------------------------------------------------------------
-- Dados iniciais para testes
-- Os CPFs abaixo são fictícios, porém matematicamente válidos.
-- ---------------------------------------------------------------------
INSERT INTO raca (Nome_Raca, Tipo_Animal) VALUES
    ('Poodle',            'Cachorro'),
    ('Labrador',          'Cachorro'),
    ('Golden Retriever',  'Cachorro'),
    ('Shih Tzu',          'Cachorro'),
    ('Vira-lata',         'Cachorro'),
    ('Siamês',            'Gato'),
    ('Persa',             'Gato'),
    ('Maine Coon',        'Gato'),
    ('Sem Raça Definida', 'Gato');

INSERT INTO cliente (Nome, CPF, Data_Nascimento, Telefone, Endereco, Bairro, Cidade, Estado, CEP) VALUES
    ('Ana Souza',      '123.456.789-09', '1985-03-12', '(12) 99111-1111', 'Rua das Flores, 100',   'Centro',        'São José dos Campos', 'SP', '12210-000'),
    ('Bruno Lima',     '111.444.777-35', '1990-11-25', '(12) 99222-2222', 'Av. Cidade Jardim, 250', 'Jardim Aquarius','São José dos Campos', 'SP', '12246-000'),
    ('Carla Mendes',   '529.982.247-25', '1978-07-08', '(12) 99333-3333', 'Rua Rui Barbosa, 45',   'Vila Adyana',   'São José dos Campos', 'SP', '12243-000'),
    ('Diego Ferreira', '390.533.447-05', '1995-03-30', '(12) 99444-4444', 'Rua Sete de Setembro, 8','Jardim Satélite','São José dos Campos', 'SP', '12230-000');

INSERT INTO animal (Nome, Data_Nascimento, Sexo, Cor, Observacoes, ID_Cliente, ID_Raca) VALUES
    ('Thor',   '2020-03-15', 'M', 'Caramelo', 'Vacinação em dia.',               1, 2),
    ('Mel',    '2022-07-20', 'F', 'Branca',   'Alérgica a alguns shampoos.',     1, 1),
    ('Luna',   '2019-11-02', 'F', 'Cinza',    NULL,                              2, 6),
    ('Simba',  '2021-03-30', 'M', 'Laranja',  'Castrado.',                       3, 9),
    ('Bela',   '2023-01-10', 'F', 'Preta',    'Primeira consulta em jan/2023.',  4, 4),
    ('Max',    NULL,         'M', 'Marrom',   'Data de nascimento desconhecida.',4, 5);
