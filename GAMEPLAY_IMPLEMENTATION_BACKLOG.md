# Gameplay implementation backlog

1. Validate inventory byte-count semantics against Avatar258 decoder; extend harvest capacity and duplicate-row checks to real disposable DB reload, save failure and restart scenarios.
2. Finish source/client/SQL/donor action inventory, including all nested callbacks and dynamic NPCs. Current CSV is a dispatcher candidate index.
3. Trace native seed purchase through currency debit and inventory persistence; reproduce failure boundaries before implementing an atomic purchase. Current native crop/animal catalog rules remain REFERENCE_REQUIRED.
4. Obtain verified original land, crop-care and animal rules; preserve safety gates while absent.
5. Run isolated Native63 replay characterization; unchanged two-byte request cannot identify intent after replenishment. See protocol proposal in docs/NATIVE63_REPLAY_ANALYSIS.md.
6. Complete individual NPC journeys and remaining systems with actual-client acceptance.
