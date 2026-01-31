// First, initialize the Angular testing environment.
import 'zone.js';
import 'zone.js/testing'; 

const globalRef = globalThis as { global?: typeof globalThis };
if (!globalRef.global) {
  globalRef.global = globalThis;
}
