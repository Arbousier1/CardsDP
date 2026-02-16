# Legacy Mapping Notes

This project started as a datapack-to-plugin migration.
It is now fully plugin-native.

## Current source of truth

- Card/table/joker definitions: `src/main/java/com/dqc/cardsdp/definitions/BuiltinDefinitions.java`
- Runtime item creation and recipes: `src/main/java/com/dqc/cardsdp/item/CardsItemService.java`
- Table/stack interaction state machine: `src/main/java/com/dqc/cardsdp/table/TableService.java`
- Listeners:
  - `src/main/java/com/dqc/cardsdp/listener/TableListener.java`
  - `src/main/java/com/dqc/cardsdp/listener/DeckListener.java`
  - `src/main/java/com/dqc/cardsdp/listener/JokerListener.java`

## Removed runtime datapack dependency

- No JSON/mcfunction loading at runtime
- No `pack.mcmeta` packaging
- No `data/` datapack content in plugin build output

Legacy datapack behavior has been re-expressed in Java services/listeners.
