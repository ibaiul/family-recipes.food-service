# Food service - Read model

This module handles queries to the food entity projections.

Implements the query responsibility of the CQRS pattern.

## Next steps
### Features
- Query Recipes by multiple tags|ingredients|properties
- Query Ingredients by multiple properties

### Tech
- Search index abstraction? ES/Solar -> if fuzzy autocompletion happens at client side then not necessary
- Cache query responses per family
