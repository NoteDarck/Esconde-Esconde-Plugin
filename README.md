# Esconde-Esconde

Minigame de Esconde-Esconde para **Paper 26.2**, inspirado no formato clássico do TazerCraft: um ou mais procuradores são sorteados, os escondedores ganham tempo para encontrar um esconderijo, e cada captura transforma o jogador em procurador.

A versão 1.2.5 mantém o scoreboard, arenas, rodadas e auto-start, mas remove o rastreamento moderno de jogadores e desativa as habilidades extras por padrão. O combate agora usa armas especiais e vários golpes antes da captura.

## Requisitos

- Paper 26.2.
- Java 25 ou superior.
- Nenhuma dependência externa.

## Instalação

Copie `release/Esconde-Esconde-1.2.5-paper-26.2.jar` para `plugins/` e inicie o servidor. A configuração fica em `plugins/Esconde-Esconde/config.yml`.

## Cadastrando uma arena

Nenhuma arena é criada automaticamente. Para criar uma arena, use o nome que quiser:

```text
/esconde arena criar escola
/esconde arena set escola lobby
/esconde arena set escola hider
/esconde arena set escola hunter
/esconde arena set escola spectator
/esconde arena set escola pos1
/esconde arena set escola pos2
```

Os quatro pontos obrigatórios são `lobby`, `hider`, `hunter` e `spectator`. Sem os quatro, a arena não fica pronta e ninguém consegue entrar.

`pos1` e `pos2` são opcionais quando a translocação está desligada. Eles delimitam a região usada para impedir que escondedores saiam do mapa e para sortear o destino da translocação. O mundo precisa estar carregado antes de configurar os pontos.

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
3. Confira se a arena está pronta com `/esconde arena listar` e se aparece `pronta: true`.
4. Em cada rodada acontece o `ROLE_SELECTION`; por padrão, um procurador é sorteado.
5. Os escondedores recebem 30 segundos de preparação e os procuradores ficam cegos e presos ao spawn.
6. Em `HUNTING`, os procuradores são liberados e têm dez minutos para encontrar os escondedores.
7. Cada escondedor suporta cinco golpes por padrão. Cada golpe reduz a vida real do jogador e mostra os corações restantes; somente o golpe final o transforma em procurador. A regeneração é bloqueada durante a partida para que a vida represente corretamente os golpes restantes. A captura por dano instantâneo pode ser reativada na configuração.
8. A partida tem três rodadas por padrão. O placar de rodadas é mostrado na scoreboard.
9. Os escondedores vencem uma rodada se pelo menos um sobreviver ao tempo; os procuradores vencem se capturarem todos.
10. Ao fim das rodadas, todos são restaurados ao inventário, gamemode e localização originais.

## Mecânicas inspiradas no TazerCraft

- Identificação visual: procuradores usam armadura de couro vermelha e escondedores usam armadura verde.
- Procuradores recebem uma vara de blaze com Repulsão II.
- Escondedores recebem um stick com Repulsão X.
- A barra moderna de localização (`locator_bar`) é desligada nos mundos do servidor, e a bússola não é mais entregue. O brilho dos jogadores também é removido.
- Os nicknames acima dos jogadores ficam ocultos durante a partida usando uma equipe de scoreboard.
- Translocação e fogos de artifício ficam desativados por padrão.
- Capturas exibem mensagem e atualizam contadores em tempo real.
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
    hider-hits: 5
    mechanics:
      translocation: false
      fireworks: false
      catch-by-damage: false
      disable-locator-bar: true
      hide-nametags: true
```

`hunters` define a quantidade de procuradores sorteados no início de cada rodada. `hider-hits` define quantos golpes o escondedor aguenta. `rounds` e `round-seconds` controlam a duração da partida. `enabled: false` impede novas entradas na arena. Com `catch-by-damage: false`, o padrão é a captura após os golpes configurados; as outras habilidades permanecem desligadas.

## Integração com NPCs e menus

O comando que deve ser executado pelo jogador é:

```text
/esconde entrar principal
```

Para deixar o jogador entrar na primeira arena habilitada, também pode usar `/esconde entrar` sem o nome da arena. O jogador precisa ter a permissão `esconde.join`, que já é pública por padrão.

### FancyMenu

O FancyMenu é um mod de menus no cliente. Em um botão, adicione uma Action Script com a ação **Send Chat Message/Command** (`sendmessage`) e use este valor:

```text
/esconde entrar principal
```

O FancyMenu enviará o comando como o jogador. Não use `/esconde iniciar`, pois esse comando é administrativo e serve para forçar o início da partida.

### NPC com FancyNpcs

O FancyMenu não cria NPCs. Se o NPC do seu lobby for do plugin FancyNpcs, crie a ação de clique direito executando o comando como jogador:

```text
/npc action Esconde RIGHT_CLICK add player_command esconde entrar principal
```

Troque `Esconde` pelo nome real do NPC. A documentação do FancyNpcs usa a ação `player_command` para executar um comando no jogador que clicou.

Se preferir executar sem depender da permissão do jogador, não use `player_command_as_op` neste caso; dê ao grupo padrão a permissão `esconde.join`:

```text
/lp group default permission set esconde.join true
```

### DeluxeHub

No DeluxeHub, o comando pode ser colocado nas ações de um item de entrada ou em uma ação de menu. O formato oficial para executar como jogador é `[COMMAND]`:

```yaml
actions:
  - "[COMMAND] esconde entrar principal"
```

Para um item no `plugins/DeluxeHub/config.yml`, um exemplo é:

```yaml
custom_join_items:
  enabled: true
  items:
    esconde:
      material: NETHER_STAR
      amount: 1
      slot: 4
      display_name: "&aEsconde-Esconde"
      lore:
        - "&7Clique para entrar na partida"
      actions:
        - "[COMMAND] esconde entrar principal"
```

Se você quiser abrir um menu próprio do DeluxeHub, use a ação `[MENU]`; dentro desse menu, o botão que entra na partida deve usar `[COMMAND] esconde entrar principal`. O DeluxeHub também possui `[CONSOLE]`, mas não use `[CONSOLE] esconde entrar`, porque o comando precisa ser executado pelo jogador.

Essas integrações apenas colocam o jogador na fila. A partida começa automaticamente quando a arena atinge `min-players` e termina a contagem configurada.

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
- **Translocação não funciona (quando você a habilitou):** configure `pos1` e `pos2`, habilite `mechanics.translocation` e verifique se a região está no mesmo mundo dos spawns.
- **Jogador não consegue entrar:** confirme se ele não está em outra arena e se a partida ainda está em `WAITING`.
- **O plugin não carrega:** Paper 26.2 requer Java 25 ou superior; confira com `java -version`.
- **Configuração antiga:** a primeira inicialização migra a arena antiga para `arenas.principal` automaticamente.

## Limitações conhecidas

A translocação usa uma posição aleatória dentro do retângulo definido por `pos1` e `pos2`; ela não faz análise avançada de blocos para garantir que todo destino tenha dois blocos livres. Para mapas complexos, configure uma região ampla e segura.
