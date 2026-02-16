# Summon the base display entity
$execute align xyz run summon item_display ~0.500 ~0.500 ~0.500 {\
  Tags:["dqc.cards.table"],\
  item:{\
    id:"dark_oak_planks",\
    components:{\
      "minecraft:item_model":"dqc.cards:large_table",\
      "minecraft:custom_model_data":{colors:[$(table_color)]}\
    }\
  },\
  transformation:{\
    left_rotation:[0,0,0,1],\
    right_rotation:[0,0,0,1],\
    translation:[0,0,0],\
    scale:[1,1,1]\  
  }\
}