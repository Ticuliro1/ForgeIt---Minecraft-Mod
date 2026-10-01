# Validação — ForgeIt! 2.0.0

## Resultado

JAR compilado para Minecraft Java 1.21.1, NeoForge 21.1.252 e Java 21.
Os **18 testes obrigatórios passaram** no GameTestServer real do Minecraft/NeoForge (1,149 s de execução dos testes; processo concluído com código 0).

Trecho do resultado:

```text
[++++++++++++++++++]
18 GAME TESTS COMPLETE IN 1.149 s
All 18 required tests passed :)
```

## Cobertura

- Coleta física para carteira com inventário cheio; coleta duplicada; atraso e proprietário do item; limite e overflow do saldo.
- Saques nas quatro denominações, mesclagem em pilhas, inventário cheio, saldo insuficiente e pedidos inválidos; rejeição sem alterações parciais.
- Depósito, migração das moedas antigas e conservação de valor nas conversões.
- Salvamento e leitura do NBT do jogador com saldo acima de 32 bits; cópia após morte; troca entre jogadores por item físico.
- Codec de rede de 64 bits e validação de pedidos pelo servidor, inclusive repetição no mesmo tick e menu incorreto.
- Morte real de zumbi e vaca gerando as novas moedas, com preservação do loot vanilla; tabela do dragão paga uma vez entre ondas de XP.
- Reforja: cobrança exata da carteira, devolução da ferramenta, conservação de nome/encantamentos/dados, persistência, probabilidades e substituição dos atributos sem acumulação.

## Integridade dos recursos

ZIP/JAR íntegro, arquivos JSON válidos, classes Java 21 (major 65), traduções PT-BR/EN-US com as mesmas chaves. As quatro texturas de moedas são idênticas, byte a byte, aos anexos de 16×16. O modelo da mesa foi preservado do JAR editado pelo usuário. Textura editável da GUI, tabelas de loot, tags, registro de modificador global e access transformer incluídos.

## Limites da validação

O cliente gráfico e uma sessão multijogador com clientes reais **não foram executados**. O servidor gráfico virtual não conseguiu abrir seus sockets neste ambiente; por isso o layout precisa de conferência manual no jogo. A lógica de tela foi compilada, mas isso não substitui uma inspeção visual. Integrações com interfaces e mobs de outros mods também precisam de verificação no modpack.

Os jogadores simulados não negociam canais customizados; a sincronização ignora conexões sem o canal de carteira. O protocolo foi verificado por codec e as operações foram testadas no servidor, sem alegar uma partida de rede real.

## Ambiente de compilação

Gradle 9.2.1, ModDevGradle 2.0.147 e JDK 21. O ambiente de execução exige proxy e um fallback local para localizar o executável Java no NeoFormRuntime; esses ajustes afetam apenas a ferramenta de build e não foram incorporados ao mod. Para o teste de servidor, o download de recursos audiovisuais foi dispensado após preparar o índice real de assets; a execução do GameTestServer permaneceu completa. O log de autenticação apresentou falha de DNS ao consultar chaves públicas da Mojang, sem impedir os testes offline.

Em um computador normal com JDK 21, use `gradlew.bat build` ou `bash gradlew build`, e `runGameTestServer` para reproduzir os testes.

## Testes presentes no projeto

- `dragonPaysOnceAcrossExperienceWaves`
- `playerKillActuallyDropsCoins`
- `preservesItemAndReplacesModifier`
- `itemSurvivesSerialization`
- `attributesDoNotAccumulate`
- `chargesExactlyAndReturnsItem`
- `rejectsInvalidInputsAndRemoteUse`
- `rarityAndRewardRanges`
- `collectIntoWalletWithFullInventory`
- `pickupRespectsOwnerDelayAndWalletLimit`
- `withdrawConvertsAndDepositConservesValue`
- `fullInventoryAndInvalidWithdrawalAreAtomic`
- `walletPersistsThroughPlayerNbt`
- `walletSurvivesDeathAndPhysicalTrading`
- `packetsKeepFullPrecisionAndRequestsAreValidated`
- `oldCoinsMigrateWithoutConsumingWithdrawnCurrency`
- `allDenominationsNormalizeExactly`
- `commonLootKeepsVanillaDrops`

SHA-256 do JAR: `3e8fe9c335e771849af405dbd2a0f75153d6115bf27943710f224629ad39a043`
