# ROFOF Transactional Data Model — Initial contract

**Status:** Logical model baseline; physical Room schema and migrations are implementation work.  
**Reference data:** The bundled 14-table / 410-row SQLite dataset is seed/reference data only. It is not the transactional schema.

## 1. Modeling rules

- All entities have locally generated stable UUID identifiers.
- Every money value is an integer number of minor units plus currency code and exponent.
- A monetary amount is never stored as Float, Double or SQLite REAL.
- Quantities have explicit units and dimensions; conversions are allowed only within compatible dimensions.
- Committed records preserve immutable transaction-time snapshots where catalogue data may later change.
- Financial writes are atomic and idempotent where retried.
- Created/updated timestamps are metadata, not substitutes for a ledger event history.

## 2. Core entities

### Reference and catalogue
- `CatalogItem`: identity, localized labels, default unit/category, archive state.
- `Merchant` / `MerchantBranch`: store identity and optional location metadata.
- `PriceObservation`: item, merchant, observed price, currency, unit basis, observation time and provenance.
- `Category`, `UnitDefinition`, `CurrencyDefinition`: editable/reference metadata; changing them must not rewrite posted history.

### Shared purchases
- `PurchaseSession`: title, event date, currency context, status (draft/committed/archived), notes.
- `SessionParticipant`: participant identity and display snapshot within a session.
- `PurchaseLine`: item reference, item/name snapshot, quantity, unit snapshot, unit price, line total, currency and source metadata.
- `Allocation`: purchase line, participant, allocated quantity and allocated monetary amount, allocation method and rounding remainder metadata.
- `Contribution`: payer, amount, currency, payment time, method/reference and idempotency key.
- `Settlement`: debtor, creditor, amount, currency, status and settlement event timestamps.

### Personal ledger
- `PersonalExpense`: amount, currency, category snapshot/reference, merchant snapshot/reference, occurrence date, note, status and provenance.
- `PersonalExpenseTransfer`: source allocation ID, destination personal expense ID, explicit confirmation time, idempotency key and transfer status.
- `RecurringExpenseRule`: recurrence definition and next due date.
- `RecurringExpenseOccurrence`: generated draft occurrence, unique rule+period key, and posted/dismissed state.

### Reliability and portability
- `AuditEvent`: actor/device, entity type/id, action, event time, schema version and redacted change summary.
- `AttachmentMetadata`: local URI/path reference, MIME, size, checksum and retention state; bytes remain local by default.
- `ImportBatch`: source, checksum, status, validation summary and idempotency key.
- `AppMetadata`: schema version, installation/device identifier and backup metadata.

## 3. Required relational constraints

1. A purchase line belongs to exactly one purchase session.
2. An allocation references one purchase line and one participant in the same session.
3. For a committed line, allocated quantity plus explicitly unallocated quantity equals purchased quantity within the unit precision.
4. Allocated amounts plus unallocated amount reconcile to the line total using deterministic rounding.
5. Contributions are actual payments and must not be treated as allocation amounts.
6. Contribution, allocation and settlement currencies must match within a balance calculation; conversion requires an explicit rate snapshot and separate approval.
7. A personal expense transfer has a unique idempotency key and a unique source-allocation constraint for a one-time transfer.
8. A recurring occurrence has a unique (rule_id, period_key) constraint.
9. No update to a catalogue item may mutate purchase-line or personal-expense snapshots.
10. Deletion of financial records uses archive/reversal/audit semantics; cascading deletion must not erase financial history.

## 4. Balance calculation

For each participant and currency, derive net position from committed allocations (obligation) and actual contributions (paid). Settlement events reduce the outstanding bilateral balance. Do not infer a participant's share from payment amount.

A balance is a derived read model, not a mutable source-of-truth column. If cached, it must be reproducible from committed events.

## 5. Explicit shared-to-personal transfer

The transfer use case must:
1. Load the source allocation and confirm it is eligible and not already transferred.
2. Present the exact amount, currency, source purchase and participant to the user.
3. Require explicit confirmation.
4. In one database transaction, create the personal expense, create the transfer link, and append an audit event.
5. Enforce uniqueness on source allocation and idempotency key.
6. Roll back all writes if any step fails.
7. Exclude the linked transfer from combined totals where the shared purchase is already counted; provide separate ledger views and a documented combined-reporting policy.

## 6. Schema work still required

This logical contract does not claim to be a Room schema. Implementation must define:
- Concrete Room entities, DAO queries, indexes, foreign keys and type converters.
- Migration chain from the initial schema onward.
- Currency exponent validation and supported currency metadata.
- Quantity precision, canonical-unit conversion and deterministic remainder distribution.
- Audit retention and archive/reversal policy.
- Backup format version and import conflict handling.

Each rule must be linked to executable tests before the corresponding feature is marked complete.
