# Datapack -> Java Mapping

## Core bootstrap
- `data/minecraft/tags/function/load.json` -> `CardsPlugin#onEnable`
- `dqc.cards:function/load.mcfunction` -> `CardsItemService#registerRecipes` + listener registration

## Table flow
- `advancement/place_table` + `table/place_command_block_m` + `table/place` -> `TableService#handleTablePlacement` / `spawnTable`
- `advancement/table_left` + `table/damage` + `table/restore` + `table/destroy` -> `TableService#handleTableLeftClick` / `destroyTable`
- `advancement/table_right` -> `TableService#handleTableRightClick`
- `table/cleanup(_all)` -> `TableService#cleanupBrokenTables`

## Stack flow
- `advancement/stack_left` -> `TableService#handleStackLeftClick`
- `advancement/stack_right` -> `TableService#handleStackRightClick`
- `stack/click/right/*` -> `placeSingleCard`, `placeFromDeck`, `rightStandEmpty`, `rightSneakEmpty`, `setTopCardOwnerFromHead`
- `stack/click/left/*` -> `placeEntireDeck`, `leftEmptyToMainHand`, `leftEmptyToOffhand`
- `stack/visibility_*` -> `stack.locked` state + `updateStackDisplay`
- `stack/update_display` -> `updateStackDisplay`
- `stack/drop_cards` -> `destroyTable` stack-deck drop section

## Deck flow
- `deck/right_held` + `deck/right_rise` -> `DeckListener#onDeckUse`
- `deck/mainhand_flip*` -> reverse in `DeckListener#onDeckUse`
- `knuth/*` shuffle chain -> Fisher-Yates in `DeckListener#shuffle`
- `deck/mainhand_pop` + `deck/offhand_add_card` + `deck/offhand_convert_to_deck` -> corresponding operations inside `TableService`

## Joker flow
- `advancement/used_joker` + `spawn_jokers_m` -> `JokerListener#onConsumeJokerBag`
- `loot_table/joker_red.json` + `joker_blue.json` owner pools -> parsed by `DatapackLoader`

## Datapack data loading
- `recipe/deck/*.json` -> deck color + full deck card list (`DatapackLoader#parseDeckRecipe`)
- `recipe/table/*.json` -> table colors
- `recipe/joker/*.json` -> joker bag colors
