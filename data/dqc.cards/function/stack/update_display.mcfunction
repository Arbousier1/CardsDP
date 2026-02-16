execute if data entity @s data.cards[0] run data modify entity @s item set from entity @s data.cards[-1]
execute if data entity @s data.cards[0] store result entity @s transformation.scale[2] float 0.05 run data get entity @s data.cards
execute if data entity @s data.cards[0] at @s store result entity @n[type=interaction] height float 0.0017 run data get entity @s data.cards

execute unless data entity @s data.cards[0] run data modify entity @n[tag=dqc.cards.active_stack] item set value {\
  id:"paper",\
  components:{\
      "minecraft:item_model":"dqc.cards:empty_slot",\
      "minecraft:custom_model_data":{\
        flags:[false]\
      },\
  }\
}


execute unless data entity @s data.cards[0] run data modify entity @s transformation.scale[2] set value 0.3
execute unless data entity @s data.cards[6] at @s run data modify entity @n[type=interaction] height set value 0.01

execute if items entity @s container.0 player_head run item modify entity @s container.0 [{function:"minecraft:set_item",item:"poisonous_potato"},{function:"minecraft:set_components",components:{"!minecraft:enchantments":{},"!minecraft:consumable":{}}}]