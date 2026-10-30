/// <reference types="node" />
import { describe, expect, it } from 'vitest';
import { readFileSync } from 'node:fs';

// Palette parity prevents the two clients showing different periods in the same theme.
describe('shared sky identity', () => {
  const css = readFileSync('src/styles/theme.css', 'utf8');

  it('keeps all backdrop period gradients distinct', () => {
    const gradients = [...css.matchAll(/:root\[data-sky='(night|dawn|day|dusk)'\]\s*\{ --sky-top: #([\da-f]+); --sky-mid: #([\da-f]+); --sky-low: #([\da-f]+)/g)];
    expect(gradients).toHaveLength(8);
    expect(new Set(gradients.map(m => m.slice(2).join(''))).size).toBe(8);
  });

  it('mirrors the sky hero of every period in Android', () => {
    const kotlin = readFileSync('../app/src/main/java/ir/mhdolatabadi/atid/ui/theme/Color.kt', 'utf8');
    // Night is the default (unset) hero; the other periods override it.
    const heroes = [...css.matchAll(/:root(?:\[data-sky='(dawn|day|dusk)'\])? \{\s*--hero-top: #([\da-f]+); --hero-mid: #([\da-f]+); --hero-low: #([\da-f]+)/g)];
    expect(heroes.map(m => m[1] ?? 'night')).toEqual(['night', 'dawn', 'day', 'dusk']);
    expect(new Set(heroes.map(m => m.slice(2).join(''))).size).toBe(4);
    for (const hero of heroes) {
      const colours = hero.slice(2).map(c => `Color(0xFF${c.toUpperCase()})`).join(', ');
      expect(kotlin).toMatch(new RegExp(`SkyPeriod\\.${(hero[1] ?? 'night').toUpperCase()} -> HeroPalette\\(\\s*${colours.replace(/[()]/g, '\\$&')}`));
    }
  });
});
