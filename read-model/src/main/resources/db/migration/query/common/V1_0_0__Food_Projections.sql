create TABLE recipe
(
    id                      CHARACTER(36) PRIMARY KEY NOT NULL,
    name                    VARCHAR UNIQUE NOT NULL,
    links                   TEXT ARRAY
);

create TABLE ingredient
(
    id                      CHARACTER(36) PRIMARY KEY NOT NULL,
    name                    VARCHAR UNIQUE NOT NULL
);

create TABLE property
(
    id                      CHARACTER(36) PRIMARY KEY NOT NULL,
    name                    VARCHAR UNIQUE NOT NULL
);

create TABLE recipe_ingredient
(
    recipe_id               CHARACTER(36) REFERENCES recipe(id),
    ingredient_id           CHARACTER(36) REFERENCES ingredient(id),
    added_on                TIMESTAMP NOT NULL,
    CONSTRAINT recipe_ingredient_pk PRIMARY KEY(recipe_id, ingredient_id)
);

create TABLE ingredient_property
(
    ingredient_id           CHARACTER(36) REFERENCES ingredient(id),
    property_id             CHARACTER(36) REFERENCES property(id),
    added_on                TIMESTAMP NOT NULL,
    CONSTRAINT ingredient_property_pk PRIMARY KEY(ingredient_id, property_id)
);