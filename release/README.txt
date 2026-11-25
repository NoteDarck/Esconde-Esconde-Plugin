# Esconde-Esconde

Minigame de Esconde-Esconde para **Paper 26.2**, inspirado no formato clássico do TazerCraft: um ou mais procuradores são sorteados, os escondedores ganham tempo para encontrar um esconderijo, e cada captura transforma o jogador em procurador.

A versão 1.2.1 mantém o scoreboard, equipamentos e captura do release anterior e adiciona partidas independentes em várias arenas, três rodadas, UI moderna, translocação configurável e auto-start.

## Requisitos

- Paper 26.2.
- Java 25 ou superior.
- Nenhuma dependência externa.

## Instalação

Copie `release/Esconde-Esconde-1.2.1-paper-26.2.jar` para `plugins/` e inicie o servidor. A configuração fica em `plugins/Esconde-Esconde/config.yml`.

## Cadastrando uma arena

A arena `principal` é criada automaticamente. Para criar outra arena:

```text
/esconde arena criar escola
/esconde arena set escola lobby
/esconde arena set escola hider
/esconde arena set escola hunter
/esconde arena set escola spectator
/esconde arena set escola pos1
/esconde arena set escola pos2
```

`pos1` e `pos2` delimitam a região usada para impedir que escondedores saiam do mapa e para sortear o destino da translocação. O mundo precisa estar carregado antes de configurar os pontos.

Confira uma arena com:

```text
/esconde arena listar
/esconde arena info escola
```

As arenas são persistidas no arquivo `config.yml`, podem ser editadas individualmente e funcionam simultaneamente sem misturar jogadores, timers ou placares.

### Início automático

Por padrão, a arena inicia sozinha quando atinge `min-players`. O countdown padrão é de 10 segundos. Se um novo jogador entrar durante essa contagem, ela volta para 10 segundos. Se alguém sair e a arena ficar abaixo do mínimo, a contagem é cancelada.

```yaml
auto-start:
  enabled: true
  countdown-seconds: 10
```

O comando `/esconde iniciar <arena>` continua disponível para administradores, usando a mesma contagem configurada.

## Fluxo da partida

1. Os jogadores entram em uma arena em estado `WAITING`.
2. O administrador inicia a contagem `STARTING`.
3. Em cada rodada acontece o `ROLE_SELECTION`; por padrão, um procurador é sorteado.
4. Os escondedores recebem 30 segundos de preparação e os procuradores ficam cegos e presos ao spawn.
5. Em `HUNTING`, os procuradores são liberados e têm dez minutos para encontrar os escondedores.
6. Um escondedor atingido ou morto passa imediatamente para o time dos procuradores e recebe o equipamento vermelho.
7. A partida tem três rodadas por padrão. O placar de rodadas é mostrado na scoreboard.
8. Os escondedores vencem uma rodada se pelo menos um sobreviver ao tempo; os procuradores vencem se capturarem todos.
9. Ao fim das rodadas, todos são restaurados ao inventário, gamemode e localização originais.

## Mecânicas inspiradas no TazerCraft

- Identificação visual: procuradores usam armadura de couro vermelha e escondedores usam armadura verde.
- Procuradores recebem espada de ferro e bússola.
- Escondedores recebem fogos de artifício para criar distrações.
- Escondedores recebem uma translocação por rodada quando a mecânica está habilitada. O item teleporta para um ponto aleatório dentro de `pos1`/`pos2`.
- Capturas exibem mensagem, título, fogo de artifício e atualizam contadores em tempo real.
- BossBar mostra arena, fase e tempo restante.
- ActionBar mostra tempo e quantidade de jogadores por time.
- Títulos avisam sorteio, início, 30/10 segundos restantes, liberação e resultados.

## Comandos

| Comando | Descrição | Permissão | Exemplo |
|---|---|---|---|
| `/esconde entrar [arena]` | Entra na fila da arena. Sem nome usa a primeira arena habilitada. | `esconde.join` | `/esconde entrar escola` |
| `/esconde sair` | Sai da arena atual. | `esconde.join` | `/esconde sair` |
| `/esconde status [arena]` | Lista partidas ou mostra o estado de uma arena. | `esconde.player` | `/esconde status escola` |
| `/esconde ajuda` | Mostra a ajuda dentro do jogo. | `esconde.player` | `/esconde ajuda` |
| `/esconde arena listar` | Lista todas as arenas e se estão prontas. | `esconde.admin.maps` | `/esconde arena listar` |
| `/esconde arena criar <nome>` | Cria uma arena persistente. | `esconde.admin.maps` | `/esconde arena criar hospital` |
| `/esconde arena remover <nome>` | Remove uma arena que não esteja em partida. | `esconde.admin.maps` | `/esconde arena remover hospital` |
| `/esconde arena info <nome>` | Exibe limites, rodadas e duração. | `esconde.admin.maps` | `/esconde arena info escola` |
| `/esconde arena set <nome> <ponto>` | Salva `lobby`, `hider`, `hunter`, `spectator`, `pos1` ou `pos2`. | `esconde.admin.maps` | `/esconde arena set escola pos1` |
| `/esconde iniciar <arena>` | Inicia a partida. | `esconde.admin.start` | `/esconde iniciar escola` |
| `/esconde parar <arena>` | Encerra e restaura a partida. | `esconde.admin.stop` | `/esconde parar escola` |
| `/esconde reload` | Recarrega as arenas e configurações. | `esconde.admin` | `/esconde reload` |

## Configuração por arena

```yaml
arenas:
  escola:
    enabled: true
    min-players: 2
    max-players: 16
    hunters: 1
    rounds: 3
    preparation-seconds: 30
    round-seconds: 600
    between-rounds-seconds: 8
    mechanics:
      translocation: true
      fireworks: true
      catch-by-damage: true
```

`hunters` define a quantidade de procuradores sorteados no início de cada rodada. `rounds` e `round-seconds` controlam a duração da partida. `enabled: false` impede novas entradas na arena.

## LuckPerms

O plugin já declara as permissões no `plugin.yml`. Para liberar participação para todos e administração para um grupo `staff`:

```text
/lp group default permission set esconde.player true
/lp group default permission set esconde.join true
/lp group staff permission set esconde.admin true
```

`esconde.admin` herda automaticamente `esconde.admin.maps`, `esconde.admin.start` e `esconde.admin.stop`. Se preferir separar as permissões administrativas:

```text
/lp group staff permission set esconde.admin.maps true
/lp group staff permission set esconde.admin.start true
/lp group staff permission set esconde.admin.stop true
```

Para remover uma permissão:

```text
/lp group default permission unset esconde.join
```

## Solução de problemas

- **Arena não aparece como pronta:** configure os quatro pontos obrigatórios: `lobby`, `hider`, `hunter` e `spectator`.
- **Translocação não funciona:** configure `pos1` e `pos2`, habilite `mechanics.translocation` e verifique se a região está no mesmo mundo dos spawns.
- **Jogador não consegue entrar:** confirme se ele não está em outra arena e se a partida ainda está em `WAITING`.
- **O plugin não carrega:** Paper 26.2 requer Java 25 ou superior; confira com `java -version`.
- **Configuração antiga:** a primeira inicialização migra a arena antiga para `arenas.principal` automaticamente.

## Limitações conhecidas

A translocação usa uma posição aleatória dentro do retângulo definido por `pos1` e `pos2`; ela não faz análise avançada de blocos para garantir que todo destino tenha dois blocos livres. Para mapas complexos, configure uma região ampla e segura.
