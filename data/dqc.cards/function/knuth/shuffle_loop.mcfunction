# Choose the card to swap with
function dqc.cards:knuth/choose_m with storage dqc.cards:tmp

# Execute the swap
function dqc.cards:knuth/swap_m with storage dqc.cards:tmp

# Decrement the upper value by 1
execute store result storage dqc.cards:tmp i int 0.99 run data get storage dqc.cards:tmp i

# Execute another swap if we haven't reached the end of the array yet
execute if predicate {"condition":"minecraft:value_check","value":{"type":"minecraft:storage","storage":"dqc.cards:tmp","path":"i"},"range":{"min":1,"max":54}} run function dqc.cards:knuth/shuffle_loop
