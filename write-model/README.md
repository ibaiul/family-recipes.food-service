# Food service - Write model

This module handles requests to mutate food entities.

Implements the command responsibility of the CQRS pattern while storing the aggregate events 
in an event sourcing fashion.

Global invariants are enforced using "Set Based Consistency Validation".

## Next Steps
### Features
- Build shopping lists from recipes or adding ingredients manually

### Tech
- Deepen Axon framework integration
  - Distributed command bus
  - Sagas
  - Snapshots
  - Event versioning (Event Upcasting)
  - CachingRepository
