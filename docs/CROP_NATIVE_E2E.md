# Crop native E2E — milestone 3.4

**BLOCKED / NOT RUN.** No authenticated native 64/66 request, planted-cell rendering, real elapsed harvest, DB before/after, visitor test, or restart evidence was obtained in this milestone. Synthetic DB coverage from prior milestones does not establish native acceptance. No catalog migration or staging fixture was applied in this attempt.

[Chắc chắn] The bundled client `it.a(int,int,int)` builds command 64 with `int farmId, byte cell, byte seed`; `it.a(int,int)` builds 66 with `int farmId, byte cell`. The server enforces the viewed owner and uses a revision-protected farm snapshot transaction for planting; the legacy harvest implementation uses the SQL catalog and health-scaled quantity. Neither wire compatibility in a real crop interaction nor the latter economic semantics has been demonstrated. See [catalog recovery](CROP_CATALOG_RECOVERY.md).
