create TABLE recipe_name_constraint
(
    recipe_id               CHARACTER(36) PRIMARY KEY NOT NULL,
    recipe_name             VARCHAR UNIQUE NOT NULL
);

create TABLE ingredient_name_constraint
(
    ingredient_id           CHARACTER(36) PRIMARY KEY NOT NULL,
    ingredient_name         VARCHAR UNIQUE NOT NULL
);

create TABLE property_name_constraint
(
    property_id             CHARACTER(36) PRIMARY KEY NOT NULL,
    property_name           VARCHAR UNIQUE NOT NULL
);

create TABLE recipe_ingredient_constraint
(
    recipe_id               CHARACTER(36) REFERENCES recipe_name_constraint(recipe_id),
    ingredient_id           CHARACTER(36) REFERENCES ingredient_name_constraint(ingredient_id),
    CONSTRAINT recipe_ingredient_pk PRIMARY KEY(recipe_id, ingredient_id)
);

create TABLE ingredient_property_constraint
(
    ingredient_id           CHARACTER(36) REFERENCES ingredient_name_constraint(ingredient_id),
    property_id             CHARACTER(36) REFERENCES property_name_constraint(property_id),
    CONSTRAINT ingredient_property_pk PRIMARY KEY(ingredient_id, property_id)
);