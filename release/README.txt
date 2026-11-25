# Esconde-Esconde

Plugin Paper de esconde-esconde inspirado no fluxo de minigames de servidores como TarzerCraft.

## Requisitos

- Paper 26.2 (API 26.2)
- Java 25+

## Instalação

1. Copie `release/Esconde-Esconde-1.0.0.jar` para a pasta `plugins`.
2. Reinicie o servidor.
3. Configure os pontos dentro do mundo da arena:

```text
/esconde set lobby
/esconde set hider
/esconde set hunter
/esconde set spectator
```

4. Ajuste tempos e limites em `plugins/Esconde-Esconde/config.yml`.
5. Recarregue com `/esconde reload`.

## Jogadores

- `/esconde entrar` ou `/esconde join`: entra na fila.
- `/esconde sair` ou `/esconde leave`: sai da fila.
- `/esconde status`: mostra o estado e contagem.

## Administração

- `/esconde iniciar`: inicia imediatamente se houver jogadores suficientes.
- `/esconde parar`: encerra e restaura os jogadores.
- `/esconde set <lobby|hider|hunter|spectator>`: grava a posição atual.
- `/esconde reload`: recarrega o arquivo de configuração quando não há partida ativa.

## Regras implementadas

- Contagem regressiva de esconderijo e cronômetro da partida.
- Scoreboard lateral com estado, tempo, pegadores e escondidos restantes.
- Sorteio automático dos pegadores, com quantidade configurável.
- Equipamento diferenciado: pegadores recebem espada e bússola; escondidos recebem armadura de couro.
- Durante a fase de esconder, pegadores ficam presos no spawn e recebem cegueira; depois são liberados.
- Um pegador que causar dano a um escondido captura o jogador, que passa a ser pegador.
- Vitória dos pegadores quando não restam escondidos; vitória dos escondidos quando o cronômetro termina.
- Logout durante a partida conta como saída/eliminado.
- Teleporte e restauração de inventário/estado ao final.
