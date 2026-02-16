# Copy bundle contents to storage
data modify storage dqc.cards:tmp deck set from entity @s SelectedItem.components."minecraft:bundle_contents"

# Remove the top item from the array
data remove storage dqc.cards:tmp deck[0]

# Save the card count for setting damage later
execute store result storage dqc.cards:tmp count float 0.0185 run data get storage dqc.cards:tmp deck

# Store the color of the top card in the deck
execute store result storage dqc.cards:tmp color int 1 run data get storage dqc.cards:tmp deck[0].components."minecraft:custom_model_data".colors[1]

# Reset the bundle contents with a macro
function dqc.cards:deck/mainhand_set_m with storage dqc.cards:tmp

execute unless data entity @s SelectedItem.components."minecraft:bundle_contents"[0] run item modify entity @s weapon.mainhand {"function":"minecraft:set_count","count":-1,"add":true}