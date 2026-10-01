# ForgeIt! 2.0.0

Minecraft Java **1.21.1**, NeoForge **21.1.252**, Java **21**. Não exige outros mods.

## Instalar ou atualizar

Feche o jogo, retire o JAR antigo do ForgeIt! da pasta `mods` e coloque `forgeit-1.21.1-neoforge-2.0.0.jar` no lugar. Use a mesma versão no servidor e em todos os clientes. A pasta padrão do Windows é `%APPDATA%\.minecraft\mods`; launchers com instâncias usam a pasta da instância.

## Carteira e moedas

| Moeda | Valor em cobre | Conversão |
|---|---:|---|
| Cobre | 1 | Unidade base |
| Prata | 50 | 50 cobres |
| Ouro | 500 | 10 pratas |
| Platina | 5.000 | 10 ouros |

As quatro texturas fornecidas foram mantidas em **16×16**, com transparência.

As moedas caem fisicamente no mundo. Ao passar por cima, elas desaparecem e seu valor entra diretamente na carteira, mesmo com o inventário cheio. O tempo de espera para coleta e a proteção de proprietário continuam sendo respeitados.

Pressione **E** para abrir o inventário. O painel **Carteira**, à direita, abre e fecha junto com ele. O saldo é único e aparece decomposto nas quatro moedas; por exemplo, 5.551 cobres aparecem como 1 platina, 1 ouro, 1 prata e 1 cobre. A conversão não perde valor.

Para **sacar**, clique na moeda desejada, preencha a quantidade e clique em **Sacar moedas**. O servidor converte o saldo em itens físicos no inventário. Você pode guardá-los em baús, largá-los com Q ou negociar com amigos. As moedas sacadas permanecem físicas até serem depositadas ou recolhidas do chão.

O botão **Depositar moedas** converte todas as moedas do inventário em saldo. Se não houver espaço suficiente para um saque completo, nada é debitado. O limite por pedido é de 2.304 moedas.

A carteira é individual por jogador/mundo, persiste ao sair e entrar e é mantida após a morte. Moedas físicas continuam seguindo as regras normais de perda de itens. O saldo máximo é 9.000.000.000.000.000 unidades de cobre.

## Migração da versão 1.0.0

Cada moeda antiga vale **1 cobre**. Moedas antigas no inventário são depositadas automaticamente ao entrar. As que estiverem em baús podem ser retiradas e depositadas pelo painel ou coletadas do chão. O identificador antigo é mantido somente para essa migração; ele não aparece na aba criativa e não é gerado nos novos drops. Bancadas existentes e modificadores dos itens continuam usando seus identificadores originais.

## Novos drops

O gerador antigo foi removido. O novo modificador global de loot adiciona tabelas de moedas às mortes de mobs, mantendo os drops normais do Minecraft e de outros mods.

| Categoria | Recompensa padrão | Exemplos |
|---|---|---|
| Comuns / passivos | 1–8 cobres; 5% de chance de mais 1 prata | Vaca, porco, ovelha |
| Hostis | 1–3 pratas | Zumbi, esqueleto, creeper, piglins |
| Intermediários | 3–8 pratas | Blaze, bruxa, enderman, breeze, shulker |
| Raros / minichefes | 1–3 ouros | Guardião-mestre, devastador, evocador, bruto piglin, golem de ferro |
| Chefes / lendários | 2–5 platinas | Dragão, Wither, Warden |

Por padrão, exige participação de um jogador na morte e respeita `doMobLoot` e as regras de loot da entidade. O dragão usa a tabela de chefe uma única vez, apesar das várias ondas de experiência.

As categorias são ajustáveis por tags `forgeit:coin_bosses`, `coin_rare`, `coin_intermediate` e `coin_hostile`. As tabelas ficam em `data/forgeit/loot_table/coins/`. Mobs de outros mods sem tags recebem classificação por vida e hostilidade: ≥200 de vida são chefes, ≥80 são raros, hostis com ≥40 são intermediários, outros hostis são comuns hostis; os demais usam a tabela de comuns. Tags explícitas têm prioridade. Mods que não usam o fluxo normal de loot podem precisar de integração própria.

## Reforja

O custo padrão é **1 ouro = 500 cobres**, debitado da carteira. Qualquer combinação de moedas serve, pois o saldo é convertido automaticamente. No criativo, a reforja continua gratuita.

Receita da bancada:

| Ferro | Ferro | Ferro |
|---|---|---|
| Cobre | Bigorna | Cobre |
| Tábuas | Tábuas | Tábuas |

Use 3 lingotes de ferro, 2 de cobre, 1 bigorna intacta e 3 tábuas de qualquer madeira. Clique com o botão direito na bancada, coloque uma espada, machado, picareta, pá ou enxada e clique em **Reforjar**. Armaduras, arcos e escudos não são aceitos.

O “+” e o “?” foram centralizados, e o conjunto de botão e custo/saldo foi alinhado ao centro da interface. O visual da carteira segue a paleta da mesa. O fundo editável da mesa está em `assets/forgeit/textures/gui/reforging_table.png` (256×225).

| Modificador | Dano | Velocidade de ataque | Durabilidade | Chance |
|---|---:|---:|---:|---:|
| Afiada | +10% | — | — | 22% |
| Veloz | — | +15% | — | 20% |
| Pesada | +20% | −15% | — | 16% |
| Resistente | — | — | +25% | 18% |
| Frágil | +15% | — | −30% | 10% |
| Lendária | +15% | +10% | +10% | 4% |
| Enferrujada | −15% | −10% | −10% | 10% |

Os resultados substituem o anterior e podem se repetir. Encantamentos e nomes são preservados. A velocidade se refere a ataques; o desgaste proporcional é mantido. A ferramenta volta ao jogador ao fechar o menu.

## Configuração

Em `serverconfig/forgeit-server.toml` dentro da pasta do mundo:

```toml
reforgeCostCopper = 500
dropMultiplier = 1.0
requirePlayerKill = true
passiveDrops = true
```

`reforgeCostCopper` aceita 1 a 1.000.000.000. A chave antiga `reforgeCost` foi substituída. `dropMultiplier` aceita 0 a 10. Edite com o mundo fechado; para novos mundos, use `defaultconfigs/forgeit-server.toml`. O servidor controla todos os valores e resultados.

## Teste rápido no jogo

Com comandos habilitados:

```mcfunction
/give @s forgeit:reforging_table
/give @s forgeit:platinum_coin 2
/give @s minecraft:diamond_sword
```

Abra E e deposite as moedas; escolha prata, saque algumas e jogue-as para outro jogador. A aba criativa **ForgeIt!** contém a bancada e as quatro moedas.

## Compilar e editar

Com JDK 21 e `JAVA_HOME` configurado:

- Windows: `gradlew.bat build`
- Linux/macOS: `bash gradlew build`
- Testes de integração: `bash gradlew runGameTestServer`
- Cliente de desenvolvimento: `bash gradlew runClient`

A saída jogável é `build/libs/forgeit-1.21.1-neoforge-2.0.0.jar`. Não instale o arquivo `-sources.jar` como mod. Recursos ficam em `src/main/resources`; o código, em `src/main/java`. O gerador antigo foi desativado para preservar as texturas e o modelo personalizados.

## Créditos e validação

Código: MIT. Modelo da mesa e texturas de moedas fornecidos por Ticuliro; modelo preservado da versão editada do JAR. Consulte `LICENSE` e `TEMPLATE_LICENSE.txt`.

Os resultados e limites dos testes desta versão estão em `VALIDACAO.md`.

Referências oficiais de implementação:
- https://docs.neoforged.net/docs/1.21.1/datastorage/attachments/
- https://docs.neoforged.net/docs/1.21.1/networking/payload/
- https://docs.neoforged.net/docs/1.21.1/resources/server/loottables/glm/

Não é um produto oficial da Mojang ou Microsoft.
