# Copy card from offhand to storage
data modify storage dqc.cards:tmp deck set value [{}]
data modify storage dqc.cards:tmp deck[0] set from entity @s equipment.offhand

# Replace the player's offhand with an empty deck
loot replace entity @s weapon.offhand loot dqc.cards:deck_empty

# Save the card count for setting damage later
execute store result storage dqc.cards:tmp count float 0.0185 run data get storage dqc.cards:tmp deck
execute store result storage dqc.cards:tmp color int 1 run data get storage dqc.cards:tmp deck[0].components."minecraft:custom_model_data".colors[1]

# Add the card to the deck
function dqc.cards:deck/offhand_set_m with storage dqc.cards:tmp