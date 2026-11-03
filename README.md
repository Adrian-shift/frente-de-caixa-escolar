# CantinaFast

**Sistema de Frente de Caixa (PDV) desktop, rápido e 100% offline, para a cantina do IFBA – Campus Irecê.**

Projeto desenvolvido como Trabalho de Conclusão de Curso (TCC) para substituir o caderno de anotações no balcão da cantina, agilizando o atendimento e entregando o fechamento do dia já somado por forma de pagamento.

<img width="1080" height="733" alt="image" src="https://github.com/user-attachments/assets/a89a6ddb-3448-4cb3-947a-f937545c6920" />

---

## Sobre o projeto

### O problema

Na cantina, o aluno compra uma ficha no balcão e depois retira o lanche. Todas as vendas e formas de pagamento eram anotadas à mão em um caderno, e no fim do dia a atendente precisava somar tudo por método de pagamento (Dinheiro, Cartão e PIX) para depois lançar no sistema fiscal da empresa. Isso causava:

- filas e atrasos nos horários de pico;
- cansaço da atendente com o registro manual;
- muito tempo gasto na soma do fim do dia;
- o sistema comercial existente era lento para o balcão e não identificava a forma de pagamento.

### A solução

O CantinaFast é uma ferramenta **operacional** de caixa: registra cada venda com poucos cliques, calcula o troco automaticamente e gera o fechamento do dia separado por forma de pagamento.

> **Escopo:** o sistema **não emite notas fiscais** e não substitui o software contábil oficial da cantina. Ele serve de apoio ao caixa, e o fechamento pode ser exportado em CSV para facilitar o lançamento no sistema fiscal.

---

## Funcionalidades

### Venda Rápida
- Botões grandes de produtos, coloridos por categoria (Ficha, Bebida, Lanche, Marmita, Outro)
- Carrinho que agrupa itens iguais (ex.: `3 × Ficha R$ 5,00 — R$ 15,00`)
- Remover um item ou limpar o pedido inteiro
- Formas de pagamento: **Dinheiro**, **Cartão** e **PIX**
- Cálculo automático de **troco**, com aviso de quanto falta quando o valor recebido não cobre o total
- **Atalhos de teclado:** `F1` Dinheiro · `F2` Cartão · `F3` PIX · `Enter` Finalizar venda
- Se a venda não for gravada, o pedido continua na tela para tentar de novo

### Encomendas (Marmita / PF)
- Cadastro de reservas por cliente, produto, quantidade e dia de entrega
- Pagamento na hora do pedido ou depois (o pagamento entra no fechamento do dia em que for registrado)
- Controle de entrega (só entrega encomenda paga)
- Resumo do dia: total de marmitas/PF e quantas ainda faltam entregar
- Cancelamento de encomendas ainda não pagas

### Vendas do Dia
- Lista de todas as vendas de qualquer data, com hora, itens, pagamento e total
- Cancelamento de venda com motivo opcional (a venda **nunca é apagada**, apenas marcada como cancelada)
- Vendas canceladas aparecem em cinza e não entram no fechamento

### Fechamento do Caixa
- Total do dia por forma de pagamento (Dinheiro, Cartão, PIX) e total geral
- Itens vendidos com quantidade e valor
- Aviso de vendas canceladas no dia
- **Exportação em CSV** (abre no Excel/LibreOffice, com acentuação correta)

### Gerenciar Produtos
- Cadastro, edição e remoção de produtos
- A remoção apenas esconde o produto (as vendas antigas são preservadas)

### Segurança dos dados
- **Backup automático** ao abrir e ao fechar o programa, mais botão de **backup manual**
- Mantém os últimos **30 dias** de backup
- Operações de venda em **transação** (se der erro no meio, nada é salvo pela metade)
- **Log de erros** com rotação automática de arquivo

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 (LTS) |
| Interface | JavaFX 21 + FXML (Scene Builder) + CSS |
| Banco de dados | SQLite (via `sqlite-jdbc`), arquivo local, sem servidor |
| Build | Maven (com Maven Wrapper) |
| Empacotamento | `jpackage` (gera aplicativo executável para Windows) |
| Arquitetura | MVC (Model – View – Controller) + DAO |

---

## Estrutura do projeto

```
src/main/java/br/edu/ifba/cantinapdv/
├── Main.java               # Ponto de entrada
├── Launcher.java           # Inicia o JavaFX, banco, backup e janela principal
├── controller/             # Controladores das telas
│   ├── MainController.java
│   ├── CaixaController.java
│   ├── EncomendasController.java
│   ├── VendasDiaController.java
│   ├── FechamentoController.java
│   └── ProdutosController.java
├── dao/                    # Acesso ao banco de dados
│   ├── ConexaoBanco.java   # Conexão e criação/atualização das tabelas
│   ├── ProdutoDAO.java
│   ├── VendaDAO.java
│   ├── EncomendaDAO.java
│   ├── RelatorioDAO.java
│   ├── BackupBanco.java
│   ├── Caminhos.java
│   └── Log.java
└── model/                  # Classes de dados
    ├── Produto.java
    ├── Encomenda.java
    ├── VendaDoDia.java
    └── LinhaResumo.java

src/main/resources/br/edu/ifba/cantinapdv/
├── *.fxml                  # Telas (View)
├── estilo.css              # Estilo visual
└── icones/                 # Logo e ícones
```

---

## Banco de dados

O SQLite cria o arquivo `cantina.db` automaticamente na primeira execução, junto com os produtos iniciais (fichas e bebidas).

Tabelas principais:

- `produtos`: itens à venda (com campo `ativo` para remoção lógica)
- `vendas`: cada venda, com forma de pagamento e dados de cancelamento
- `itens_venda`: itens de cada venda
- `encomendas`: reservas de marmita/PF, ligadas à venda quando pagas

### Onde ficam os dados

Tudo é salvo na pasta do usuário do Windows:

```
C:\Users\<usuario>\CantinaFast\
├── cantina.db        # banco de dados
├── backups\          # cantina-AAAA-MM-DD.db (últimos 30 dias)
└── logs\             # cantinafast.log
```

---

## Como executar

### Pré-requisitos

- **JDK 21** instalado
- (Opcional) IntelliJ IDEA

### Rodando pelo Maven

```bash
# Clonar o repositório
git clone https://github.com/Adrian-shift/frente-de-caixa-escolar.git
cd frente-de-caixa-escolar

# Executar
./mvnw javafx:run        # Linux/Mac
mvnw.cmd javafx:run      # Windows
```

### Gerando o aplicativo para o computador da cantina (Windows)

1. Gere o `.jar` e copie as bibliotecas para `target/libs`:
   ```bash
   mvnw.cmd clean package
   ```
2. Execute o script `empacotar.bat` (ajuste no início do arquivo o caminho do seu JDK 21, se for diferente).
3. O programa ficará pronto em `target\pacote\CantinaFast`, sem precisar instalar Java no computador de destino.

---

## Possíveis melhorias futuras

- Controle de estoque
- Impressão de comprovante/ficha
- Níveis de acesso (atendente e administrador)
- Relatórios por período (semana/mês)

---

## Contexto acadêmico

Projeto desenvolvido como TCC no **Instituto Federal da Bahia (IFBA) – Campus Irecê**.

**Autor:** Adrian Luis Alves Rocha
