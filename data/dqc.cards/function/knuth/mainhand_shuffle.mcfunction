# Copy bundle contents to storage
data modify storage dqc.cards:tmp deck set from entity @s SelectedItem.components."minecraft:bundle_contents"
execute store result storage dqc.cards:tmp i int 0.99 run data get storage dqc.cards:tmp deck

# Check if we have enough elements to shuffle, then shuffle recursively
execute if predicate {"condition":"minecraft:value_check","value":{"type":"minecraft:storage","storage":"dqc.cards:tmp","path":"i"},"range":{"min":1,"max":53}} run function dqc.cards:knuth/shuffle_loop

# Copy the result back on to the bundle
execute store result storage dqc.cards:tmp count float 0.0185 run data get storage dqc.cards:tmp deck
execute store result storage dqc.cards:tmp color int 1 run data get storage dqc.cards:tmp deck[0].components."minecraft:custom_model_data".colors[1]
function dqc.cards:deck/mainhand_set_m with storage dqc.cards:tmp