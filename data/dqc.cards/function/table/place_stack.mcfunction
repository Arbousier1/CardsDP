# Summon the interaction for placing and removing cards
summon interaction ~ ~ ~ {\
  Tags:["dqc.cards.stack"],\
  width:.3333,\
  height:0.01,\
  response:1b\
}

# Summon the item display that displays the current card stack
summon item_display ~ ~ ~ {\
  Tags:["dqc.cards.stack"],\
  item_display:"fixed",\
  item:{\
    id:"paper",\
    components:{\
      "minecraft:item_model":"dqc.cards:empty_slot",\
      "minecraft:custom_model_data":{\
        flags:[false]\
      },\
    }\
  },\
  data:{\
    cards:[]\
  },\
  transformation:{\
    left_rotation:{\
      angle:-1.571,\
      axis:[1,0,0]\
    },\
    right_rotation:[0,0,0,1],\
    translation:[0,0,0],\
    scale:[0.30,0.30,0.30]\
  }\
}

# Mount the item display so we can find it with `execute on vehicle` later
ride @n[type=interaction] mount @n[type=item_display]