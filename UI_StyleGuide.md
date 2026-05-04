# SABI UI Style Guide

> **Mandatory reading** for every contributor working on SABI's frontend.  
> This guide defines the visual language, navigation patterns, and component rules
> that keep the application consistent across all pages.

---

## Table of Contents

1. [Design Philosophy](#1-design-philosophy)
2. [Color Palette](#2-color-palette)
3. [Typography](#3-typography)
4. [Dark Mode](#4-dark-mode)
5. [Accessibility (WCAG AA)](#5-accessibility-wcag-aa)
6. [Page Layout & Template](#6-page-layout--template)
7. [Navigation Patterns](#7-navigation-patterns)
8. [Forms & Input Pages](#8-forms--input-pages)
9. [Buttons](#9-buttons)
10. [PrimeFaces Component Conventions](#10-primefaces-component-conventions)
11. [Messages & Notifications](#11-messages--notifications)
12. [Responsive Design](#12-responsive-design)

---

## 1. Design Philosophy

- **Mobile-first**: Every page must be fully usable on a phone screen (360px+) before optimizing for desktop.
- **Marine Theme**: SABI is a saltwater-aquarium app. The visual language reflects this: ocean blues, teal accents, calm gradients.
- **No surprises**: UI behaves predictably. Destructive actions require confirmation. Navigation is always visible.
- **Accessibility over aesthetics**: When in conflict, WCAG AA compliance wins.

---

## 2. Color Palette

The palette is defined as CSS custom properties in `sabistyle.css` and must be used consistently.

### Light Mode

| CSS Variable            | Hex Value  | Name / Usage                          | Contrast on white |
|-------------------------|------------|---------------------------------------|-------------------|
| `--sabi-primary`        | `#0369a1`  | Ocean Blue – primary buttons, links   | ~5.7:1 ✅         |
| `--sabi-primary-dark`   | `#075985`  | Deep Ocean – headings, active states  | ~7.0:1 ✅         |
| `--sabi-primary-light`  | `#bae6fd`  | Sky Blue – table headers, borders     | decorative only   |
| `--sabi-accent`         | `#0891b2`  | Coral-Sea Teal – hover states         | ~4.6:1 ✅         |
| `--sabi-accent-light`   | `#cffafe`  | Light Teal – row hover, backgrounds   | decorative only   |
| `--sabi-bg`             | `#f0f9ff`  | Page background                       | —                 |
| `--sabi-surface`        | `#ffffff`  | Card / panel background               | —                 |
| `--sabi-text`           | `#1e293b`  | Body text                             | ~15:1 ✅          |
| `--sabi-text-muted`     | `#64748b`  | Secondary text, hints                 | ~4.6:1 ✅         |
| `--sabi-border`         | `#cbd5e1`  | Input & panel borders                 | —                 |
| `--sabi-warning-bg`     | `#fff3cd`  | Warning panels                        | —                 |
| `--sabi-warning-border` | `#ffc107`  | Warning panel border                  | —                 |

### Semantic Colors

| Usage                   | Hex        | WCAG (on white) |
|-------------------------|------------|-----------------|
| Save / Confirm button   | `#065f46`  | ~8.0:1 ✅        |
| Error text / danger     | `#b91c1c`  | ~5.5:1 ✅        |
| Overdue dates           | `#b91c1c`  | ~5.5:1 ✅        |
| Danger button bg        | `#dc2626`  | ~4.6:1 ✅        |

### Forbidden Color Combinations

| Foreground        | Background     | Contrast | Status         |
|-------------------|----------------|----------|----------------|
| `lightblue`       | white / light  | ~1.4:1   | ❌ FORBIDDEN   |
| `yellow`          | white          | ~1.1:1   | ❌ FORBIDDEN   |
| Any pastel text   | light bg       | < 4.5:1  | ❌ FORBIDDEN   |

---

## 3. Typography

Headings are styled globally via `sabistyle.css`:

| Element | Color Variable         | Size (desktop) |
|---------|------------------------|----------------|
| `h1`    | `--sabi-primary-dark`  | 1.55–1.65rem   |
| `h2`    | `--sabi-primary`       | 1.35–1.4rem    |
| `h3`    | `--sabi-accent`        | 1.15–1.25rem   |

- **Page titles**: Use `<h2>` at the top of each content area with `style="color:#075985; margin-top:0;"`.
- **Font stack**: System fonts (`-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica Neue, Arial, sans-serif`).
- **Base size**: 16px, line-height 1.6.

---

## 4. Dark Mode

SABI supports a dark mode toggled via `body.dark-mode` (set by `UserSession.bodyStyleClass`).

- **PrimeFaces theme**: `saga-blue` (light) / `arya-blue` (dark) — switched dynamically.
- Dark mode overrides for custom elements (header, footer, panels, body) are in `sabistyle.css`.

### Dark Mode Color Overrides

| Variable           | Light         | Dark          |
|--------------------|---------------|---------------|
| `--sabi-bg`        | `#f0f9ff`     | `#0f172a`     |
| `--sabi-surface`   | `#ffffff`     | `#1e293b`     |
| `--sabi-text`      | `#1e293b`     | `#e2e8f0`     |
| `--sabi-text-muted`| `#64748b`     | `#94a3b8`     |
| `--sabi-border`    | `#cbd5e1`     | `#334155`     |

### Dark Mode Rules

- Never use hard-coded hex colors in XHTML inline styles for text that must be readable in dark mode. Use CSS variables instead, or provide explicit dark-mode overrides in `sabistyle.css`.
- `overdue-date` class: `#b91c1c` (light) / `#fca5a5` (dark) — both WCAG-compliant.

---

## 5. Accessibility (WCAG AA)

**All new UI must meet WCAG AA as a minimum.**

### Contrast Requirements

| Text Type                             | Minimum Contrast |
|---------------------------------------|-----------------|
| Normal body text (< 18px regular)     | **4.5:1**       |
| Large text (≥ 18px normal / ≥ 14px bold)| **3:1**       |
| UI components (buttons, inputs, icons)| **3:1**         |

### Links

- Links on light background (white / `--sabi-bg`): use `#0369a1` or `#075985`
- Links on dark background (header/footer): use `#bae6fd` (~5.2:1)
- Links must be visually distinguishable: `text-decoration: underline` or explicit color contrast with surrounding text.

### HTML in i18n Properties

- `<a>` tags in message bundles: **always close correctly** with `</a>`, never `</>`.
- Inline `style="color:..."` in bundles: use only WCAG-compliant colors from the palette above.

---

## 6. Page Layout & Template

### Master Layout

All pages use `masterLayout.xhtml` as the Facelets template:

```xml
<ui:composition template="/template/masterLayout.xhtml">
    <ui:define name="content">
        <!-- page content here -->
    </ui:define>
</ui:composition>
```

The layout provides:
- Sticky gradient header (`#header`) — Ocean Blue gradient
- Main content area (`#content`) — max-width 960px, centered, responsive padding
- Footer (`#footer`) — `--sabi-primary-dark` background

### Page Title

Every page must have a `<title>` tag:
```xml
<h:head>
    <title>SABI - #{msg['page.title.key']}</title>
</h:head>
```

And an `<h2>` heading at the top of the content area:
```xml
<h2 style="color:#075985; margin-top:0;">
    #{msg['page.title.key']}
</h2>
```

---

## 7. Navigation Patterns

### Rule: No Dialogs / Popups for Input Forms

**Input forms (create, edit, duplicate) MUST be standalone pages, not modal dialogs.**

Rationale:
- Dialogs truncate content on small screens / scaled displays
- Dialogs lose context on back-navigation
- Standalone pages support browser back-button natively
- Consistent with mobile-app UX paradigm

**The only exception**: Confirmation dialogs (destructive actions like "delete", "mark departed") may use `p:confirmDialog` or `p:dialog` because they contain minimal content.

### Back Navigation: Dual Pattern (MANDATORY)

Every input page and every detail page must provide **two complementary** back-navigation elements:

#### 1. Breadcrumb Link at the Top

A text link with a left-arrow before the main heading, linking back to the parent list:

```xml
<div style="margin-bottom:1rem;">
    <h:link outcome="/secured/parentView"
            styleClass="sabi-back-link">
        &#8592; #{msg['common.back.to.list']}
    </h:link>
</div>
```

The CSS class `.sabi-back-link` is defined in `sabistyle.css`:
- Color: `--sabi-primary` (`#0369a1`)
- `text-decoration: underline`
- Font size: `0.9rem`

#### 2. Cancel Button at the Bottom of the Form

A secondary button in the button row at the bottom of the form:

```xml
<p:commandButton value="#{msg['common.cancel.b']}"
                 icon="pi pi-times"
                 styleClass="ui-button-secondary"
                 type="button"
                 onclick="window.location.href='#{request.contextPath}/secured/parentView.xhtml';"/>
```

**Rules:**
- Always use `type="button"` (not `type="submit"`) to prevent form submission.
- Always use `onclick` with direct `window.location.href` — do not use JSF `action` for Cancel navigation, because JSF AJAX navigation is unreliable with PrimeFaces redirect semantics.
- The Cancel button never submits the form, never triggers validation.

#### After Successful Save / Submit

After a successful save the page either:
- **Redirects automatically** (`window.location.href` from `oncomplete` JS), or
- **Shows a success panel** — which must include a "Back to list" button:

```xml
<p:commandButton value="#{msg['common.back.to.list.b']}"
                 icon="pi pi-arrow-left"
                 type="button"
                 onclick="window.location.href='#{request.contextPath}/secured/parentView.xhtml';"/>
```

#### Why Breadcrumb Link AND Cancel Button?

- **Breadcrumb link**: natural "where am I / where was I" cue, visible without scrolling.
- **Cancel button**: contextual, next to Save — muscle-memory for form-filling users.
- The header menu is always available as a third path back — but not a substitute for in-page navigation because it requires an extra tap/click to open.

### JSF Navigation with `faces-redirect`

When returning a navigation outcome from a Java action method, always use `faces-redirect=true`:

```java
public String onSave() {
    // ... save logic ...
    return "/secured/parentView?faces-redirect=true";
}
```

**NEVER use `successForwardUrl()`** with JSF — Servlet Forwards carry the POST ViewState from the
source page, causing `ArrayIndexOutOfBoundsException` in `UIComponentBase.restoreState`.

---

## 8. Forms & Input Pages

### Form Page Structure

```xml
<h:form id="entityForm" style="max-width:700px;">

    <!-- Preserve IDs across RequestScope boundary -->
    <h:inputHidden id="entityId" value="#{view.currentEntity.id}"/>

    <!-- Messages at top -->
    <p:messages id="formMessages" showDetail="true" closable="true"/>

    <!-- Fields -->
    <div class="p-field p-mb-2">
        <p:outputLabel for="fieldId" value="#{msg['form.field.label']}"
                       style="display:block; font-weight:600;"/>
        <p:inputText id="fieldId"
                     value="#{view.currentEntity.field}"
                     required="true"
                     requiredMessage="#{msg['form.field.required']}"
                     style="width:100%;"/>
        <p:message for="fieldId"/>
    </div>

    <!-- Action buttons — bottom right -->
    <div style="display:flex; justify-content:flex-end; gap:0.5rem; margin-top:1.5rem;">
        <p:commandButton value="#{msg['common.save.b']}"
                         icon="pi pi-check"
                         style="background:#065f46; border-color:#065f46; color:#ffffff;"
                         actionListener="#{view.onSave()}"
                         update="formMessages entityId"
                         oncomplete="if (!args.validationFailed) sabiRedirectToList();"/>
        <p:commandButton value="#{msg['common.cancel.b']}"
                         icon="pi pi-times"
                         styleClass="ui-button-secondary"
                         type="button"
                         onclick="window.location.href='#{request.contextPath}/secured/parentView.xhtml';"/>
    </div>
</h:form>
```

### Form Width

- Forms: `max-width: 700px` — ensures readability on wide screens without becoming unwieldy on mobile.
- Full-width lists / tables: no max-width restriction (they use the layout's 960px cap).

### Field Spacing

- Use CSS classes `p-field p-mb-2` on each field wrapper `<div>`.
- Labels: `style="display:block; font-weight:600;"` for required fields; `style="display:block;"` for optional fields.
- Inline validation message immediately below each input: `<p:message for="fieldId"/>`.

### Hidden IDs (RequestScope Survival)

When a `@RequestScoped` backing bean holds the current entity, the entity ID must be persisted in a hidden field so it survives across AJAX round trips:

```xml
<h:inputHidden id="entityId" value="#{view.currentEntity.id}"/>
```

### `enctype="multipart/form-data"` for File Uploads

When a form includes a file upload (even a raw `<input type="file">`), the form must have `enctype="multipart/form-data"`. **Exception**: When the file is uploaded separately via JavaScript Fetch API (as in `fishStockEntryPage.xhtml`), standard `enctype` is fine.

---

## 9. Buttons

### Primary Action (Save / Confirm)

```xml
<p:commandButton value="#{msg['common.save.b']}"
                 icon="pi pi-check"
                 style="background:#065f46; border-color:#065f46; color:#ffffff;"/>
```

Color: `#065f46` (dark forest green) — distinct from the ocean-blue primary buttons so "Save" is immediately recognizable. Contrast on white: ~8:1.

### Secondary Action (Cancel / Back / Reset)

```xml
<p:commandButton value="#{msg['common.cancel.b']}"
                 icon="pi pi-times"
                 styleClass="ui-button-secondary"
                 type="button"
                 onclick="window.location.href='...';"/>
```

### Default Primary Button (non-save)

Default blue: uses global `.ui-button` style → `--sabi-primary` (`#0369a1`) background.

### Danger Button (Delete / Destructive)

```xml
<p:commandButton value="#{msg['common.delete.b']}"
                 icon="pi pi-trash"
                 styleClass="ui-button-danger"/>
```

Color: `#dc2626`, hover: `#b91c1c`.

### Button Row Layout

Buttons at the bottom of a form are always in a flex row, right-aligned:

```xml
<div style="display:flex; justify-content:flex-end; gap:0.5rem; margin-top:1.5rem;">
    <!-- primary action first (left), secondary last (right) -->
    <p:commandButton ... /> <!-- Save -->
    <p:commandButton ... /> <!-- Cancel -->
</div>
```

---

## 10. PrimeFaces Component Conventions

### Panels

- Use `p:panel` with a title for grouping related fields.
- Collapsible panels: toggleable by default.
- Colored variants via CSS classes: `redColoredPanel`, `yellowColoredPanel`, `greenColoredPanel`.

### Data Tables

- `p:dataTable` with `styleClass="sabi-catalogue-table"` for catalogue-style tables.
- Empty message: always set `emptyMessage="#{msg['common.no.data.l']}"`.

### AJAX in Buttons

- `p:commandButton` is AJAX by default — only add `ajax="false"` when a full page navigation is required (e.g., from a list page navigating to an edit page).
- For Cancel buttons: never use AJAX navigation — always `type="button"` + `onclick`.

### AutoComplete

- `minQueryLength="2"` minimum.
- Always set `emptyMessage`.
- Bind `itemSelect` event for post-selection logic.

### Info Overlays (Tooltips)

For contextual help on fields, use `p:overlayPanel` with an `(i)` icon button:

```xml
<p:commandButton id="fieldInfoBtn"
                 icon="pi pi-info-circle"
                 styleClass="ui-button-text ui-button-rounded"
                 style="width:1.5rem; height:1.5rem; padding:0; color:#0369a1;"
                 type="button"
                 onclick="PF('fieldOverlayWidget').toggle(this); return false;"/>
<p:overlayPanel widgetVar="fieldOverlayWidget" showCloseIcon="true" style="max-width:380px;">
    <!-- help content -->
</p:overlayPanel>
```

---

## 11. Messages & Notifications

- In-form validation feedback: `<p:messages id="formMessages" showDetail="true" closable="true"/>` at the top of the form.
- Per-field feedback: `<p:message for="fieldId"/>` immediately below the input.
- Warning panels (e.g., duplicate detection): yellow background `background:#fef3c7; border:1px solid #d97706; color:#92400e;`.
- No-tank hint: use CSS class `no-tank-hint` (defined in `sabistyle.css`).

---

## 12. Responsive Design

Breakpoints (defined in `sabistyle.css`):

| Breakpoint | Range       | Notes                                |
|------------|-------------|--------------------------------------|
| Small phone| < 480px     | Stack form fields, full-width buttons|
| Phone      | 400px+      | Base font 1rem                       |
| Large phone| 710px+      | More padding                         |
| Tablet     | 1024px+     | 1.5rem content padding               |
| Desktop    | 1224px+     | Standard                             |
| Retina     | 1400px+     | Slightly larger fonts                |

### iOS/Android Webapp Mode

Safe-area insets are handled automatically for notch devices via:
```css
@media (display-mode: standalone) {
    #header { padding-top: calc(0.6rem + env(safe-area-inset-top, 0px)); }
}
```

No manual action needed — just don't override `#header` padding without accounting for this.

---

*Last updated: May 2026*

