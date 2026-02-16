# Summon the base interaction entity
execute align xyz run summon interaction ~0.500 ~0.825 ~0.500 {\
  Tags:["dqc.cards.table"],\
  width:1.001,\
  height:-0.826,\
  response:1b,\
  data:{\
    health:2,\
  }\
}

# Summon the base display entity
data modify storage dqc.cards:tmp table_color set from block ~ ~ ~ CustomName
function dqc.cards:table/place_m with storage dqc.cards:tmp

# Summon the card stacks
execute align xyz positioned ~0.167 ~0.82 ~0.167 run function dqc.cards:table/place_stack
execute align xyz positioned ~0.167 ~0.82 ~0.500 run function dqc.cards:table/place_stack
execute align xyz positioned ~0.167 ~0.82 ~0.833 run function dqc.cards:table/place_stack
execute align xyz positioned ~0.500 ~0.82 ~0.167 run function dqc.cards:table/place_stack
execute align xyz positioned ~0.500 ~0.82 ~0.500 run function dqc.cards:table/place_stack
execute align xyz positioned ~0.500 ~0.82 ~0.833 run function dqc.cards:table/place_stack
execute align xyz positioned ~0.833 ~0.82 ~0.167 run function dqc.cards:table/place_stack
execute align xyz positioned ~0.833 ~0.82 ~0.500 run function dqc.cards:table/place_stack
execute align xyz positioned ~0.833 ~0.82 ~0.833 run function dqc.cards:table/place_stack

# Rotate the stacks based on placer rotation
execute if block ~ ~ ~ command_block[facing=south] align xyz as @n[tag=dqc.cards.stack,type=item_display,limit=9,dx=0,dy=0,dz=0] run data modify entity @s transformation.right_rotation set value {angle:3.142,axis:[0,0,1]}
execute if block ~ ~ ~ command_block[facing=west] align xyz as @n[tag=dqc.cards.stack,type=item_display,limit=9,dx=0,dy=0,dz=0] run data modify entity @s transformation.right_rotation set value {angle:1.571,axis:[0,0,1]}
execute if block ~ ~ ~ command_block[facing=north] align xyz as @n[tag=dqc.cards.stack,type=item_display,limit=9,dx=0,dy=0,dz=0] run data modify entity @s transformation.right_rotation set value {angle:0,axis:[0,0,1]}
execute if block ~ ~ ~ command_block[facing=east] align xyz as @n[tag=dqc.cards.stack,type=item_display,limit=9,dx=0,dy=0,dz=0] run data modify entity @s transformation.right_rotation set value {angle:4.712,axis:[0,0,1]}

setblock ~ ~ ~ end_portal_frame