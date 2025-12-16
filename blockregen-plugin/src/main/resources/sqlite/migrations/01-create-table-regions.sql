CREATE TABLE IF NOT EXISTS `regions`
(
    `id`                  integer primary key,
    `name`                text unique not null,

    /* bool */
    `all`                 integer     not null,
    /* bool */
    `disable_other_break` integer,
    `priority`            integer     not null,

    /* world name */
    `world_name`          text        not null,

    /* There's no enum type, differentiate with id and map to a java enum. */
    /* 0 - cuboid, 1 - world */
    `type`                integer     not null,
    /* 0: Serialized BlockPosition. */
    `cuboid_top_left`     text,
    /* 0: Serialized BlockPosition. */
    `cuboid_bottom_right` text
) STRICT;