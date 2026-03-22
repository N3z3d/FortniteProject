import { vi, expect } from 'vitest';
import { getTestBed } from '@angular/core/testing';
import { BrowserTestingModule, platformBrowserTesting } from '@angular/platform-browser/testing';

// ---------------------------------------------------------------------------
// Jasmine-specific matchers missing in Vitest/Chai
// ---------------------------------------------------------------------------
expect.extend({
  toBeTrue(received: unknown) {
    const pass = received === true;
    return {
      pass,
      message: () => (pass ? `expected ${received} not to be true` : `expected ${received} to be true`),
    };
  },
  toBeFalse(received: unknown) {
    const pass = received === false;
    return {
      pass,
      message: () => (pass ? `expected ${received} not to be false` : `expected ${received} to be false`),
    };
  },
  toHaveSize(received: unknown, size: number) {
    const len =
      received == null
        ? -1
        : typeof (received as any).length === 'number'
          ? (received as any).length
          : typeof (received as any).size === 'number'
            ? (received as any).size
            : Object.keys(received as any).length;
    const pass = len === size;
    return {
      pass,
      message: () => (pass ? `expected size not to be ${size}` : `expected size ${len} to equal ${size}`),
    };
  },
});

// ---------------------------------------------------------------------------
// Jasmine compatibility shim — maps jasmine.* APIs to Vitest's vi.*
// ---------------------------------------------------------------------------

function createSpyFn(): any {
  const fn = vi.fn() as any;

  fn.and = {
    returnValue: (val: any) => { fn.mockReturnValue(val); return fn; },
    returnValues: (...vals: any[]) => {
      vals.forEach(v => fn.mockReturnValueOnce(v));
      return fn;
    },
    callFake: (impl: (...args: any[]) => any) => { fn.mockImplementation(impl); return fn; },
    callThrough: () => fn,
    stub: () => { fn.mockImplementation(() => undefined); return fn; },
    throwError: (err: any) => {
      fn.mockImplementation(() => {
        throw typeof err === 'string' ? new Error(err) : err;
      });
      return fn;
    },
    resolveTo: (val: any) => { fn.mockResolvedValue(val); return fn; },
    rejectWith: (err: any) => { fn.mockRejectedValue(err); return fn; },
  };

  fn.calls = {
    count: () => fn.mock.calls.length,
    any: () => fn.mock.calls.length > 0,
    reset: () => fn.mockClear(),
    mostRecent: () =>
      fn.mock.calls.length > 0
        ? { args: fn.mock.calls[fn.mock.calls.length - 1], returnValue: fn.mock.results[fn.mock.calls.length - 1]?.value }
        : null,
    first: () =>
      fn.mock.calls.length > 0 ? { args: fn.mock.calls[0], returnValue: fn.mock.results[0]?.value } : null,
    all: () =>
      fn.mock.calls.map((args: any[], i: number) => ({
        args,
        object: fn.mock.instances[i],
        returnValue: fn.mock.results[i]?.value,
      })),
    argsFor: (i: number) => fn.mock.calls[i],
    allArgs: () => fn.mock.calls,
  };

  return fn;
}

function addSpyExtensions(spy: any): any {
  spy.and = {
    returnValue: (val: any) => { spy.mockReturnValue(val); return spy; },
    returnValues: (...vals: any[]) => { vals.forEach(v => spy.mockReturnValueOnce(v)); return spy; },
    callFake: (impl: any) => { spy.mockImplementation(impl); return spy; },
    callThrough: () => { spy.mockRestore?.(); return spy; },
    stub: () => { spy.mockImplementation(() => undefined); return spy; },
    throwError: (err: any) => {
      spy.mockImplementation(() => { throw typeof err === 'string' ? new Error(err) : err; });
      return spy;
    },
    resolveTo: (val: any) => { spy.mockResolvedValue(val); return spy; },
    rejectWith: (err: any) => { spy.mockRejectedValue(err); return spy; },
  };
  spy.calls = {
    count: () => spy.mock.calls.length,
    any: () => spy.mock.calls.length > 0,
    reset: () => spy.mockClear(),
    mostRecent: () =>
      spy.mock.calls.length > 0
        ? { args: spy.mock.calls[spy.mock.calls.length - 1] }
        : null,
    first: () => spy.mock.calls.length > 0 ? { args: spy.mock.calls[0] } : null,
    argsFor: (i: number) => spy.mock.calls[i],
    all: () => spy.mock.calls.map((args: any[]) => ({ args })),
    allArgs: () => spy.mock.calls,
  };
  return spy;
}

// jasmine.clock() — thin wrapper over Vitest fake timers
const jasmineClockApi = {
  install: () => { vi.useFakeTimers(); return jasmineClockApi; },
  uninstall: () => { vi.useRealTimers(); return jasmineClockApi; },
  tick: (ms: number) => { vi.advanceTimersByTime(ms); return jasmineClockApi; },
  mockDate: (date: Date | number) => { vi.setSystemTime(date); return jasmineClockApi; },
};

(globalThis as any).jasmine = {
  createSpy: (_name?: string): any => createSpyFn(),
  createSpyObj: <T>(_name: string, methods: (keyof T | string)[], properties?: Record<string, unknown>): any => {
    const obj: any = {};
    for (const m of methods) {
      obj[m as string] = createSpyFn();
    }
    if (properties) {
      Object.assign(obj, properties);
    }
    return obj;
  },
  clock: () => jasmineClockApi,
  objectContaining: (expected: any) => expect.objectContaining(expected),
  stringContaining: (expected: string) => expect.stringContaining(expected),
  stringMatching: (pattern: string | RegExp) =>
    typeof pattern === 'string' ? expect.stringContaining(pattern) : expect.stringMatching(pattern),
  arrayContaining: (expected: any[]) => expect.arrayContaining(expected),
  any: (type: any) => expect.any(type),
};

(globalThis as any).spyOn = (obj: any, methodName: string): any => {
  const spy = vi.spyOn(obj, methodName) as any;
  return addSpyExtensions(spy);
};

(globalThis as any).spyOnProperty = (obj: any, propertyName: string, accessType: 'get' | 'set' = 'get'): any => {
  const spy =
    accessType === 'get'
      ? (vi.spyOn(obj, propertyName, 'get') as any)
      : (vi.spyOn(obj, propertyName, 'set') as any);
  return addSpyExtensions(spy);
};

// ---------------------------------------------------------------------------
// localStorage mock — jsdom/forks pool doesn't expose a functional localStorage
// (TranslationService constructor calls getItem/removeItem/clear on startup)
// ---------------------------------------------------------------------------
const _localStorageStore: Record<string, string> = {};
const localStorageMock = {
  getItem: (key: string) => _localStorageStore[key] ?? null,
  setItem: (key: string, value: string) => { _localStorageStore[key] = String(value); },
  removeItem: (key: string) => { delete _localStorageStore[key]; },
  clear: () => { Object.keys(_localStorageStore).forEach(k => delete _localStorageStore[k]); },
  get length() { return Object.keys(_localStorageStore).length; },
  key: (i: number) => Object.keys(_localStorageStore)[i] ?? null,
};
Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock, writable: true });

// IntersectionObserver polyfill for jsdom
if (typeof IntersectionObserver === 'undefined') {
  (globalThis as any).IntersectionObserver = class {
    observe = vi.fn();
    unobserve = vi.fn();
    disconnect = vi.fn();
    constructor(_callback: any, _options?: any) {}
  };
}

// ---------------------------------------------------------------------------
// Angular TestBed initialization
// ---------------------------------------------------------------------------
getTestBed().initTestEnvironment(
  BrowserTestingModule,
  platformBrowserTesting(),
  { teardown: { destroyAfterEach: false } },
);
