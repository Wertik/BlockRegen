# Game versioned modules

Each of these submodules provide support for a different range of versions of the game. Their names are rather vague so
here's a table stating their supported versions and the reason for them.

| module  | version range | split reason                               |
|---------|---------------|--------------------------------------------|
| ancient | 1.7 - 1.8     | material changes                           |
| legacy  | 1.9 - 1.12    | material changes                           |
| modern  | 1.13 - 1.19   | material flattening, worldguard, worldedit |
| pink    | 1.20 - 1.21   | pink petals                                |

`modern` also includes a listener for the `PlayerHarvestEvent` it's registered on 1.16+.

**Why are classes in the "modern" module and package "...current" named "latest"?** Because the full classnames are used for json deserialization.
Changing them would prevent Data.json from loading.
