# Esconde-Esconde

Minigame de Esconde-Esconde para **Paper 26.2**, inspirado no formato clássico do TazerCraft: um ou mais procuradores são sorteados, os escondedores ganham tempo para encontrar um esconderijo, e cada captura transforma o jogador em procurador.

A versão 1.3.4 mantém o scoreboard, arenas, rodadas e auto-start, mas remove o rastreamento moderno de jogadores e desativa as habilidades extras por padrão. O combate agora usa armas especiais e vários golpes antes da captura.

## Requisitos

- Paper 26.2.
- Java 25 ou superior.
- Nenhuma dependência externa.

## Instalação

Copie `release/Esconde-Esconde-1.3.4-paper-26.2-release.jar` para `plugins/` e inicie o servidor. A configuração fica em `plugins/Esconde-Esconde/config.yml`.

## Cadastrando uma arena

Nenhuma arena é criada automaticamente. Para criar uma arena, use o nome que quiser:

```text
/esconde arena criar escola
/esconde arena set escola lobby
/esconde arena set escola hider
/esconde arena set escola hunter
/esconde arena set escola spectator
/esconde arena set escola exit
/esconde arena set escola pos1
/esconde arena set escola pos2
```

Os quatro pontos obrigatórios são `lobby`, `hider`, `hunter` e `spectator`. O ponto `exit` é opcional e define para onde o jogador volta ao clicar no item de saída. Sem os quatro, a arena não fica pronta e ninguém consegue entrar.

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

## PvP, seleção de arenas e itens após a partida

Durante a fase de caça, o PvP real fica liberado entre os jogadores da arena. O escondedor só é capturado quando sua vida chega a zero; depois do respawn, ele é transformado em procurador e enviado ao `hunter-spawn`. Dano durante a preparação ou fora de um confronto válido continua bloqueado.

`/esconde entrar` agora escolhe automaticamente uma arena habilitada, pronta e que ainda tenha vaga. A preferência é pela arena que já tem mais jogadores; em caso de empate, uma delas é sorteada. Também é possível usar explicitamente:

```text
/esconde entrar sorteado
```

Ao terminar a partida, o plugin entrega os itens configurados `random-join` (lã) e `exit` (barreira). A lã executa `esconde entrar sorteado` e leva o jogador para outra partida disponível. A barreira executa as ações configuradas no `items.yml`, normalmente `[COMMAND] spawn`, para sair para o lobby do servidor.

## Configuração dos itens (`items.yml`)

Todos os itens usados pelo minigame ficam em `plugins/Esconde-Esconde/items.yml`. O arquivo é criado automaticamente na primeira inicialização. Ele permite alterar material, quantidade, nome, lore, slot, encantamentos, cor da armadura, movimentação e descarte.

Exemplo do item de saída:

```yaml
items:
  exit:
    enabled: true
    material: BARRIER
    amount: 1
    name: '&cSair do Esconde-Esconde'
    slot: 8
    allow-move: false
    allow-drop: false
    actions:
      - '[LEAVE]'
      - '[COMMAND] spawn'
```

Ações disponíveis:

- `[COMMAND] spawn`: executa `spawn` como o jogador que clicou, como nos menus do DeluxeHub.
- `[CONSOLE] essentials:spawn %player%`: executa o comando pelo console.
- `[MESSAGE] &aVocê saiu!`: envia uma mensagem ao jogador.
- `[LEAVE]`: remove o jogador da fila ou partida e restaura seu inventário.
- `[RETURN]`: teleporta para o ponto `exit` da arena ou, se não existir, para o `lobby`.

Os placeholders `%player%` e `%arena%` podem ser usados nas ações. Para alterar outros itens, edite `hunter-weapon`, `hider-weapon`, `hider-teleport`, `hider-firework`, além das peças de armadura. Depois de editar, use `/esconde reload`.

`allow-move: false` impede mover o item pelo inventário durante a arena; `allow-drop: false` impede jogá-lo no chão. Para desativar qualquer item, use `enabled: false`. Itens de combate sem `actions` preservam o comportamento especial do plugin, como a pérola de translocação e os fogos.

## Item para sair e ponto de retorno

Ao entrar na fila, o jogador recebe um item de barreira no slot 8. Ao clicar nele, ele sai da arena e é enviado ao ponto `exit` configurado. Para definir esse ponto, fique no local desejado e use:

```text
/esconde arena set mario exit
```

Se `exit` não for configurado, o jogador volta para o `lobby` da própria arena. O item pode ser alterado em `settings.exit-item` ou desativado com `enabled: false`.

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
- **Jogador não consegue entrar:** confirme se ele não está em outra arena e se a partida ainda está em `WAITING`. Ao usar `/esconde entrar mario`, o plugin informa quais pontos faltam; configure `lobby`, `hider`, `hunter` e `spectator` e confira `pronta: true`.
- **O plugin não carrega:** Paper 26.2 requer Java 25 ou superior; confira com `java -version`.
- **Configuração antiga:** a primeira inicialização migra a arena antiga para `arenas.principal` automaticamente.

## Limitações conhecidas

A translocação usa uma posição aleatória dentro do retângulo definido por `pos1` e `pos2`; ela não faz análise avançada de blocos para garantir que todo destino tenha dois blocos livres. Para mapas complexos, configure uma região ampla e segura.

O item `maps` usa `FILLED_MAP` e executa `[MENU] mapasEsc`. A ação `[MENU] nome` é convertida pelo plugin em `menu nome` executado pelo jogador; ajuste o comando se o seu plugin de menus usar outra sintaxe. A lã não é mais entregue ao jogador após a partida, evitando que um comando de entrada seja executado enquanto ele ainda está associado a uma arena.

Para ocultar os nicknames, além do scoreboard interno, o plugin executa durante a entrada na partida:

```text
/team add ocultar
/team modify ocultar nametagVisibility never
/team join ocultar <jogador>
```

Ao sair, o jogador é removido desse time.


## Integridade de sessões e espectadores (1.3.3+)

A entrada normal é validada pelo `GameManager`, que mantém um índice único por UUID e uma reserva curta durante a operação. O jogador é associado à arena somente depois que o snapshot do estado foi salvo e o teleporte para o lobby foi confirmado. Se alguma etapa falhar, a reserva é liberada e o snapshot é restaurado. A seleção automática prioriza arenas prontas com menor ocupação, distribuindo melhor a fila entre arenas simultâneas.

Quando `/esconde entrar <arena>` é executado durante uma partida, a entrada é feita como espectador, desde que `spectator-settings.allow-join-during-game` esteja habilitado, a arena permita espectadores e o limite não tenha sido atingido. Espectadores ficam em uma coleção separada, não contam em `max-players`, não recebem função de escondedor/procurador, entram no modo `SPECTATOR` e são restaurados ao sair:

```yaml
spectator-settings:
  enabled: true
  allow-join-during-game: true
  allow-join-during-final-round: true
  allow-join-when-arena-full: true
  max-spectators-per-arena: 20
```

É possível limitar uma arena individualmente com:

```yaml
arenas:
  escola:
    spectators:
      allow-join: true
      max: 10
```

A saída `/esconde sair` usa o mesmo fluxo para participantes e espectadores. `PlayerQuitEvent`, `PlayerKickEvent` e a desativação do plugin removem a associação global; o desligamento cancela os timers, remove BossBars e restaura jogadores que ainda estão online. O reload é recusado quando há sessões ativas para evitar perda de snapshots.

## Auditoria e limitações conhecidas

A auditoria confirmou que o projeto já possui arenas independentes, persistência no `config.yml`, migração do schema legado, itens identificados por `PersistentDataContainer` e atualização por arena. Foram corrigidos o risco de associação após a mutação da arena, a seleção que concentrava jogadores na arena mais cheia, a ausência de tratamento para kick/desligamento e a possibilidade de reload apagar o índice de sessões.

A entrada de espectadores básica está implementada, mas esta versão ainda não possui a GUI própria de seleção de jogadores, itens configuráveis de espectador, target em primeira pessoa ou scoreboard separado de espectador. Esses pontos devem ser tratados como trabalho futuro, não como funcionalidades concluídas. Também não há persistência em disco de snapshots durante uma queda abrupta do servidor; após uma queda, o estado de um jogador desconectado não pode ser recuperado pelo plugin.

## Release 1.3.4

O release compilado está em `release/Esconde-Esconde-1.3.4-paper-26.2-release.jar`. O SHA-256 do arquivo é:

```text
c1e7dcbdea9183fd8de0e5e6b61ebe308ef40025495ba589215e5310bacfd428
```

## Verificação desta revisão

`git diff --check` foi executado sem erros. O projeto foi compilado com sucesso usando Maven 3.9.11, JDK 25.0.5.1 e Paper API 26.2:

```text
mvn -DskipTests package
```

O build foi executado com `-DskipTests`, pois o repositório original não possui testes automatizados. Os cenários de concorrência e ciclo de vida ainda precisam ser validados em um servidor Paper de teste com jogadores simulados ou um harness Bukkit/Paper.
