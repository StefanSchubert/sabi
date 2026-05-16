# i18n Key Contract: 005-coral-stock

**Phase 1 output for `/speckit.plan`**  
**Date**: 2026-05-22

All keys below must be present in all 6 message bundle files:
`messages.properties` (fallback), `messages_de.properties`, `messages_en.properties`,
`messages_es.properties`, `messages_fr.properties`, `messages_it.properties`

Files location: `sabi-webclient/src/main/resources/i18n/`

---

## Section: Coral Stock — General Labels

| Key | EN value (reference) |
|-----|----------------------|
| `coralstock.tab.label` | Coral Stock |
| `coralstock.active.section.label` | Currently in tank |
| `coralstock.departed.section.label` | Departed corals |
| `coralstock.add.button.label` | Add coral |
| `coralstock.edit.button.label` | Edit |
| `coralstock.delete.button.label` | Delete |
| `coralstock.departure.button.label` | Record departure |
| `coralstock.save.button.label` | Save |
| `coralstock.cancel.button.label` | Cancel |
| `coralstock.detail.title` | Coral Detail |
| `coralstock.list.title` | Coral Stock |
| `coralstock.no.entries.label` | No corals in this aquarium yet. |

## Section: Coral Stock — Form Fields

| Key | EN value (reference) |
|-----|----------------------|
| `coralstock.form.speciesname.label` | Species name |
| `coralstock.form.speciesname.required` | Species name is required. |
| `coralstock.form.scientificname.label` | Scientific name |
| `coralstock.form.classification.label` | Classification |
| `coralstock.form.carelevel.label` | Care level |
| `coralstock.form.entrdate.label` | Date added |
| `coralstock.form.entrydate.required` | Entry date is required. |
| `coralstock.form.entrydate.future` | Entry date cannot be in the future. |
| `coralstock.form.refurl.label` | Reference URL |
| `coralstock.form.refurl.invalid` | Reference URL must start with http:// or https:// |
| `coralstock.form.notes.label` | Notes |
| `coralstock.form.photo.label` | Photo |
| `coralstock.form.photo.upload` | Upload photo |
| `coralstock.form.photo.too_large` | The photo exceeds the 5 MB size limit. |
| `coralstock.form.photo.invalid_format` | Unsupported image format. Please use JPEG, PNG, WebP, or GIF. |
| `coralstock.form.catalogue.search.label` | Search catalogue |
| `coralstock.form.catalogue.noresults` | No coral catalogue entries found. |
| `coralstock.form.catalogue.propose_link` | Propose new catalogue entry |

## Section: Coral Stock — Departure Form

| Key | EN value (reference) |
|-----|----------------------|
| `coralstock.departure.form.title` | Record coral departure |
| `coralstock.departure.date.label` | Departure date |
| `coralstock.departure.date.required` | Departure date is required. |
| `coralstock.departure.date.before_entry` | Departure date cannot be before the entry date. |
| `coralstock.departure.reason.label` | Departure reason |
| `coralstock.departure.reason.required` | A departure reason must be selected. |
| `coralstock.departure.note.label` | Departure note |
| `coralstock.departure.note.maxlength` | The departure note may not exceed 500 characters. |
| `coralstock.delete.denied.label` | Coral could not be deleted. |
| `coralstock.delete.has_departure.label` | This coral entry has a departure record and cannot be deleted. |

## Section: Departure Reason Values

| Key | EN value (reference) |
|-----|----------------------|
| `coralstock.departure.reason.DIED` | Died |
| `coralstock.departure.reason.SOLD` | Sold |
| `coralstock.departure.reason.GIVEN_AWAY` | Given away |
| `coralstock.departure.reason.MOVED_TO_OTHER_TANK` | Moved to another tank |
| `coralstock.departure.reason.OTHER` | Other |

## Section: Classification Labels

| Key | EN value (reference) |
|-----|----------------------|
| `coralstock.classification.LPS` | LPS (Large Polyp Stony) |
| `coralstock.classification.SPS` | SPS (Small Polyp Stony) |

## Section: Care Level Labels

| Key | EN value (reference) |
|-----|----------------------|
| `coralstock.carelevel.EASY` | Easy |
| `coralstock.carelevel.MODERATE` | Moderate |
| `coralstock.carelevel.DEMANDING` | Demanding |

## Section: Growth History

| Key | EN value (reference) |
|-----|----------------------|
| `coralstock.growth.title` | Growth History |
| `coralstock.growth.add.button` | Add measurement |
| `coralstock.growth.delete.button` | Delete measurement |
| `coralstock.growth.edit.button` | Edit |
| `coralstock.growth.form.date.label` | Measurement date |
| `coralstock.growth.form.date.required` | Measurement date is required. |
| `coralstock.growth.form.date.future` | Measurement date cannot be in the future. |
| `coralstock.growth.form.date.after_departure` | Measurement date cannot be after the departure date. |
| `coralstock.growth.form.type.label` | Measurement type |
| `coralstock.growth.form.type.required` | Measurement type is required. |
| `coralstock.growth.form.value.label` | Value |
| `coralstock.growth.form.value.required` | Measurement value is required. |
| `coralstock.growth.form.value.positive` | Measurement value must be positive. |
| `coralstock.growth.form.value.branch_count_integer` | Branch count must be a whole number. |
| `coralstock.growth.toggle.table` | Table view |
| `coralstock.growth.toggle.chart` | Chart view |
| `coralstock.growth.no.entries` | No growth measurements recorded yet. |

## Section: Growth Type Labels

| Key | EN value (reference) |
|-----|----------------------|
| `coralstock.growthtype.SURFACE_AREA_CM2` | Surface area (cm²) |
| `coralstock.growthtype.SIZE_CM` | Size (cm) |
| `coralstock.growthtype.VOLUME_CM3` | Volume (cm³) |
| `coralstock.growthtype.BRANCH_COUNT` | Branch count |

## Section: Polyp Condition History

| Key | EN value (reference) |
|-----|----------------------|
| `coralstock.polyp.title` | Polyp Condition History |
| `coralstock.polyp.add.button` | Add observation |
| `coralstock.polyp.delete.button` | Delete |
| `coralstock.polyp.edit.button` | Edit |
| `coralstock.polyp.form.date.label` | Observation date |
| `coralstock.polyp.form.date.required` | Observation date is required. |
| `coralstock.polyp.form.date.future` | Observation date cannot be in the future. |
| `coralstock.polyp.form.date.after_departure` | Observation date cannot be after the departure date. |
| `coralstock.polyp.form.condition.label` | Condition |
| `coralstock.polyp.form.condition.required` | A condition state must be selected. |
| `coralstock.polyp.no.entries` | No polyp condition observations recorded yet. |

## Section: Polyp Condition State Labels

| Key | EN value (reference) |
|-----|----------------------|
| `coralstock.condition.VITAL` | Vital |
| `coralstock.condition.TISSUE_LOSS` | Tissue loss |
| `coralstock.condition.PALE` | Pale |
| `coralstock.condition.LIMP` | Limp |
| `coralstock.condition.SIGNIFICANT_GROWTH` | Significant growth |

## Section: Coral Catalogue

| Key | EN value (reference) |
|-----|----------------------|
| `coralcatalogue.title` | Coral Catalogue |
| `coralcatalogue.scientificname.label` | Scientific name |
| `coralcatalogue.scientificname.required` | Scientific name is required. |
| `coralcatalogue.classification.label` | Classification |
| `coralcatalogue.classification.required` | Classification is required. |
| `coralcatalogue.carelevel.label` | Care level |
| `coralcatalogue.carelevel.required` | Care level is required. |
| `coralcatalogue.status.PENDING` | Pending approval |
| `coralcatalogue.status.PUBLIC` | Approved |
| `coralcatalogue.status.REJECTED` | Rejected |
| `coralcatalogue.duplicate_warning` | A coral catalogue entry with this scientific name already exists. You may still submit. |
| `coralcatalogue.i18n.commonname.label` | Common name |
| `coralcatalogue.i18n.description.label` | Description |
| `coralcatalogue.i18n.description.maxlength.error` | Description may not exceed 2000 characters. |
| `coralcatalogue.i18n.refurl.label` | Reference URL |
| `coralcatalogue.propose.button` | Propose new entry |
| `coralcatalogue.propose.success` | Your proposal has been submitted and is pending approval. |
| `coralcatalogue.propose.title` | Propose new coral catalogue entry |
| `coralcatalogue.edit.title` | Edit coral catalogue entry |
| `coralcatalogue.save.button` | Save |
| `coralcatalogue.cancel.button` | Cancel |

## Section: Coral Catalogue Admin

| Key | EN value (reference) |
|-----|----------------------|
| `coralcatalogue.admin.title` | Coral Catalogue Administration |
| `coralcatalogue.admin.pending.list.title` | Pending Proposals |
| `coralcatalogue.admin.approve.button` | Approve |
| `coralcatalogue.admin.reject.button` | Reject |
| `coralcatalogue.admin.approve.success` | Entry has been approved and is now publicly visible. |
| `coralcatalogue.admin.reject.success` | Entry has been rejected. |
| `coralcatalogue.admin.proposer.label` | Proposer |
| `coralcatalogue.admin.proposaldate.label` | Submitted on |
| `coralcatalogue.admin.no.pending` | No pending coral catalogue proposals. |

## Section: House Reef Report

| Key | EN value (reference) |
|-----|----------------------|
| `report.corals.section.title` | Coral Population |
| `report.coral.classification.label` | Classification |
| `report.coral.latest_growth.label` | Latest growth |
| `report.coral.latest_condition.label` | Latest condition |
| `report.coral.no_growth.label` | No measurements |
| `report.coral.no_condition.label` | No observations |
| `report.include_corals.toggle.label` | Include coral stock in public report |

## Summary: Total Key Count

Approximately **110 new i18n keys**.  
All keys must appear in all 6 property files before merging to main.

