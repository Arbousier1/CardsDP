advancement revoke @s only dqc.cards:table_right

execute at @n[tag=dqc.cards.table,nbt={interaction:{}}] align xyz if entity @s[y_rotation=-45..45] as @e[tag=dqc.cards.stack,type=item_display,dx=0,dy=0,dz=0] run data modify entity @s transformation.right_rotation set value {angle:3.142,axis:[0,0,1]}
execute at @n[tag=dqc.cards.table,nbt={interaction:{}}] align xyz if entity @s[y_rotation=45..135] as @e[tag=dqc.cards.stack,type=item_display,dx=0,dy=0,dz=0] run data modify entity @s transformation.right_rotation set value {angle:1.571,axis:[0,0,1]}
execute at @n[tag=dqc.cards.table,nbt={interaction:{}}] align xyz if entity @s[y_rotation=135..225] as @e[tag=dqc.cards.stack,type=item_display,dx=0,dy=0,dz=0] run data modify entity @s transformation.right_rotation set value {angle:0,axis:[0,0,1]}
execute at @n[tag=dqc.cards.table,nbt={interaction:{}}] align xyz if entity @s[y_rotation=225..315] as @e[tag=dqc.cards.stack,type=item_display,dx=0,dy=0,dz=0] run data modify entity @s transformation.right_rotation set value {angle:4.712,axis:[0,0,1]}

data remove entity @n[tag=dqc.cards.table,nbt={interaction:{}}] interaction