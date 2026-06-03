---
name: Vibrant Minimalism
colors:
  surface: '#f8f9fa'
  surface-dim: '#d9dadb'
  surface-bright: '#f8f9fa'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f4f5'
  surface-container: '#edeeef'
  surface-container-high: '#e7e8e9'
  surface-container-highest: '#e1e3e4'
  on-surface: '#191c1d'
  on-surface-variant: '#3b4a44'
  inverse-surface: '#2e3132'
  inverse-on-surface: '#f0f1f2'
  outline: '#6b7b74'
  outline-variant: '#b9cac3'
  surface-tint: '#006b57'
  primary: '#006b57'
  on-primary: '#ffffff'
  primary-container: '#00e5bc'
  on-primary-container: '#00614f'
  inverse-primary: '#00e0b8'
  secondary: '#6844c7'
  on-secondary: '#ffffff'
  secondary-container: '#9c7afe'
  on-secondary-container: '#310082'
  tertiary: '#006591'
  on-tertiary: '#ffffff'
  tertiary-container: '#94d2ff'
  on-tertiary-container: '#005b84'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#42fdd3'
  primary-fixed-dim: '#00e0b8'
  on-primary-fixed: '#002019'
  on-primary-fixed-variant: '#005141'
  secondary-fixed: '#e8ddff'
  secondary-fixed-dim: '#cebdff'
  on-secondary-fixed: '#21005e'
  on-secondary-fixed-variant: '#5028ae'
  tertiary-fixed: '#c9e6ff'
  tertiary-fixed-dim: '#89ceff'
  on-tertiary-fixed: '#001e2f'
  on-tertiary-fixed-variant: '#004c6e'
  background: '#f8f9fa'
  on-background: '#191c1d'
  surface-variant: '#e1e3e4'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 48px
    fontWeight: '800'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
  title-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  xs: 4px
  sm: 12px
  md: 24px
  lg: 48px
  xl: 80px
  gutter: 24px
  margin-mobile: 16px
  margin-desktop: 40px
---

## Brand & Style

The visual identity of this design system is built on "High-Energy Minimalism." It targets a modern, tech-savvy audience that appreciates clean interfaces but craves personality and warmth. By pairing expansive white space with highly saturated, joyful accents, the UI evokes a sense of optimism, efficiency, and playfulness.

The design style is a hybrid of **Minimalism** and **High-Contrast/Bold**. It avoids the sterility of traditional corporate palettes by using a "candy-coated" logic—colors are dense and vibrant rather than washed out, providing clear focal points against a pristine, clinical background. The emotional response should be one of clarity and rhythmic energy.

## Colors

The palette is anchored by a "Prism White" background to ensure maximum legibility and a sense of lightness. The accent colors—Mint, Lavender, Sky Blue, and Peach—have been calibrated for high saturation and medium-high brightness. This ensures they "pop" off the screen with enough visual weight to carry functional meaning (like buttons or active states) without crossing into the fluorescent territory of neon.

**Surface Strategy:**
- **Primary Surfaces:** Pure white (#FFFFFF) for cards and main containers.
- **Backgrounds:** Ultra-light grey (#F9FAFB) to provide subtle contrast for white elements.
- **Accents:** Use these generously for interactive elements, data visualization, and iconography to guide the user's eye through the layout.

## Typography

This design system utilizes **Plus Jakarta Sans** for its friendly, open counters and modern geometric structure. It bridges the gap between professional utility and approachable warmth. 

The type hierarchy is weighted toward bold headers to maintain the "pop" aesthetic. Display and Headline styles use tighter letter spacing and heavier weights to feel impactful. Body text remains airy and legible, utilizing a standard 1.5x line height. Labels are uppercase with slight tracking to provide a distinct stylistic break from body copy.

## Layout & Spacing

The layout follows a **fluid grid** system based on an 8px rhythm. 

- **Desktop (1440px+):** 12-column grid with 24px gutters and 40px side margins.
- **Tablet (768px - 1439px):** 8-column grid with 24px gutters and 32px side margins.
- **Mobile (Up to 767px):** 4-column grid with 16px gutters and 16px side margins.

Horizontal spacing (gutters) is kept consistent to maintain vertical alignment, while vertical spacing between sections (lg and xl) is used generously to emphasize the minimalist, breathable feel of the design system.

## Elevation & Depth

Depth is handled through **Tonal Layers** and **Ambient Shadows**. To keep the interface feeling light and "pop-focused," shadows are not neutral; they are slightly tinted with the accent color of the element they support.

- **Level 0 (Floor):** Neutral background.
- **Level 1 (Card):** White surface with a very soft, diffused 10% opacity shadow.
- **Level 2 (Interactive/Floating):** Higher blur radius (20px-30px) with 15% opacity, often using the component's primary accent color as the shadow tint to create a "glow" effect rather than a heavy drop shadow.
- **Overlays:** Semi-transparent white backdrops with a 12px background blur (Glassmorphism lite) are used for modals to maintain context.

## Shapes

The shape language is consistently **Rounded (Level 2)**. This reinforces the friendly and approachable brand personality. 

- **Buttons & Small Components:** 8px (0.5rem) corner radius.
- **Cards & Containers:** 16px (1rem) corner radius.
- **Large Sections/Modals:** 24px (1.5rem) corner radius.

This moderate rounding avoids the "bubbly" look of pill shapes while feeling significantly softer and more modern than sharp or slightly softened corners.

## Components

**Buttons:**
Primary buttons use full-saturation backgrounds (Mint or Sky Blue) with white text. High-energy hover states should involve a slight scale-up (1.02x) and an increased shadow spread.

**Input Fields:**
Fields use a white background with a thin 1px light-grey border. On focus, the border transitions to a 2px thickness using the Primary Mint color, accompanied by a soft mint outer glow.

**Chips:**
Used for categorization, chips should utilize the Lavender or Peach accents at 15% opacity for the background, with high-saturation text of the same hue for maximum contrast and "pop."

**Cards:**
White surfaces with "Level 1" elevation. For featured content, a 4px top-border in one of the vibrant accent colors adds a distinct branded touch.

**Checkboxes & Radios:**
When active, these are filled with the Primary color. The "check" icon or internal radio "dot" must be pure white to ensure high-contrast visibility.